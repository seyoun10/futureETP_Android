package io.github.seyoon.futureetp

import android.content.Context
import io.github.seyoon.futureetp.core.api.ApiClient
import io.github.seyoon.futureetp.core.api.createHttpClient
import io.github.seyoon.futureetp.core.storage.StoredCredentials
import io.github.seyoon.futureetp.core.storage.createSecureStore

/** 앱 전체에서 하나씩만 두는 의존성(AOS-005). ViewModel은 여기서 받은 것을 생성자로 넘겨받는다 */
class AppContainer(
    context: Context,
) {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL

    /** 접근 키·installation ID(AOS-007). 저장된 키가 없으면 dev 변형만 DEV_API_TOKEN을 쓴다(D-043, D-044) */
    val credentials: StoredCredentials by lazy {
        StoredCredentials(
            createSecureStore(context),
            BuildConfig.DEV_API_TOKEN,
        )
    }

    val apiClient: ApiClient by lazy { ApiClient(createHttpClient(), apiBaseUrl, credentials) }
}
