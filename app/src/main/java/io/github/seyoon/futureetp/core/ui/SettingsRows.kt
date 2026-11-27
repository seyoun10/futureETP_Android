package io.github.seyoon.futureetp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.errorTextRes
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing

private val SEGMENT_MIN_HEIGHT = 36.dp

/** B와 하위 화면의 묶음 제목. 화면 폭 전체의 surface 띠에 가운데 정렬 굵은 글자(RN `SectionHeader`) */
@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .semantics { heading() },
        color = AppColors.text,
        fontSize = FontSize.body,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

data class SegmentOption<T>(
    val value: T,
    val label: String,
)

/**
 * 설정 행: 이름 + [a|b|c] 중 하나 고르기(RN `SegmentedControl`). [value]가 null이면(설정 조회 중) 아무것도 고르지 않는다.
 * 고른 값을 다시 누르면 아무것도 하지 않는다(햅틱도 없음, D-109).
 * [stacked]면 선택지가 많을 때처럼 이름을 위에 두고 선택지가 전체 폭을 나눠 쓴다
 */
@Composable
fun <T> SegmentedControl(
    label: String,
    options: List<SegmentOption<T>>,
    value: T?,
    onChange: (T) -> Unit,
    description: String? = null,
    stacked: Boolean = false,
) {
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchTarget)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
    Column(Modifier.padding(bottom = Spacing.xs)) {
        if (stacked) {
            Column(rowModifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(label, color = AppColors.text, fontSize = FontSize.body)
                Segments(label, options, value, onChange, stacked = true)
            }
        } else {
            Row(
                rowModifier,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f), color = AppColors.text, fontSize = FontSize.body)
                Segments(label, options, value, onChange, stacked = false)
            }
        }
        description?.let { RowDescription(it, Modifier.padding(horizontal = Spacing.lg)) }
    }
}

@Composable
private fun <T> Segments(
    label: String,
    options: List<SegmentOption<T>>,
    value: T?,
    onChange: (T) -> Unit,
    stacked: Boolean,
) {
    val haptic = rememberTapHaptic()
    Row(
        Modifier
            .then(if (stacked) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(Spacing.sm))
            .background(AppColors.surface)
            .selectableGroup()
            .semantics { contentDescription = label },
    ) {
        options.forEach { option ->
            val selected = option.value == value
            Box(
                Modifier
                    .then(if (stacked) Modifier.weight(1f) else Modifier.widthIn(min = Size.touchTarget))
                    .heightIn(min = SEGMENT_MIN_HEIGHT)
                    .background(if (selected) AppColors.surfaceSelected else AppColors.surface)
                    .selectable(selected = selected, role = Role.RadioButton) {
                        if (!selected) {
                            haptic()
                            onChange(option.value)
                        }
                    }.padding(horizontal = if (stacked) Spacing.xs else Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(option.label, color = AppColors.text, fontSize = FontSize.body, textAlign = TextAlign.Center)
            }
        }
    }
}

/** 설정 저장 실패 줄(B·B-a 맨 위, RN `settings.saveFailed`). 새 변경을 보내면 사라진다 */
@Composable
fun SaveFailedLine(error: Throwable) {
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

/** 설정 행 이름·값 아래 한 줄 설명 */
@Composable
fun RowDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, modifier = modifier, color = AppColors.textSecondary, fontSize = FontSize.caption)
}
