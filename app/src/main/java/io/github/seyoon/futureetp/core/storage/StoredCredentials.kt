package io.github.seyoon.futureetp.core.storage

import android.util.Log
import io.github.seyoon.futureetp.core.api.ApiCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * 화면이 보는 접근 키 상태(RN `useAccessKeyStore`, D-077·D-079). 키 값은 넣지 않는다
 * @property loaded 저장된 키를 한 번 읽었음. 읽기 전에는 첫 화면(B-b 또는 A)을 고르지 않는다
 * @property rejected 저장된 키를 서버가 거절함(UNAUTHORIZED). 키는 새 키를 저장할 때까지 지우지 않는다
 */
data class AccessKeyState(
    val loaded: Boolean = false,
    val hasKey: Boolean = false,
    val rejected: Boolean = false,
)

private val UUID_V4 = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

/**
 * 요청 헤더 값을 보안 저장소에서 읽는다(D-044, D-120).
 * - installation ID: 처음 부를 때 저장값을 읽고, 없거나 UUID v4가 아니면 새로 만들어 저장한다.
 *   저장소 읽기·쓰기가 실패해도 이번 실행은 만든 값을 쓴다(다음 실행은 새 기기로 보임, RN `deviceId.ts`와 같음)
 * - 접근 키: 저장된 키, 없으면 [devToken](dev 변형 `DEV_API_TOKEN`, prod는 빈 값).
 *   처음 한 번만 저장소에서 읽어 메모리에 두고, 이후 요청은 메모리 값을 쓴다(D-077)
 */
class StoredCredentials(
    private val store: SecureStore,
    devToken: String,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ApiCredentials {
    private val devToken = devToken.ifEmpty { null }
    private val mutex = Mutex()
    private var cachedDeviceId: String? = null
    private val keyMutex = Mutex()
    private var key: String? = null
    private var keyLoaded = false
    private val _keyState = MutableStateFlow(AccessKeyState())
    val keyState: StateFlow<AccessKeyState> = _keyState.asStateFlow()
    private val _keySaves = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 새 키를 저장할 때마다 내보낸다. 조회들이 옛 키로 실패한 결과를 다시 불러온다(RN `invalidateQueries`, D-044) */
    val keySaves: SharedFlow<Unit> = _keySaves.asSharedFlow()

    override suspend fun deviceId(): String =
        mutex.withLock {
            cachedDeviceId ?: loadOrCreateDeviceId().also { cachedDeviceId = it }
        }

    override suspend fun accessToken(): String? = loadAccessKey()

    /** 앱 시작 때 부른다. 읽기 실패는 키 없음으로 보고 B-b에서 다시 넣게 한다(D-077) */
    suspend fun loadAccessKey(): String? =
        keyMutex.withLock {
            if (!keyLoaded) {
                key = readOrNull()?.accessKey ?: devToken
                keyLoaded = true
                _keyState.update { it.copy(loaded = true, hasKey = key != null) }
            }
            key
        }

    /** 서버에서 확인한 키만 넘긴다(D-044). 저장에 실패하면 예외를 그대로 던지고 메모리 값도 그대로 둔다 */
    suspend fun saveAccessKey(key: String) {
        store.update { it.copy(accessKey = key) }
        keyMutex.withLock {
            this.key = key
            keyLoaded = true
        }
        _keyState.value = AccessKeyState(loaded = true, hasKey = true, rejected = false)
        _keySaves.tryEmit(Unit)
    }

    /** 키가 없어 보내지 않은 요청은 거절이 아니라 저장된 키가 있을 때만 표시한다(D-079) */
    override fun reportRejected() {
        _keyState.update { if (it.hasKey) it.copy(rejected = true) else it }
    }

    /** B-b를 떠날 때 지운다. 옛 키가 다시 거절되면 또 B-b로 온다(D-079) */
    fun clearRejected() {
        _keyState.update { it.copy(rejected = false) }
    }

    private suspend fun loadOrCreateDeviceId(): String {
        val stored = readOrNull()?.deviceId
        if (stored != null && UUID_V4.matches(stored)) return stored
        val id = newId()
        try {
            store.update { it.copy(deviceId = id) }
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Log.w(TAG, "failed to save device id", e)
        }
        return id
    }

    /** 저장소를 열 수 없으면(Keystore 오류 등) 값 없음으로 본다 */
    private suspend fun readOrNull(): SecureValues? =
        try {
            store.read()
        } catch (e: CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Log.w(TAG, "failed to read secure store", e)
            null
        }

    private companion object {
        const val TAG = "StoredCredentials"
    }
}
