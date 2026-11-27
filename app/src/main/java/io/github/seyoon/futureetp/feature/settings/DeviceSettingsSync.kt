package io.github.seyoon.futureetp.feature.settings

import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** 서버가 받는 IANA 시간대인지(계약 `settingsPatch` timezone, RN `Intl.DateTimeFormat`으로 확인) */
fun isIanaTimezone(id: String): Boolean = id in ZoneId.getAvailableZoneIds()

/**
 * 기기 시간대와 앱 언어가 서버 값과 다르면 PATCH한다(product.md 푸시, D-048, D-084, D-137. RN `DeviceSettingsSync.tsx`).
 * 서버 언어는 푸시 문구용 사본이라(D-082) 앱 언어가 원본이다. 같은 값은 프로세스당 한 번만 보낸다(실패해도 되풀이하지 않음).
 * 프로세스에 하나만 두고(AppContainer), 키가 있는 동안 앱이 화면에 올라올 때마다 [sync]를 부른다
 */
class DeviceSettingsSync(
    private val fetch: suspend () -> DeviceSettings,
    private val patch: suspend (SettingsPatch) -> DeviceSettings,
    private val deviceTimezone: () -> String?,
    private val appLanguage: () -> AppLanguage,
) {
    private val mutex = Mutex()
    private var sentTimezone: String? = null
    private var sentLocale: AppLanguage? = null

    /** 설정을 받아 비교한다. 조회 실패는 무시한다(다음에 화면에 올라올 때 다시) */
    suspend fun sync() {
        val settings =
            try {
                fetch()
            } catch (_: AppError) {
                return
            }
        onSettings(settings)
    }

    /** 받은 설정과 비교해 다른 값만 한 번의 PATCH로 보낸다. 설정 화면이 받은 설정도 여기로 넘길 수 있다 */
    suspend fun onSettings(settings: DeviceSettings) {
        val body =
            mutex.withLock {
                val timezone =
                    deviceTimezone()?.takeIf {
                        it != settings.timezone &&
                            it != sentTimezone &&
                            isIanaTimezone(it)
                    }
                val locale = appLanguage().takeIf { it != settings.locale && it != sentLocale }
                timezone?.let { sentTimezone = it }
                locale?.let { sentLocale = it }
                SettingsPatch(locale = locale, timezone = timezone).takeIf { timezone != null || locale != null }
            } ?: return
        try {
            patch(body)
        } catch (_: AppError) {
            // 같은 값은 다시 보내지 않는다(RN과 같음)
        }
    }
}
