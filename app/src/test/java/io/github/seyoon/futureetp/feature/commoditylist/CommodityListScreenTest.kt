package io.github.seyoon.futureetp.feature.commoditylist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.ui.HEADER_BADGE_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class CommodityListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<CommodityCode>()
    private var dismissed = 0
    private var settingsOpened = 0

    private fun show(
        alertOff: Boolean = false,
        pullRefreshing: Boolean = false,
        refreshFailed: Boolean = false,
    ) {
        compose.setContent {
            CommodityListScreen(
                CommodityListUiState(QueryState(data = commodityList()), alertOff, pullRefreshing, refreshFailed),
                CommodityListActions(
                    onRetry = {},
                    onPullRefresh = {},
                    onDismissRefreshFailed = { dismissed++ },
                    onOpenCommodity = { opened += it },
                    onOpenSettings = { settingsOpened++ },
                ),
            )
        }
    }

    @Test
    fun showsQuotesInServerOrder() {
        show()
        compose.onNodeWithText("금\n(GC)").assertExists()
        compose.onNodeWithText("DEC26●").assertExists()
        compose.onNodeWithText("OCT26 ").assertExists()
        compose.onNodeWithText("2,418.2").assertExists()
        compose.onNodeWithText("+12.1").assertExists()
        compose.onNodeWithText("+0.50%").assertExists()
        compose.onNodeWithText("-0.71%").assertExists()
    }

    @Test
    fun noTradeLineShowsLastSettlement() {
        show()
        compose.onNodeWithText("69.80").assertExists()
        compose.onNodeWithText("거래 없음").assertExists()
    }

    @Test
    fun bellOnlyOnAlertTarget() {
        show()
        compose.onNodeWithContentDescription("알림 대상", useUnmergedTree = true).assertExists()
        compose.onAllNodesWithTag(HEADER_BADGE_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun rowOpensCommodity() {
        show()
        compose.onNodeWithText("원유\n(CL)").performClick()
        assertEquals(listOf(CommodityCode.CL), opened)
    }

    @Test
    fun alertOffShowsDotOnSettings() {
        show(alertOff = true)
        compose.onNodeWithTag(HEADER_BADGE_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("설정", useUnmergedTree = true).performClick()
        assertEquals(1, settingsOpened)
    }

    @Test
    fun refreshingBlocksTouches() {
        show(pullRefreshing = true)
        compose.onNodeWithTag(REFRESH_BLOCKER_TAG).assertExists()
        compose.onNodeWithText("원유\n(CL)").performClick()
        compose.onNodeWithContentDescription("설정", useUnmergedTree = true).performClick()
        assertEquals(emptyList<CommodityCode>(), opened)
        assertEquals(0, settingsOpened)
    }

    @Test
    fun failureDialogOnlyCloses() {
        show(refreshFailed = true)
        compose.onNodeWithText("새로고침을 마치지 못했습니다").assertExists()
        compose.onNodeWithText("잠시 뒤 다시 당겨 주세요.").assertExists()
        compose.onNodeWithText("확인").performClick()
        assertEquals(1, dismissed)
    }
}
