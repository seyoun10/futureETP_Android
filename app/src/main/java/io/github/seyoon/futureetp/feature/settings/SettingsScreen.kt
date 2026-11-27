package io.github.seyoon.futureetp.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.errorTextRes
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.settings.UpDownStyle
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.ui.ErrorView
import io.github.seyoon.futureetp.core.ui.FooterLine
import io.github.seyoon.futureetp.core.ui.HeaderIconButton
import io.github.seyoon.futureetp.core.ui.RowDescription
import io.github.seyoon.futureetp.core.ui.ScreenHeader
import io.github.seyoon.futureetp.core.ui.SectionHeader
import io.github.seyoon.futureetp.core.ui.SegmentOption
import io.github.seyoon.futureetp.core.ui.SegmentedControl
import io.github.seyoon.futureetp.core.ui.nameRes
import io.github.seyoon.futureetp.core.ui.rememberTapHaptic

/** 화면 밖에서 정하는 값과 이동 */
data class SettingsNav(
    val onBack: (() -> Unit)?,
    val onOpenAlerts: () -> Unit,
    val onOpenAccessKey: () -> Unit,
    val onOpenDataSources: () -> Unit,
)

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    hasAccessKey: Boolean,
    bundleText: String,
    nav: SettingsNav,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val upDownStyle by viewModel.upDownStyle.collectAsStateWithLifecycle()
    SettingsScreen(
        state =
            SettingsUiState(
                settings = settings,
                saveError = saveError,
                language = viewModel.language,
                upDownStyle = upDownStyle,
                hasAccessKey = hasAccessKey,
                bundleText = bundleText,
            ),
        actions =
            SettingsActions(
                onLanguage = viewModel::setLanguage,
                onUpDownStyle = viewModel::setUpDownStyle,
                onChange = viewModel::update,
                onRetry = viewModel::retry,
                onReset = viewModel::reset,
            ),
        nav = nav,
    )
}

data class SettingsUiState(
    val settings: QueryState<DeviceSettings>,
    val saveError: Throwable?,
    val language: AppLanguage,
    val upDownStyle: UpDownStyle?,
    val hasAccessKey: Boolean,
    val bundleText: String,
)

data class SettingsActions(
    val onLanguage: (AppLanguage) -> Unit,
    val onUpDownStyle: (UpDownStyle) -> Unit,
    val onChange: (SettingsPatch) -> Unit,
    val onRetry: () -> Unit,
    val onReset: () -> Unit,
)

/**
 * B 설정(screens.md B). 맨 아래 번들 줄은 스크롤 밖 화면 최하단에 고정하고, 키패드가 올라오면 키패드가 덮는다.
 * 스크롤 영역만 키패드 높이(번들 줄 높이를 뺀 만큼)를 비켜 입력칸을 끌어올릴 수 있게 한다
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    nav: SettingsNav,
) {
    var confirmingReset by remember { mutableStateOf(false) }
    var footerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val scrollBottom = with(density) { (imeBottom - footerHeight).coerceAtLeast(0).toDp() }

    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        ScreenHeader(stringResource(R.string.settings_title), onBack = nav.onBack) {
            HeaderIconButton(R.drawable.ic_restore, stringResource(R.string.settings_reset)) { confirmingReset = true }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = scrollBottom)
                .verticalScroll(rememberScrollState()),
        ) {
            state.saveError?.let { SaveFailedLine(it) }
            DisplaySection(state, actions)
            SectionHeader(stringResource(R.string.settings_section_disparity))
            val settings = state.settings
            val error = settings.error
            if (settings.data == null && error != null && !settings.isFetching) {
                ErrorView(error, actions.onRetry, Modifier.heightIn(min = ERROR_MIN_HEIGHT))
            } else {
                DisparitySection(settings.data, actions.onChange, nav.onOpenAlerts)
            }
            SectionHeader(stringResource(R.string.settings_section_other))
            LinkRow(
                label = stringResource(R.string.settings_access_key),
                value = if (state.hasAccessKey) stringResource(R.string.access_key_saved) else null,
                description = stringResource(R.string.settings_access_key_desc),
                onClick = nav.onOpenAccessKey,
            )
            LinkRow(
                label = stringResource(R.string.settings_data_sources),
                description = stringResource(R.string.settings_data_sources_desc),
                onClick = nav.onOpenDataSources,
            )
        }
        Box(Modifier.onSizeChanged { footerHeight = it.height }) { FooterLine(state.bundleText) }
    }
    if (confirmingReset) {
        ResetDialog(
            onConfirm = {
                confirmingReset = false
                actions.onReset()
            },
            onDismiss = { confirmingReset = false },
        )
    }
}

private val ERROR_MIN_HEIGHT = 160.dp

@Composable
private fun SaveFailedLine(error: Throwable) {
    Text(
        stringResource(R.string.settings_save_failed, stringResource(errorTextRes(error))),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .semantics { liveRegion = LiveRegionMode.Polite },
        color = AppColors.error,
        fontSize = FontSize.caption,
        textAlign = TextAlign.Center,
    )
}

/** 언어·등락 색은 기기 설정이다. 언어는 푸시 문구에 쓰므로 서버에도 보낸다 */
@Composable
private fun DisplaySection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    SectionHeader(stringResource(R.string.settings_section_general))
    SegmentedControl(
        label = stringResource(R.string.settings_language),
        options =
            listOf(
                SegmentOption(AppLanguage.KO, stringResource(R.string.settings_language_ko)),
                SegmentOption(AppLanguage.EN, stringResource(R.string.settings_language_en)),
            ),
        value = state.language,
        onChange = actions.onLanguage,
        description = stringResource(R.string.settings_language_desc),
    )
    SegmentedControl(
        label = stringResource(R.string.settings_up_down_style),
        options =
            listOf(
                SegmentOption(UpDownStyle.KR, stringResource(R.string.settings_up_down_style_kr)),
                SegmentOption(UpDownStyle.US, stringResource(R.string.settings_up_down_style_us)),
            ),
        value = state.upDownStyle,
        onChange = actions.onUpDownStyle,
        description = stringResource(R.string.settings_up_down_style_desc),
    )
}

