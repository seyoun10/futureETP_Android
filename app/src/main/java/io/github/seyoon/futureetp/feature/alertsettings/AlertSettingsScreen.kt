package io.github.seyoon.futureetp.feature.alertsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.Change
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.push.NotificationPermission
import io.github.seyoon.futureetp.core.push.rememberNotificationPermission
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.ui.ErrorView
import io.github.seyoon.futureetp.core.ui.SaveFailedLine
import io.github.seyoon.futureetp.core.ui.ScreenHeader
import io.github.seyoon.futureetp.core.ui.SectionHeader
import io.github.seyoon.futureetp.core.ui.SegmentOption
import io.github.seyoon.futureetp.core.ui.SegmentedControl
import io.github.seyoon.futureetp.core.ui.nameRes
import io.github.seyoon.futureetp.core.ui.rememberTapHaptic
import io.github.seyoon.futureetp.core.util.formatRareLevel

private val ERROR_MIN_HEIGHT = 160.dp

@Composable
fun AlertSettingsRoute(
    viewModel: AlertSettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val permission = rememberNotificationPermission()
    AlertSettingsScreen(
        state = AlertSettingsUiState(settings, saveError, permission.status),
        actions =
            AlertSettingsActions(
                onChange = viewModel::update,
                onRetry = viewModel::retry,
                onRequestPermission = permission.request,
            ),
        onBack = onBack,
    )
}

data class AlertSettingsUiState(
    val settings: QueryState<DeviceSettings>,
    val saveError: Throwable?,
    /** 확인 전이면 null(배너를 숨긴다) */
    val permission: NotificationPermission?,
)

data class AlertSettingsActions(
    val onChange: (SettingsPatch) -> Unit,
    val onRetry: () -> Unit,
    val onRequestPermission: () -> Unit,
)

/**
 * B-a 알림 설정(screens.md B-a, RN `AlertSettingsScreen`). 바꾼 값은 바로 서버에 저장한다.
 * 적용 기준은 B 이격도차트 설정을 그대로 쓰므로 여기서는 요약만 보여 준다. 선택지 "없음"은 알림 끄기(alertCommodity null)다
 */
@Composable
fun AlertSettingsScreen(
    state: AlertSettingsUiState,
    actions: AlertSettingsActions,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        ScreenHeader(stringResource(R.string.alert_settings_title), onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            state.permission?.takeUnless { it.granted }?.let { PermissionBanner(it, actions.onRequestPermission) }
            state.saveError?.let { SaveFailedLine(it) }
            val settings = state.settings
            val data = settings.data
            val error = settings.error
            if (data == null && error != null && !settings.isFetching) {
                ErrorView(error, actions.onRetry, Modifier.heightIn(min = ERROR_MIN_HEIGHT))
            } else {
                CommodityRow(data, actions.onChange)
                if (data != null) {
                    CriteriaSummary(data)
                    QuietTimeSection(data.quietTime) { actions.onChange(SettingsPatch(quietTime = Change(it))) }
                }
            }
            Text(
                stringResource(R.string.alert_settings_guide),
                modifier = Modifier.padding(Spacing.lg),
                color = AppColors.textSecondary,
                fontSize = FontSize.caption,
            )
        }
    }
}

/** 대상 원자재. 조회 중에는 아무것도 고르지 않는다. null 선택지가 "없음"이다 */
@Composable
private fun CommodityRow(
    settings: DeviceSettings?,
    onChange: (SettingsPatch) -> Unit,
) {
    val none = stringResource(R.string.alert_settings_commodity_none)
    // 조회 중(settings null)과 "없음"(alertCommodity null)을 구분하려고 선택값을 Change로 감싼다
    SegmentedControl(
        label = stringResource(R.string.alert_settings_commodity),
        options =
            (listOf(null) + CommodityCode.entries).map { code ->
                SegmentOption(Change(code), code?.let { stringResource(it.nameRes) } ?: none)
            },
        value = settings?.let { Change(it.alertCommodity) },
        onChange = { onChange(SettingsPatch(alertCommodity = it)) },
        stacked = true,
    )
}

@Composable
private fun CriteriaSummary(settings: DeviceSettings) {
    val basis =
        stringResource(
            when (settings.basis) {
                Basis.FRONT -> R.string.settings_basis_front
                Basis.KR -> R.string.settings_basis_kr
                Basis.US -> R.string.settings_basis_us
            },
        )
    val window =
        if (settings.window == ChartWindow.ALL) {
            stringResource(R.string.settings_window_all)
        } else {
            stringResource(R.string.alert_settings_criteria_window_years, settings.window.name.drop(1))
        }
    SectionHeader(stringResource(R.string.alert_settings_criteria))
    CriteriaLine(stringResource(R.string.alert_settings_criteria_chart, basis, settings.maPeriod.days, window))
    CriteriaLine(
        stringResource(
            R.string.alert_settings_criteria_levels,
            settings.rareLevels.joinToString("/", transform = ::formatRareLevel),
        ),
    )
}

@Composable
private fun CriteriaLine(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        color = AppColors.text,
        fontSize = FontSize.body,
    )
}

/** 권한이 꺼져 있을 때만 보인다. OS가 다시 물을 수 있으면 [허용], 아니면 [설정 열기] */
@Composable
private fun PermissionBanner(
    permission: NotificationPermission,
    onRequest: () -> Unit,
) {
    val haptic = rememberTapHaptic()
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.alert_settings_permission_off),
            modifier = Modifier.weight(1f),
            color = AppColors.text,
            fontSize = FontSize.body,
        )
        Box(
            Modifier
                .heightIn(min = Size.touchTarget)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(AppColors.surfaceSelected)
                .clickable(role = Role.Button) {
                    haptic()
                    onRequest()
                }.padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(
                    if (permission.canAsk) R.string.alert_settings_allow else R.string.alert_settings_open_settings,
                ),
                color = AppColors.text,
                fontSize = FontSize.body,
            )
        }
    }
}
