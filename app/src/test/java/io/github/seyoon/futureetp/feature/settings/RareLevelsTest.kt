package io.github.seyoon.futureetp.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RareLevelsTest {
    private fun check(vararg texts: String) = checkRareLevels(texts.toList())

    @Test
    fun validLevelsAreRoundedAndSorted() {
        assertEquals(RareLevelsCheck.Valid(listOf(0.1, 1.0, 2.5, 16.0, 49.9)), check("16", "2,5", "0.1", "49.9", " 1 "))
    }

    @Test
    fun outOfRangeOrTooManyDecimalsIsInvalid() {
        val invalid = RareLevelsCheck.Invalid(RareLevelProblem.INVALID)
        assertEquals(invalid, check("0", "2", "4", "8", "16"))
        assertEquals(invalid, check("50", "2", "4", "8", "16"))
        assertEquals(invalid, check("1.25", "2", "4", "8", "16"))
        assertEquals(invalid, check("", "2", "4", "8", "16"))
        assertEquals(invalid, check("1", "2", "4", "8"))
    }

    @Test
    fun onlyPlainNumbersParse() {
        listOf("1e1", "0x8", "-1", "1.", ".5", "1 2").forEach { assertNull(it, parseRareLevel(it)) }
        assertEquals(2.5, parseRareLevel("2,5"))
    }

    @Test
    fun duplicatesAreCheckedAfterRounding() {
        assertEquals(RareLevelsCheck.Invalid(RareLevelProblem.DUPLICATE), check("1", "1.0", "4", "8", "16"))
        assertEquals(
            RareLevelsCheck.Invalid(RareLevelProblem.DUPLICATE),
            checkRareLevels(listOf("1", "1.00000000001", "4", "8", "16")),
        )
    }

    @Test
    fun levelTextMatchesRn() {
        assertEquals("1", rareLevelText(1.0))
        assertEquals("2.5", rareLevelText(2.5))
        assertEquals("0.1", rareLevelText(0.1))
    }
}
