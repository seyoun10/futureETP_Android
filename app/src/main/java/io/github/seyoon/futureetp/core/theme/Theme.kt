package io.github.seyoon.futureetp.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.seyoon.futureetp.core.settings.UpDownStyle

// 다크 전용(D-031). Material 컴포넌트가 읽는 색을 토큰으로 맞춘다
private val colors =
    darkColorScheme(
        background = AppColors.background,
        onBackground = AppColors.text,
        surface = AppColors.surface,
        onSurface = AppColors.text,
        surfaceVariant = AppColors.surface,
        onSurfaceVariant = AppColors.textSecondary,
        outline = AppColors.border,
        outlineVariant = AppColors.separator,
        error = AppColors.error,
        scrim = AppColors.backdrop,
    )

/** 앱 전체 테마. 등락 색은 표시 설정(D-073)을 [LocalUpDownPalette]로 넣는다 */
@Composable
fun FutureEtpTheme(
    upDownStyle: UpDownStyle = UpDownStyle.US,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalUpDownPalette provides upDownPalette(upDownStyle)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
