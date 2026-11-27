package io.github.seyoon.futureetp.feature.settings

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.settings.UpDownStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val network = AppError(AppErrorCode.NETWORK, "offline")
    private val server =
        DeviceSettings(
            locale = AppLanguage.KO,
            timezone = "Asia/Seoul",
            basis = Basis.FRONT,
            maPeriod = MaPeriod.MA100,
            window = ChartWindow.Y10,
            rareLevels = listOf(1.0, 2.0, 4.0, 8.0, 16.0),
            alertCommodity = CommodityCode.CL,
            quietTime = null,
        )
    private val patches = mutableListOf<SettingsPatch>()
    private var resets = 0
    private var retries = 0

    private fun show(
        settings: QueryState<DeviceSettings> = QueryState(data = server),
        saveError: Throwable? = null,
    ) {
        compose.setContent {
            SettingsScreen(
                state =
                    SettingsUiState(
                        settings = settings,
                        saveError = saveError,
                        language = AppLanguage.KO,
                        upDownStyle = UpDownStyle.KR,
                        hasAccessKey = true,
                        bundleText = "AOS dev: 1.0.0 (빌드 1)",
                    ),
                actions =
                    SettingsActions(
                        onLanguage = {},
                        onUpDownStyle = {},
                        onChange = { patches += it },
                        onRetry = { retries++ },
                        onReset = { resets++ },
                    ),
                nav = SettingsNav(onBack = null, onOpenAlerts = {}, onOpenAccessKey = {}, onOpenDataSources = {}),
            )
        }
    }

    @Test
    fun showsServerValuesAndAlertTarget() {
        show()
        compose.onNodeWithText("대표월물").assertIsSelected()
        compose.onNodeWithText("원유").performScrollTo()
        compose.onNodeWithText("설정됨").performScrollTo()
        compose.onNodeWithText("AOS dev: 1.0.0 (빌드 1)").assertExists()
    }

    @Test
    fun nothingIsSelectedWhileLoading() {
        show(QueryState(isFetching = true))
        compose.onNodeWithText("대표월물").assertIsNotSelected()
    }

    @Test
    fun choosingAnotherValuePatchesOnlyThatField() {
        show()
        compose.onNodeWithText("한국형").performClick()
        compose.onNodeWithText("대표월물").performClick()
        assertEquals(listOf(SettingsPatch(basis = Basis.KR)), patches)
    }

    @Test
    fun firstLoadFailureShowsRetryInDisparitySection() {
        show(QueryState(error = network))
        compose.onNodeWithText("다시 시도").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun saveFailureLineShowsReason() {
        show(saveError = network)
        compose.onNodeWithText("저장하지 못했습니다. 네트워크에 연결할 수 없습니다.").assertExists()
    }

    @Test
    fun invalidRareLevelShowsErrorAndDoesNotSave() {
        show()
        val field = compose.onNodeWithContentDescription("rare-level 1")
        field.performScrollTo().performTextReplacement("50")
        field.performImeAction()
        compose.waitForIdle()
        compose.onNodeWithText("0.1~49.9 사이 값을 소수 첫째 자리까지 입력해 주세요.").assertExists()
        assertEquals(emptyList<SettingsPatch>(), patches)
    }

    @Test
    fun validRareLevelsSaveSorted() {
        show()
        val field = compose.onNodeWithContentDescription("rare-level 1")
        field.performScrollTo().performTextReplacement("20")
        field.performImeAction()
        compose.waitForIdle()
        assertEquals(listOf(SettingsPatch(rareLevels = listOf(2.0, 4.0, 8.0, 16.0, 20.0))), patches)
    }

    @Test
    fun resetAsksFirst() {
        show()
        compose.onNodeWithContentDescription("초기화").performClick()
        compose.onNodeWithText("취소").performClick()
        assertEquals(0, resets)
        compose.onNodeWithContentDescription("초기화").performClick()
        compose.onNodeWithText("초기화").performClick()
        assertEquals(1, resets)
    }
}
