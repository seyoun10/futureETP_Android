package io.github.seyoon.futureetp.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.model.Freshness
import io.github.seyoon.futureetp.core.api.model.MarketState
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.util.formatLocalDateTime
import io.github.seyoon.futureetp.core.util.formatTradeDate
import java.time.Instant

// 줄 높이를 최소로 둔다(글자 높이 + 위아래 1dp, RN FooterLine)
private val LINE_HEIGHT = (FontSize.caption.value + 2).sp
private val LINE_BLOCK = (FontSize.caption.value + 4).dp

@StringRes
private fun MarketState.labelRes(): Int =
    when (this) {
        MarketState.OPEN -> R.string.freshness_market_state_open
        MarketState.CLOSED -> R.string.freshness_market_state_closed
        MarketState.SETTLEMENT_PENDING -> R.string.freshness_market_state_settlement_pending
    }

/**
 * A·C 화면 최하단 기준 시각 줄: `10/22 09:00 기준 · 거래일 10/22 · 지연 · 장중`(screens.md 공통 규칙, RN `FreshnessLine`).
 * 새로고침이 실패해도 마지막 데이터의 기준 시각을 그대로 보여 줘 오래됨을 알린다.
 * freshness가 없어도(탭 전환 중·첫 조회 중) 빈 줄로 자리를 지킨다
 */
@Composable
fun FreshnessFooter(freshness: Freshness?) {
    val text =
        freshness?.let {
            listOfNotNull(
                stringResource(R.string.freshness_as_of, formatLocalDateTime(it.asOf)),
                stringResource(R.string.freshness_trade_date, formatTradeDate(it.tradeDate)),
                if (it.delayed) stringResource(R.string.freshness_delayed) else null,
                stringResource(it.marketState.labelRes()),
            ).joinToString(" · ")
        }
    FooterLine(text)
}

/** 거래일·장 상태가 없는 화면(ETP 목록)은 기준 시각만. 수집 전(null)이면 빈 줄 */
@Composable
fun AsOfFooter(asOf: Instant?) {
    FooterLine(asOf?.let { stringResource(R.string.freshness_as_of, formatLocalDateTime(it)) })
}

/**
 * 화면 최하단 안내 한 줄(A·C 기준 시각 줄, B 앱 번들 줄, RN `FooterLine`). 높이를 아끼려고 내비게이션 바 여백 위쪽에 겹쳐 둔다.
 * 내용이 없어도 같은 Text로 빈 줄을 그려 위의 tab-bar가 흔들리거나 탭마다 위치가 달라지지 않게 한다
 */
@Composable
fun FooterLine(text: String?) {
    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    Text(
        text ?: " ",
        modifier =
            Modifier
                .fillMaxWidth()
                .background(AppColors.background)
                .padding(bottom = (bottomInset - LINE_BLOCK).coerceAtLeast(0.dp))
                .padding(horizontal = Spacing.lg, vertical = 1.dp),
        color = AppColors.textSecondary,
        fontSize = FontSize.caption,
        lineHeight = LINE_HEIGHT,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
