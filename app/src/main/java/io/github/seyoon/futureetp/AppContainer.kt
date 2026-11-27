package io.github.seyoon.futureetp

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.seyoon.futureetp.core.api.ApiClient
import io.github.seyoon.futureetp.core.api.createHttpClient
import io.github.seyoon.futureetp.core.api.patchSettings
import io.github.seyoon.futureetp.core.api.settings
import io.github.seyoon.futureetp.core.data.SettingsRepository
import io.github.seyoon.futureetp.core.query.appReturns
import io.github.seyoon.futureetp.core.settings.AppLocale
import io.github.seyoon.futureetp.core.settings.DisplaySettingsStore
import io.github.seyoon.futureetp.core.settings.createAppLocale
import io.github.seyoon.futureetp.core.settings.createDisplaySettingsStore
import io.github.seyoon.futureetp.core.storage.StoredCredentials
import io.github.seyoon.futureetp.core.storage.createSecureStore
import io.github.seyoon.futureetp.feature.settings.DeviceSettingsSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import java.util.TimeZone

/** 앱 전체에서 하나씩만 두는 의존성(AOS-005). ViewModel은 여기서 받은 것을 생성자로 넘겨받는다 */
class AppContainer(
    context: Context,
) {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL

    /** 화면 사이에 남는 조회·변경(repository)의 범위. 프로세스와 함께 끝난다 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 접근 키·installation ID(AOS-007). 저장된 키가 없으면 dev 변형만 DEV_API_TOKEN을 쓴다(D-043, D-044) */
    val credentials: StoredCredentials by lazy {
        StoredCredentials(
            createSecureStore(context),
            BuildConfig.DEV_API_TOKEN,
        )
    }

    /** 등락 색 등 기기 UI 설정(D-073, AOS-014) */
    val displaySettings: DisplaySettingsStore by lazy { createDisplaySettingsStore(context) }

    /** 앱 안 언어(per-app language, AOS-015) */
    val appLocale: AppLocale by lazy { createAppLocale(context) }

    /** 앱이 백그라운드에서 다시 올라올 때(조회 재요청, D-030) */
    val appReturns: Flow<Unit> by lazy { appReturns(ProcessLifecycleOwner.get().lifecycle) }

    val apiClient: ApiClient by lazy { ApiClient(createHttpClient(), apiBaseUrl, credentials) }

    /** 서버 기기 설정의 시간대·언어 맞추기(D-084, D-137). 보낸 값을 프로세스 동안 기억한다 */
    val deviceSettingsSync: DeviceSettingsSync by lazy {
        DeviceSettingsSync(
            fetch = { apiClient.settings() },
            patch = apiClient::patchSettings,
            deviceTimezone = { TimeZone.getDefault().id },
            appLanguage = { appLocale.current },
        )
    }

    /** 서버 기기 설정(B·B-a·A·C 공용, 낙관적 PATCH). 키가 있을 때 처음 쓰면서 조회한다 */
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            scope = appScope,
            fetch = { apiClient.settings() },
            patch = apiClient::patchSettings,
            appReturns = appReturns,
            invalidations = credentials.keySaves,
        )
    }
}
