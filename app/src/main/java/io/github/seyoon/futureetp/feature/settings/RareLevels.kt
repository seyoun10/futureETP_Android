package io.github.seyoon.futureetp.feature.settings

import kotlin.math.abs
import kotlin.math.roundToLong

// rare-level 입력 규칙(shared `rareLevelSchema`·`rareLevelsSchema`, D-083)
const val RARE_LEVEL_COUNT = 5
const val RARE_LEVEL_MIN = 0.1
const val RARE_LEVEL_MAX = 49.9

/** 한 칸 최대 글자 수(`49.9`) */
const val RARE_LEVEL_MAX_LENGTH = 4

private const val DECIMAL_EPSILON = 1e-9
private const val TENTHS = 10.0
private val LEVEL_TEXT = Regex("""^\d+(\.\d+)?$""")

enum class RareLevelProblem { INVALID, DUPLICATE }

sealed interface RareLevelsCheck {
    /** 소수 첫째 자리로 반올림해 오름차순 정렬한 값 */
    data class Valid(
        val levels: List<Double>,
    ) : RareLevelsCheck

    data class Invalid(
        val problem: RareLevelProblem,
    ) : RareLevelsCheck
}

/** 숫자만 받는다(공백·지수·16진 거절). 기기에 따라 소수점이 쉼표로 들어온다. 아니면 null */
fun parseRareLevel(text: String): Double? =
    text
        .trim()
        .replace(',', '.')
        .takeIf(LEVEL_TEXT::matches)
        ?.toDouble()

/** 범위 안이고 소수 첫째 자리까지면 그 자리로 반올림한 값, 아니면 null */
private fun normalizeLevel(value: Double): Double? {
    val tenths = value * TENTHS
    val rounded = tenths.roundToLong()
    val ok = value in RARE_LEVEL_MIN..RARE_LEVEL_MAX && abs(tenths - rounded) < DECIMAL_EPSILON
    return if (ok) rounded / TENTHS else null
}

/** 5칸 입력 검사. 중복은 반올림한 값으로 본다(1과 1.00000000001은 중복) */
fun checkRareLevels(texts: List<String>): RareLevelsCheck {
    val levels = texts.map { text -> parseRareLevel(text)?.let(::normalizeLevel) }
    return when {
        levels.size != RARE_LEVEL_COUNT || levels.any { it == null } ->
            RareLevelsCheck.Invalid(
                RareLevelProblem.INVALID,
            )
        levels.toSet().size != levels.size -> RareLevelsCheck.Invalid(RareLevelProblem.DUPLICATE)
        else -> RareLevelsCheck.Valid(levels.filterNotNull().sorted())
    }
}

/** 입력칸에 보일 값(RN `String(number)`: `1`, `2.5`) */
fun rareLevelText(level: Double): String =
    if (level == level.roundToLong().toDouble()) level.roundToLong().toString() else level.toString()
