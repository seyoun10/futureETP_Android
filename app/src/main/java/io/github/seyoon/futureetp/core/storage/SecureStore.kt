package io.github.seyoon.futureetp.core.storage

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private const val FILE_NAME = "secure.json"
private const val KEYSET_NAME = "secure_keyset"
private const val KEYSET_PREFS = "secure_keyset_prefs"
private const val MASTER_KEY_URI = "android-keystore://futureetp_secure_master_key"

/** 보안 저장소에 두는 값 전부(AOS-007). 접근 키(D-044)와 installation ID(D-120)만 둔다 */
@Serializable
data class SecureValues(
    val accessKey: String? = null,
    val deviceId: String? = null,
)

/** 암호화 전 평문 형식. 모르는 필드는 버리고 읽는다 */
internal object SecureValuesSerializer : Serializer<SecureValues> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = SecureValues()

    override suspend fun readFrom(input: InputStream): SecureValues {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return defaultValue
        return try {
            json.decodeFromString(SecureValues.serializer(), bytes.decodeToString())
        } catch (e: SerializationException) {
            throw CorruptionException("Unreadable secure values", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Unreadable secure values", e)
        }
    }

    override suspend fun writeTo(
        t: SecureValues,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(SecureValues.serializer(), t).encodeToByteArray())
    }
}

/**
 * 파일 하나를 [aead]로 암호화하는 DataStore. 복호화·해석에 실패하면(키셋을 잃음 등) 빈 값으로 바꾼다:
 * installation ID는 새로 만들고 접근 키는 B-b에서 다시 넣는다(재설치와 같음, AOS-007)
 */
internal fun secureDataStore(
    aead: Aead,
    file: File,
    scope: CoroutineScope,
): DataStore<SecureValues> =
    DataStoreFactory.create(
        serializer = AeadSerializer(aead, SecureValuesSerializer, associatedData = FILE_NAME.encodeToByteArray()),
        corruptionHandler = ReplaceFileCorruptionHandler { SecureValues() },
        scope = scope,
        produceFile = { file },
    )

/**
 * 접근 키·installation ID 저장소(AOS-007). 첫 접근 때 [createDataStore]를 [io]에서 부른다
 * (Keystore·키셋 준비가 디스크를 읽어 메인 스레드에서 하지 않는다)
 */
class SecureStore(
    createDataStore: () -> DataStore<SecureValues>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val dataStore by lazy(createDataStore)

    suspend fun read(): SecureValues = withContext(io) { dataStore.data.first() }

    suspend fun update(transform: (SecureValues) -> SecureValues) {
        withContext(io) { dataStore.updateData(transform) }
    }
}

/** 앱이 쓰는 보안 저장소. 키셋은 Android Keystore 마스터 키로 감싸 SharedPreferences에 둔다(백업 제외) */
fun createSecureStore(context: Context): SecureStore {
    val appContext = context.applicationContext
    return SecureStore({
        AeadConfig.register()
        val aead =
            AndroidKeysetManager
                .Builder()
                .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREFS)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        secureDataStore(
            aead,
            appContext.dataStoreFile(FILE_NAME),
            CoroutineScope(Dispatchers.IO + SupervisorJob()),
        )
    })
}
