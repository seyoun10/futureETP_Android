package io.github.seyoon.futureetp

import io.github.seyoon.futureetp.core.api.ApiClient
import io.github.seyoon.futureetp.core.api.createHttpClient

/** 앱 전체에서 하나씩만 두는 의존성(AOS-005). ViewModel은 여기서 받은 것을 생성자로 넘겨받는다 */
class AppContainer {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
    val apiClient: ApiClient by lazy { ApiClient(createHttpClient(), apiBaseUrl) }
}
