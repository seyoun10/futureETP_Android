package io.github.seyoon.futureetp.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 다크 전용(D-031). 토큰은 3단계 테마 항목에서 RN theme와 맞춘다
private val colors =
    darkColorScheme(
        background = Color.Black,
        surface = Color(0xFF1E1E1E),
    )

@Composable
fun FutureEtpTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
