package io.github.seyoon.futureetp.feature.commoditylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.api.model.CommoditySummary
import io.github.seyoon.futureetp.core.api.model.ContractQuote
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.LocalUpDownPalette
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.theme.UpDownPalette
import io.github.seyoon.futureetp.core.theme.changePairColor
import io.github.seyoon.futureetp.core.ui.AlertBell
import io.github.seyoon.futureetp.core.ui.AutoRefreshEffect
import io.github.seyoon.futureetp.core.ui.FreshnessFooter
import io.github.seyoon.futureetp.core.ui.QueryStateView
import io.github.seyoon.futureetp.core.ui.ScreenHeader
import io.github.seyoon.futureetp.core.ui.SettingsHeaderButton
import io.github.seyoon.futureetp.core.ui.nameRes
import io.github.seyoon.futureetp.core.ui.rememberRefreshHaptic
import io.github.seyoon.futureetp.core.ui.rememberTapHaptic
import io.github.seyoon.futureetp.core.util.formatChange
import io.github.seyoon.futureetp.core.util.formatChangePct
import io.github.seyoon.futureetp.core.util.formatContractMonth
import io.github.seyoon.futureetp.core.util.formatPrice

/** 새로고침 중 화면 전체(헤더 포함) 터치를 막는 투명 층 */
const val REFRESH_BLOCKER_TAG = "refresh-touch-blocker"
const val COMMODITY_LIST_TAG = "commodity-list"

private const val APP_TITLE = "futureETP"
private val NAME_COLUMN_WIDTH = 104.dp
private val MONTH_WIDTH = 52.dp
private val CHANGE_WIDTH = 64.dp
private val PCT_WIDTH = 60.dp
private val NO_TRADE_WIDTH = CHANGE_WIDTH + PCT_WIDTH
private val CELL_STYLE =
    TextStyle(fontSize = FontSize.caption, textAlign = TextAlign.Center, fontFeatureSettings = "tnum")

data class CommodityListUiState(
    val list: QueryState<CommodityList>,
    val alertOff: Boolean,
    val pullRefreshing: Boolean,
    val refreshFailed: Boolean,
)

class CommodityListActions(
    val onRetry: () -> Unit,
    val onPullRefresh: () -> Unit,
    val onDismissRefreshFailed: () -> Unit,
    val onOpenCommodity: (CommodityCode) -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun CommodityListRoute(
    viewModel: CommodityListViewModel,
    onOpenCommodity: (CommodityCode) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val list by viewModel.state.collectAsStateWithLifecycle()
    val alertOff by viewModel.alertOff.collectAsStateWithLifecycle()
    val pullRefreshing by viewModel.pullRefreshing.collectAsStateWithLifecycle()
    val refreshFailed by viewModel.refreshFailed.collectAsStateWithLifecycle()
    AutoRefreshEffect(viewModel.query)
    CommodityListScreen(
        CommodityListUiState(list, alertOff, pullRefreshing, refreshFailed),
        CommodityListActions(
            onRetry = viewModel::retry,
            onPullRefresh = viewModel::pullRefresh,
            onDismissRefreshFailed = viewModel::dismissRefreshFailed,
            onOpenCommodity = onOpenCommodity,
            onOpenSettings = onOpenSettings,
        ),
    )
}

/** A 원자재 목록(screens.md A, D-085). 원자재와 월물 줄은 서버가 준 순서대로 그린다 */
@Composable
fun CommodityListScreen(
    state: CommodityListUiState,
    actions: CommodityListActions,
) {
    Box(Modifier.fillMaxSize().background(AppColors.background)) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(APP_TITLE, onBack = null) {
                SettingsHeaderButton(state.alertOff, actions.onOpenSettings)
            }
            Box(Modifier.weight(1f)) {
                QueryStateView(state.list, actions.onRetry) { data ->
                    Column(Modifier.fillMaxSize()) {
                        CommodityList(data, state.pullRefreshing, actions, Modifier.weight(1f))
                        FreshnessFooter(data.freshness)
                    }
                }
            }
        }
        if (state.pullRefreshing) RefreshTouchBlocker()
    }
    if (state.refreshFailed) RefreshFailedDialog(actions.onDismissRefreshFailed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommodityList(
    data: CommodityList,
    refreshing: Boolean,
    actions: CommodityListActions,
    modifier: Modifier,
) {
    val refreshHaptic = rememberRefreshHaptic()
    val pullState = rememberPullToRefreshState()
    val palette = LocalUpDownPalette.current
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshHaptic()
            actions.onPullRefresh()
        },
        modifier = modifier,
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = AppColors.surface,
                color = AppColors.textSecondary,
            )
        },
    ) {
        LazyColumn(Modifier.fillMaxSize().testTag(COMMODITY_LIST_TAG)) {
            items(data.commodities, key = { it.code }) { commodity ->
                CommodityRow(commodity, palette) { actions.onOpenCommodity(commodity.code) }
            }
        }
    }
}

