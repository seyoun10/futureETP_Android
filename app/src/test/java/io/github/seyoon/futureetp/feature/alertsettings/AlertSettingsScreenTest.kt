package io.github.seyoon.futureetp.feature.alertsettings

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.Change
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.QuietTime
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.push.NotificationPermission
import io.github.seyoon.futureetp.core.query.QueryState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class AlertSettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val server =
        DeviceSettings(
            locale = AppLanguage.KO,
            timezone = "Asia/Seoul",
            basis = Basis.FRONT,
            maPeriod = MaPeriod.MA100,
            window = ChartWindow.Y10,
            rareLevels = listOf(1.0, 2.0, 4.0, 8.0, 16.0),
            alertCommodity = CommodityCode.CL,
            quietTime = QuietTime(startMin = 1380, endMin = 420),
        )
    private val patches = mutableListOf<SettingsPatch>()
    private var retries = 0
    private var permissionRequests = 0

    private fun show(
        settings: QueryState<DeviceSettings> = QueryState(data = server),
        permission: NotificationPermission? = NotificationPermission(granted = true, canAsk = false),
    ) {
        compose.setContent {
            AlertSettingsScreen(
                state = AlertSettingsUiState(settings, saveError = null, permission = permission),
                actions =
                    AlertSettingsActions(
                        onChange = { patches += it },
                        onRetry = { retries++ },
                        onRequestPermission = { permissionRequests++ },
                    ),
                onBack = {},
            )
        }
    }

    @Test
    fun showsTargetAndCriteriaSummary() {
        show()
        compose.onNodeWithText("원유").assertIsSelected()
        compose.onNodeWithText("기준값 대표월물 · 100일선 · 10년").assertExists()
        compose.onNodeWithText("레벨 1/2/4/8/16% (위·아래)").assertExists()
        compose.onNodeWithContentDescription("종료 다음 날 07:00").assertExists()
    }

    @Test
    fun nothingIsSelectedWhileLoading() {
        show(QueryState(isFetching = true))
        compose.onNodeWithText("없음").assertIsNotSelected()
        compose.onNodeWithText("원유").assertIsNotSelected()
    }

    @Test
    fun noneTurnsAlertsOff() {
        show()
        compose.onNodeWithText("없음").performClick()
        assertEquals(listOf(SettingsPatch(alertCommodity = Change(null))), patches)
    }

    @Test
    fun quietTimeSwitchOffSavesNull() {
        show()
        compose.onNodeWithContentDescription("알림 거부 시간").performScrollTo().performClick()
        assertEquals(listOf(SettingsPatch(quietTime = Change(null))), patches)
    }

    @Test
    fun quietTimeSwitchOnSavesDefault() {
        show(QueryState(data = server.copy(quietTime = null)))
        compose.onNodeWithContentDescription("알림 거부 시간").performScrollTo().performClick()
        assertEquals(listOf(SettingsPatch(quietTime = Change(DEFAULT_QUIET_TIME))), patches)
    }

    @Test
    fun endSheetBlocksStartAndSavesPick() {
        show()
        compose.onNodeWithContentDescription("종료 다음 날 07:00").performScrollTo().performClick()
        compose.onNode(hasText("23:00") and isSelectable()).assertIsNotEnabled()
        compose.onNode(hasText("07:00") and isSelectable()).assertIsSelected()
        compose.onNode(hasText("08:00") and isSelectable()).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf(SettingsPatch(quietTime = Change(QuietTime(startMin = 1380, endMin = 480)))), patches)
    }

    @Test
    fun permissionBannerAsksWhenOsCanAsk() {
        show(permission = NotificationPermission(granted = false, canAsk = true))
        compose.onNodeWithText("허용").performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    fun permissionBannerOpensSettingsWhenOsWillNotAsk() {
        show(permission = NotificationPermission(granted = false, canAsk = false))
        compose.onNodeWithText("설정 열기").performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    fun grantedOrUnknownPermissionHidesBanner() {
        show(permission = null)
        compose.onNodeWithText("⚠ 알림 권한이 꺼져 있습니다").assertDoesNotExist()
    }

    @Test
    fun firstLoadFailureShowsRetry() {
        show(QueryState(error = AppError(AppErrorCode.NETWORK, "offline")))
        compose.onNodeWithText("다시 시도").performClick()
        assertEquals(1, retries)
    }
}
