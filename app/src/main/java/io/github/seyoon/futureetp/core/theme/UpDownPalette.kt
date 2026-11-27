package io.github.seyoon.futureetp.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.seyoon.futureetp.core.settings.UpDownStyle
import io.github.seyoon.futureetp.core.util.CHANGE_PCT_DECIMALS
import java.math.BigDecimal
import java.math.RoundingMode

// 등락 색은 토큰이 아니라 표시 설정에서 고른다(D-031, RN `theme/upDownColor.ts`). 차트·목록이 같은 함수로 읽는다

data class UpDownPalette(
    val up: Color,
    val down: Color,
    val flat: Color,
)

private val KR_PALETTE = UpDownPalette(up = Color(0xFFEF5350), down = Color(0xFF42A5F5), flat = AppColors.neutral)
private val US_PALETTE = UpDownPalette(up = Color(0xFF26A69A), down = Color(0xFFEF5350), flat = AppColors.neutral)

/** 스타일마다 같은 객체를 돌려준다(remember 키가 십자선을 옮길 때마다 바뀌지 않게) */
fun upDownPalette(style: UpDownStyle): UpDownPalette =
    when (style) {
        UpDownStyle.KR -> KR_PALETTE
        UpDownStyle.US -> US_PALETTE
    }

/** 현재 표시 설정의 등락 색. `FutureEtpTheme`이 넣는다 */
val LocalUpDownPalette = staticCompositionLocalOf { US_PALETTE }

/**
 * 변화량 부호로 색을 고른다. 0·NaN은 방향 없는 색이다.
 * [decimals]를 주면 그 자릿수로 반올림한 값(화면에 쓴 값)으로 고른다: `0.0%`로 보이는 -0.04는 방향 없는 색
 */
fun changeColor(
    change: Double,
    palette: UpDownPalette,
    decimals: Int? = null,
): Color =
    when (shownSign(change, decimals)) {
        1 -> palette.up
        -1 -> palette.down
        else -> palette.flat
    }

/**
 * 변화량과 등락률(소수 둘째 자리)을 함께 보이는 줄의 색. 둘 다 반올림해 0이면 방향 없는 색이다
 * (NG처럼 변화량은 `0.000`인데 등락률은 `+0.01%`일 수 있어 둘 중 0이 아닌 쪽을 따른다)
 */
fun changePairColor(
    change: Double,
    changePct: Double,
    decimals: Int,
    palette: UpDownPalette,
): Color {
    val sign = shownSign(change, decimals).takeIf { it != 0 } ?: shownSign(changePct, CHANGE_PCT_DECIMALS)
    return changeColor(sign.toDouble(), palette)
}

// JS `Math.sign(Number(value.toFixed(decimals)))`. NaN은 0, 무한대는 부호 그대로다
private fun shownSign(
    value: Double,
    decimals: Int?,
): Int =
    when {
        value.isNaN() -> 0
        decimals == null || value.isInfinite() -> kotlin.math.sign(value).toInt()
        else -> BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).signum()
    }