/** 조회 중에는 아무것도 고르지 않은 채로 보여 준다. 알림 행은 B-a로 간다 */
@Composable
private fun DisparitySection(
    settings: DeviceSettings?,
    onChange: (SettingsPatch) -> Unit,
    onOpenAlerts: () -> Unit,
) {
    SegmentedControl(
        label = stringResource(R.string.settings_basis),
        options =
            listOf(
                SegmentOption(Basis.FRONT, stringResource(R.string.settings_basis_front)),
                SegmentOption(Basis.KR, stringResource(R.string.settings_basis_kr)),
                SegmentOption(Basis.US, stringResource(R.string.settings_basis_us)),
            ),
        value = settings?.basis,
        onChange = { onChange(SettingsPatch(basis = it)) },
        description = stringResource(R.string.settings_basis_desc),
    )
    SegmentedControl(
        label = stringResource(R.string.settings_ma_period),
        options = MaPeriod.entries.map { SegmentOption(it, it.days.toString()) },
        value = settings?.maPeriod,
        onChange = { onChange(SettingsPatch(maPeriod = it)) },
        description = stringResource(R.string.settings_ma_period_desc),
    )
    val windowAll = stringResource(R.string.settings_window_all)
    SegmentedControl(
        label = stringResource(R.string.settings_window),
        options =
            ChartWindow.entries.map {
                SegmentOption(it, if (it == ChartWindow.ALL) windowAll else it.name.drop(1))
            },
        value = settings?.window,
        onChange = { onChange(SettingsPatch(window = it)) },
        description = stringResource(R.string.settings_window_desc),
    )
    RareLevelsInput(
        levels = settings?.rareLevels,
        onSave = { onChange(SettingsPatch(rareLevels = it)) },
        description = stringResource(R.string.settings_rare_levels_desc),
    )
    LinkRow(
        label = stringResource(R.string.settings_alerts),
        value =
            settings?.let {
                it.alertCommodity?.let { code -> stringResource(code.nameRes) }
                    ?: stringResource(R.string.settings_alert_off)
            },
        description = stringResource(R.string.settings_alerts_desc),
        onClick = onOpenAlerts,
    )
}

/** 이름 + 값 + › 행. 설명은 이름·값 아래 한 줄이다. 누르면 선택 햅틱 */
@Composable
private fun LinkRow(
    label: String,
    description: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    val haptic = rememberTapHaptic()
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchTarget)
            .clickable(role = Role.Button) {
                haptic()
                onClick()
            },
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f), color = AppColors.text, fontSize = FontSize.body)
                    value?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(end = Spacing.sm),
                            color = AppColors.textSecondary,
                            fontSize = FontSize.body,
                        )
                    }
                }
                RowDescription(description, Modifier.padding(top = Spacing.xs))
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColors.textSecondary,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.separator))
    }
}

@Composable
private fun ResetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reset_title)) },
        text = { Text(stringResource(R.string.settings_reset_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_reset), color = AppColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
