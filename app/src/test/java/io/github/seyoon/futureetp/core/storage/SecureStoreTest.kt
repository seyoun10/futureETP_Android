package io.github.seyoon.futureetp.core.storage

import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

// Android Keystore는 JVM에 없어 키셋은 메모리에서 만든다. 암호화·저장·복구 동작만 본다(Keystore 연결은 기기에서 확인)
@RunWith(RobolectricTestRunner::class)
class SecureStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun newAead(): Aead {
        AeadConfig.register()
        return KeysetHandle
            .newBuilder()
            .addEntry(
                KeysetHandle
                    .generateEntryFromParameters(PredefinedAeadParameters.AES256_GCM)
                    .withRandomId()
                    .makePrimary(),
            ).build()
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    /** DataStore 하나를 열어 [block]을 돌린 뒤 닫는다. 같은 파일을 두 DataStore가 동시에 열 수 없다 */
    private suspend fun <T> withStore(
        aead: Aead,
        file: File,
        block: suspend (SecureStore) -> T,
    ): T {
        val job = Job()
        val store = SecureStore({ secureDataStore(aead, file, CoroutineScope(Dispatchers.IO + job)) })
        return try {
            block(store)
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun credentials(
        store: SecureStore,
        devToken: String = "",
        ids: Iterator<String> =
            generateSequence {
                java.util.UUID
                    .randomUUID()
                    .toString()
            }.iterator(),
    ) = StoredCredentials(store, devToken) { ids.next() }

    @Test
    fun valuesAreEncryptedOnDiskAndReadBack() =
        runTest {
            val aead = newAead()
            val file = folder.root.resolve("secure.json")
            withStore(aead, file) { it.update { v -> v.copy(accessKey = "key-plain-text") } }

            assertFalse(file.readBytes().decodeToString().contains("key-plain-text"))
            assertEquals("key-plain-text", withStore(aead, file) { it.read().accessKey })
        }

    @Test
    fun undecryptableFileIsReplacedWithEmptyValues() =
        runTest {
            val file = folder.root.resolve("secure.json")
            withStore(newAead(), file) { it.update { v -> v.copy(accessKey = "old") } }

            // 키셋을 잃은 경우: 다른 키로는 풀 수 없다
            val values = withStore(newAead(), file) { it.read() }
            assertEquals(SecureValues(), values)
        }

    @Test
    fun deviceIdIsCreatedOnceAndKeptAcrossLaunches() =
        runTest {
            val aead = newAead()
            val file = folder.root.resolve("secure.json")
            val first =
                withStore(aead, file) { store ->
                    val creds = credentials(store)
                    creds.deviceId().also { assertEquals(it, creds.deviceId()) }
                }
            assertTrue(first.matches(Regex("^[0-9a-f-]{36}$")))
            assertEquals(first, withStore(aead, file) { credentials(it).deviceId() })
        }

    @Test
    fun invalidStoredDeviceIdIsReplaced() =
        runTest {
            val aead = newAead()
            val file = folder.root.resolve("secure.json")
            val valid = "0f8fad5b-d9cb-469f-a165-70867728950e"
            val id =
                withStore(aead, file) { store ->
                    store.update { it.copy(deviceId = "NOT-A-UUID") }
                    credentials(store, ids = listOf(valid).iterator()).deviceId()
                }
            assertEquals(valid, id)
            assertEquals(valid, withStore(aead, file) { it.read().deviceId })
        }

    @Test
    fun storeFailureStillGivesOneDeviceIdForThisLaunch() =
        runTest {
            val broken = SecureStore({ error("keystore unavailable") })
            val creds = credentials(broken, devToken = "dev-token")
            val id = creds.deviceId()

            assertEquals(id, creds.deviceId())
            assertEquals("dev-token", creds.accessToken())
        }

    @Test
    fun storedAccessKeyWinsOverDevToken() =
        runTest {
            withStore(newAead(), folder.root.resolve("secure.json")) { store ->
                val creds = credentials(store, devToken = "dev-token")
                assertEquals("dev-token", creds.accessToken())

                creds.saveAccessKey("saved-key")
                assertEquals("saved-key", creds.accessToken())
                assertNotEquals(null, store.read().accessKey)
            }
        }

    @Test
    fun noKeyWithoutDevToken() =
        runTest {
            withStore(newAead(), folder.root.resolve("secure.json")) { store ->
                assertNull(credentials(store, devToken = "").accessToken())
            }
        }

    @Test
    fun keyStateFollowsLoadSaveAndRejection() =
        runTest {
            withStore(newAead(), folder.root.resolve("secure.json")) { store ->
                val creds = credentials(store)
                assertEquals(AccessKeyState(), creds.keyState.value)

                creds.loadAccessKey()
                assertEquals(AccessKeyState(loaded = true, hasKey = false), creds.keyState.value)
                // 키가 없으면 거절로 보지 않는다
                creds.reportRejected()
                assertFalse(creds.keyState.value.rejected)

                creds.saveAccessKey("saved-key")
                creds.reportRejected()
                assertEquals(AccessKeyState(loaded = true, hasKey = true, rejected = true), creds.keyState.value)
                // 새 키를 저장하면 거절 표시가 지워진다
                creds.saveAccessKey("new-key")
                assertFalse(creds.keyState.value.rejected)
                creds.reportRejected()
                creds.clearRejected()
                assertFalse(creds.keyState.value.rejected)
            }
        }

    @Test
    fun storedKeyIsReadOnceAtLoad() =
        runTest {
            val aead = newAead()
            val file = folder.root.resolve("secure.json")
            withStore(aead, file) { it.update { v -> v.copy(accessKey = "stored") } }
            withStore(aead, file) { store ->
                val creds = credentials(store)
                assertEquals("stored", creds.loadAccessKey())
                assertTrue(creds.keyState.value.hasKey)
                // 이후 저장소가 바뀌어도 메모리 값을 쓴다(D-077)
                store.update { it.copy(accessKey = null) }
                assertEquals("stored", creds.accessToken())
            }
        }

    @Test
    fun failedSaveKeepsTheOldKey() =
        runTest {
            val creds = credentials(SecureStore({ error("keystore unavailable") }), devToken = "dev-token")
            creds.loadAccessKey()
            val saved = runCatching { creds.saveAccessKey("new-key") }
            assertTrue(saved.isFailure)
            assertEquals("dev-token", creds.accessToken())
        }
}
