package io.github.seyoon.futureetp.feature.settings

import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// RN `DeviceSettingsSync.test.tsx`와 같은 경우를 본다
class DeviceSettingsSyncTest {
    private val server =
        DeviceSettings(
            locale = AppLanguage.KO,
            timezone = "Asia/Seoul",
            basis = Basis.FRONT,
            maPeriod = MaPeriod.MA100,
            window = ChartWindow.Y10,
            rareLevels = listOf(5.0, 10.0, 15.0, 20.0, 25.0),
            alertCommodity = CommodityCode.CL,
            quietTime = null,
        )

    private class Fixture(
        var settings: DeviceSettings,
        var timezone: String? = "Asia/Seoul",
        var language: AppLanguage = AppLanguage.KO,
        var fetchError: AppError? = null,
        var patchError: AppError? = null,
    ) {
        val patches = mutableListOf<SettingsPatch>()
        val sync =
            DeviceSettingsSync(
                fetch = { fetchError?.let { throw it } ?: settings },
                patch = { body ->
                    patches += body
                    patchError?.let { throw it }
                    settings.copy(
                        timezone = body.timezone ?: settings.timezone,
                        locale = body.locale ?: settings.locale,
                    )
                },
                deviceTimezone = { timezone },
                appLanguage = { language },
            )
    }

    @Test
    fun sendsOnlyTimezoneWhenItDiffers() =
        runTest {
            val f = Fixture(server, timezone = "Europe/London")
            f.sync.sync()
            assertEquals(listOf(SettingsPatch(timezone = "Europe/London")), f.patches)
        }

    @Test
    fun skipsTimezoneThatIsSameMissingOrNotIana() =
        runTest {
            listOf("Asia/Seoul", null, "Not/AZone").forEach { tz ->
                val f = Fixture(server, timezone = tz)
                f.sync.sync()
                assertEquals("timezone=$tz", emptyList<SettingsPatch>(), f.patches)
            }
        }

    @Test
    fun sendsLocaleAndTimezoneTogether() =
        runTest {
            val f = Fixture(server, timezone = "Europe/London", language = AppLanguage.EN)
            f.sync.sync()
            assertEquals(listOf(SettingsPatch(locale = AppLanguage.EN, timezone = "Europe/London")), f.patches)
        }

    @Test
    fun doesNotResendSameValuesEvenAfterFailure() =
        runTest {
            val f = Fixture(server, language = AppLanguage.EN, patchError = AppError(AppErrorCode.NETWORK))
            f.sync.sync()
            f.sync.sync()
            assertEquals(listOf(SettingsPatch(locale = AppLanguage.EN)), f.patches)
        }

    @Test
    fun sendsAgainWhenAppLanguageChangesLater() =
        runTest {
            val f = Fixture(server)
            f.sync.sync()
            f.language = AppLanguage.EN
            f.sync.sync()
            assertEquals(listOf(SettingsPatch(locale = AppLanguage.EN)), f.patches)
        }

    @Test
    fun ignoresFetchFailure() =
        runTest {
            val f = Fixture(server, timezone = "Europe/London", fetchError = AppError(AppErrorCode.UNAUTHORIZED))
            f.sync.sync()
            assertEquals(emptyList<SettingsPatch>(), f.patches)
        }
}