/** 새로고침이 끝날 때까지 다른 터치를 막는다. 투명이라 목록 위 인디케이터는 보인다(D-138) */
@Composable
private fun RefreshTouchBlocker() {
    Box(
        Modifier
            .fillMaxSize()
            .testTag(REFRESH_BLOCKER_TAG)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    )
}

/** 공통 실패 안내(RN `showFailedAlert`). [확인]은 닫기만 하고, 다시 하려면 다시 당긴다 */
@Composable
private fun RefreshFailedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_confirm), color = AppColors.text) }
        },
        title = { Text(stringResource(R.string.commodity_list_refresh_failed_title)) },
        text = { Text(stringResource(R.string.commodity_list_refresh_failed_message)) },
        containerColor = AppColors.surface,
        titleContentColor = AppColors.text,
        textContentColor = AppColors.textSecondary,
    )
}

@Composable
private fun CommodityRow(
    commodity: CommoditySummary,
    palette: UpDownPalette,
    onClick: () -> Unit,
) {
    val haptic = rememberTapHaptic()
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    haptic()
                    onClick()
                }.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.width(NAME_COLUMN_WIDTH),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    "${stringResource(commodity.code.nameRes)}\n(${commodity.code.name})",
                    color = AppColors.text,
                    fontSize = FontSize.body,
                    textAlign = TextAlign.Center,
                )
                if (commodity.alert) AlertBell()
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                commodity.contracts.forEach { QuoteLine(it, commodity.priceDecimals, palette) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.separator))
    }
}

/** 월물 / 현재가 / 전일 대비 / 등락률. 오늘 값이 없으면 마지막 정산가와 "거래 없음"이다 */
@Composable
private fun QuoteLine(
    quote: ContractQuote,
    decimals: Int,
    palette: UpDownPalette,
) {
    val change = quote.change
    val color =
        if (quote.noTrade || change == null) {
            AppColors.neutral
        } else {
            changePairColor(change, quote.changePct ?: 0.0, decimals, palette)
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Cell(
            formatContractMonth(quote.contractMonth) + if (quote.isFront) "●" else " ",
            AppColors.textSecondary,
            MONTH_WIDTH,
        )
        Text(
            quote.price?.let { formatPrice(it, decimals) } ?: "-",
            Modifier.weight(1f),
            color = AppColors.text,
            style = CELL_STYLE,
        )
        if (quote.noTrade) {
            Cell(stringResource(R.string.commodity_list_no_trade), AppColors.neutral, NO_TRADE_WIDTH)
        } else {
            Cell(change?.let { formatChange(it, decimals) } ?: "-", color, CHANGE_WIDTH)
            Cell(quote.changePct?.let(::formatChangePct) ?: "-", color, PCT_WIDTH)
        }
    }
}

@Composable
private fun Cell(
    text: String,
    color: Color,
    width: Dp,
) {
    Text(text, Modifier.width(width), color = color, style = CELL_STYLE)
}
