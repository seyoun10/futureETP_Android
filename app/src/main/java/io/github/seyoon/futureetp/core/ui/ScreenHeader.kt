package io.github.seyoon.futureetp.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FontSize
import io.github.seyoon.futureetp.core.theme.Size
import io.github.seyoon.futureetp.core.theme.Spacing

/**
 * 모든 화면의 상단 줄: [←] 제목 (오른쪽 버튼)(RN `ScreenHeader`). 제목은 왼쪽 정렬이고 아래에 1px `barSeparator` 선.
 * [onBack]이 null이면 [←]가 없다(첫 화면)
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    right: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.background(AppColors.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                HeaderIconButton(R.drawable.ic_arrow_back, stringResource(R.string.common_back), onBack)
            } else {
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                title,
                modifier = Modifier.weight(1f).semantics { heading() },
                color = AppColors.text,
                fontSize = FontSize.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            right()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.barSeparator))
    }
}

/** title-bar 아이콘 버튼(RN `HeaderIconButton`). 누르면 선택 햅틱 */
@Composable
fun HeaderIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    val haptic = rememberTapHaptic()
    Box(
        Modifier
            .sizeIn(minWidth = Size.touchTarget, minHeight = Size.touchTarget)
            .clickable(role = Role.Button) {
                haptic()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = AppColors.text)
    }
}
