package io.github.seyoon.futureetp.feature.alertsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.model.QuietTime
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing
import io.github.seyoon.futureetp.core.ui.RowDescription
import io.github.seyoon.futureetp.core.ui.rememberTapHaptic

private const val SLOTS_PER_ROW = 4
private const val SHEET_MAX_HEIGHT = 0.6f
private const val BLOCKED_ALPHA = 0.3f

/** 시각 숫자 폭을 맞춘다(RN `tabular-nums`) */
private val TIME_STYLE = TextStyle(color = AppColors.text, fontSize = FontSize.body, fontFeatureSettings = "tnum")

/**
 * 알림 거부 시간(RN `QuietTimeSection`, D-007, D-026). 끄면 null로 저장한다. 켜면 시작·종료 버튼이 나오고,
 * 누르면 30분 단위 48칸 시트에서 고른다. 시작≠종료라 반대쪽 시각은 고를 수 없다
 */
@Composable
fun QuietTimeSection(
    value: QuietTime?,
    onChange: (QuietTime?) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<QuietEdge?>(null) }
    QuietSwitchRow(on = value != null) { on -> onChange(if (on) DEFAULT_QUIET_TIME else null) }
    if (value != null) QuietTimesRow(value) { editing = it }
    RowDescription(
        stringResource(R.string.alert_settings_quiet_note),
        Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
    )
    val edge = editing
    if (value != null && edge != null) {
        val title =
            if (edge ==
                QuietEdge.START
            ) {
                R.string.alert_settings_quiet_start
            } else {
                R.string.alert_settings_quiet_end
            }
        TimeSlotSheet(
            title = stringResource(title),
            selected = value.minuteOf(edge),
            blocked = value.blockedFor(edge),
            onPick = { minute ->
                editing = null
                if (minute != value.minuteOf(edge)) onChange(value.with(edge, minute))
            },
            onCancel = { editing = null },
        )
    }
}

@Composable
private fun QuietSwitchRow(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val label = stringResource(R.string.alert_settings_quiet_time)
    val haptic = rememberTapHaptic()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchTarget)
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = AppColors.text, fontSize = FontSize.body)
        Switch(
            checked = on,
            onCheckedChange = {
                haptic()
                onToggle(it)
            },
            modifier = Modifier.semantics { contentDescription = label },
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = AppColors.text,
                    checkedTrackColor = AppColors.surfaceSelected,
                    uncheckedThumbColor = AppColors.text,
                    uncheckedTrackColor = AppColors.surface,
                    uncheckedBorderColor = AppColors.border,
                ),
        )
    }
}

/** 시작 ~ 종료. 자정을 넘기면 종료에 `다음 날` */
@Composable
private fun QuietTimesRow(
    value: QuietTime,
    onEdit: (QuietEdge) -> Unit,
) {
    Row(
        Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeButton(stringResource(R.string.alert_settings_quiet_start), formatQuietMinute(value.startMin)) {
            onEdit(QuietEdge.START)
        }
        Text("~", color = AppColors.textSecondary, fontSize = FontSize.body)
        val end = formatQuietMinute(value.endMin)
        TimeButton(
            stringResource(R.string.alert_settings_quiet_end),
            if (value.endsNextDay) stringResource(R.string.alert_settings_quiet_next_day, end) else end,
        ) { onEdit(QuietEdge.END) }
    }
}

@Composable
private fun TimeButton(
    label: String,
    time: String,
    onClick: () -> Unit,
) {
    val haptic = rememberTapHaptic()
    Box(
        Modifier
            .heightIn(min = Size.touchTarget)
            .clip(RoundedCornerShape(Spacing.sm))
            .background(AppColors.surface)
            .clickable(role = Role.Button, onClickLabel = label) {
                haptic()
                onClick()
            }.semantics(mergeDescendants = true) { contentDescription = "$label $time" }
            .padding(horizontal = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(time, style = TIME_STYLE)
    }
}

/** 48칸 중 하나를 누르면 바로 고르고 닫는다. [blocked]는 반대쪽 시각이다 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotSheet(
    title: String,
    selected: Int,
    blocked: Int,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val haptic = rememberTapHaptic()
    ModalBottomSheet(
        onDismissRequest = onCancel,
        // 반만 펼친 상태 없이 바로 다 펼친다
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = AppColors.surface,
        scrimColor = AppColors.backdrop,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxHeight(SHEET_MAX_HEIGHT)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(title, color = AppColors.text, fontSize = FontSize.title)
            FlowRow(
                Modifier.verticalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                maxItemsInEachRow = SLOTS_PER_ROW,
            ) {
                QUIET_SLOTS.forEach { minute ->
                    val isSelected = minute == selected
                    val isBlocked = minute == blocked
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = Size.touchTarget)
                            .alpha(if (isBlocked) BLOCKED_ALPHA else 1f)
                            .clip(RoundedCornerShape(Spacing.sm))
                            .background(if (isSelected) AppColors.surfaceSelected else AppColors.background)
                            .selectable(selected = isSelected, enabled = !isBlocked, role = Role.RadioButton) {
                                haptic()
                                onPick(minute)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(formatQuietMinute(minute), style = TIME_STYLE)
                    }
                }
            }
        }
    }
}
