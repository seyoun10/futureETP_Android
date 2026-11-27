package io.github.seyoon.futureetp.core.storage

import android.util.Log
import io.github.seyoon.futureetp.core.api.ApiCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private val UUID_V4 = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

/**
 * 요청 헤더 값을 보안 저장소에서 읽는다(D-044, D-120).
 * - installation ID: 처음 부를 때 저장값을 읽고, 없거나 UUID v4가 아니면 새로 만들어 저장한다.
 *   저장소 읽기·쓰기가 실패해도 이번 실행은 만든 값을 쓴다(다음 실행은 새 기기로 보임, RN `deviceId.ts`와 같음)
 * - 접근 키: 저장된 키, 없으면 [devToken](dev 변형 `DEV_API_TOKEN`, prod는 빈 값)
 */
class StoredCredentials(
    private val store: SecureStore,
    devToken: String,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ApiCredentials {
    private val devToken = devToken.ifEmpty { null }
    private val mutex = Mutex()
    private var cachedDeviceId: String? = null

    override suspend fun deviceId(): String =
        mutex.withLock {
            cachedDeviceId ?: loadOrCreateDeviceId().also { cachedDeviceId = it }
        }

    override suspend fun accessToken(): String? = readOrNull()?.accessKey ?: devToken

    /** 서버에서 확인한 키만 넘긴다(D-044). 저장에 실패하면 예외를 그대로 던진다 */
    suspend fun saveAccessKey(key: String) {
        store.update { it.copy(accessKey = key) }
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
