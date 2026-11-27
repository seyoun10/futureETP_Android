package io.github.seyoon.futureetp.core.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.Freshness
import io.github.seyoon.futureetp.core.api.model.MarketState
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.util.formatLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class QueryStateViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val network = AppError(AppErrorCode.NETWORK, "offline")

    @Test
    fun dataWinsOverRefreshError() {
        compose.setContent {
            QueryStateView(QueryState(data = "목록", error = network), onRetry = {}) { Text(it) }
        }
        compose.onNodeWithText("목록").assertIsDisplayed()
    }

    @Test
    fun firstLoadFailureShowsMessageAndRetry() {
        var retries = 0
        compose.setContent {
            QueryStateView(QueryState<String>(error = network), onRetry = { retries++ }) { Text(it) }
        }
        compose.onNodeWithText("네트워크에 연결할 수 없습니다.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun retryingAfterFailureShowsLoading() {
        compose.setContent {
            QueryStateView(QueryState<String>(error = network, isFetching = true), onRetry = {}) { Text(it) }
        }
        compose.onNodeWithContentDescription("불러오는 중").assertIsDisplayed()
    }

    @Test
    fun freshnessLineJoinsItems() {
        val asOf = Instant.parse("2026-10-22T14:00:00Z")
        val freshness = Freshness(asOf, LocalDate.of(2026, 10, 22), true, MarketState.OPEN, null)
        compose.setContent { FreshnessFooter(freshness) }
        compose.onNodeWithText("${formatLocalDateTime(asOf)} 기준 · 거래일 10/22 · 지연 · 장중").assertIsDisplayed()
    }

    @Test
    fun asOfFooterShowsTimeOnly() {
        val asOf = Instant.parse("2026-10-22T14:00:00Z")
        compose.setContent { AsOfFooter(asOf) }
        compose.onNodeWithText("${formatLocalDateTime(asOf)} 기준").assertIsDisplayed()
    }
}
