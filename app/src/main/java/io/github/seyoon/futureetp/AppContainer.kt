package io.github.seyoon.futureetp

import io.github.seyoon.futureetp.core.api.ApiClient
import io.github.seyoon.futureetp.core.api.ApiCredentials
import io.github.seyoon.futureetp.core.api.createHttpClient
import java.util.UUID

/** 앱 전체에서 하나씩만 두는 의존성(AOS-005). ViewModel은 여기서 받은 것을 생성자로 넘겨받는다 */
class AppContainer {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
    val apiClient: ApiClient by lazy { ApiClient(createHttpClient(), apiBaseUrl, credentials) }

    // 임시: 접근 키는 dev 변형 DEV_API_TOKEN만, installation ID는 실행마다 새로 만든다.
    // 3단계 installation ID·접근 키 B-b 항목에서 보안 저장소(datastore-tink, AOS-007)로 바꾼다
    private val credentials =
        object : ApiCredentials {
            private val sessionDeviceId = UUID.randomUUID().toString()

            override suspend fun accessToken(): String? = BuildConfig.DEV_API_TOKEN.ifEmpty { null }

            override suspend fun deviceId(): String = sessionDeviceId
        }
}
