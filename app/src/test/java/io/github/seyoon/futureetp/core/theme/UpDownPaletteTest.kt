package io.github.seyoon.futureetp.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.seyoon.futureetp.core.settings.UpDownStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

// RN `theme/upDownColor.test.ts`와 같은 기대값
class UpDownPaletteTest {
    @Test
    fun krIsRedUpBlueDownAndUsIsGreenUpRedDown() {
        val kr = upDownPalette(UpDownStyle.KR)
        val us = upDownPalette(UpDownStyle.US)
        assertEquals(0xFFEF5350.toInt(), kr.up.toArgbInt())
        assertEquals(0xFF42A5F5.toInt(), kr.down.toArgbInt())
        assertEquals(0xFF26A69A.toInt(), us.up.toArgbInt())
        assertEquals(kr.up, us.down)
        assertEquals(AppColors.neutral, kr.flat)
        assertEquals(AppColors.neutral, us.flat)
    }

    @Test
    fun sameStyleReturnsSameInstance() {
        assertSame(upDownPalette(UpDownStyle.KR), upDownPalette(UpDownStyle.KR))
        assertSame(upDownPalette(UpDownStyle.US), upDownPalette(UpDownStyle.US))
    }

    @Test
    fun changeColorFollowsSign() {
        val p = upDownPalette(UpDownStyle.US)
        assertEquals(p.up, changeColor(0.5, p))
        assertEquals(p.down, changeColor(-0.01, p))
    }

    @Test
    fun zeroAndNaNAreFlat() {
        val p = upDownPalette(UpDownStyle.US)
        assertEquals(p.flat, changeColor(0.0, p))
        assertEquals(p.flat, changeColor(-0.0, p))
        assertEquals(p.flat, changeColor(Double.NaN, p))
        assertEquals(p.flat, changeColor(Double.NaN, p, 1))
    }

    @Test
    fun decimalsUseShownValue() {
        val p = upDownPalette(UpDownStyle.US)
        assertEquals(p.flat, changeColor(-0.04, p, 1))
        assertEquals(p.flat, changeColor(0.04, p, 1))
        assertEquals(p.down, changeColor(-0.05, p, 1))
        assertEquals(p.up, changeColor(0.06, p, 1))
    }

    @Test
    fun pairIsFlatOnlyWhenBothRoundToZero() {
        val p = upDownPalette(UpDownStyle.KR)
        assertEquals(p.flat, changePairColor(0.0004, 0.004, 3, p))
        assertEquals(p.flat, changePairColor(-0.04, -0.001, 1, p))
    }

    @Test
    fun pairFollowsTheNonZeroSide() {
        val p = upDownPalette(UpDownStyle.KR)
        assertEquals(p.up, changePairColor(0.0004, 0.014, 3, p))
        assertEquals(p.down, changePairColor(-0.1, -0.002, 1, p))
        assertEquals(p.up, changePairColor(Double.NaN, 0.5, 1, p))
    }

    private fun Color.toArgbInt(): Int = toArgb()
}
