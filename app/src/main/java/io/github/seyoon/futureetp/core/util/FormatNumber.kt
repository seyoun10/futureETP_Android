package io.github.seyoon.futureetp.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import kotlin.math.roundToLong

// 시세 숫자 표시. 언어와 상관없이 `2,405.1`·`+12.2`·`-1.04%` 형식이다(RN lib/formatNumber.ts, screens.md A).
// 반올림은 JS `toFixed`처럼 double의 정확한 값에서 절반 올림이다(1.005 → 1.00).

// 천 단위 쉼표 + 고정 소수 자릿수. 반올림해 0이면 부호를 붙이지 않는다
fun formatPrice(
    value: Double,
    decimals: Int,
): String {
    val fixed = toFixedAbs(value, decimals)
    val int = fixed.substringBefore('.')
    val frac = fixed.substringAfter('.', missingDelimiterValue = "")
    val sign = if (value < 0 && fixed.any { it in '1'..'9' }) "-" else ""
    return sign + groupThousands(int) + if (frac.isEmpty()) "" else ".$frac"
}

// 부호 붙인 변화량. 반올림해서 0이면 부호를 붙이지 않는다
fun formatChange(
    value: Double,
    decimals: Int,
): String {
    val abs = formatPrice(kotlin.math.abs(value), decimals)
    if (abs.none { it in '1'..'9' }) return abs
    return (if (value < 0) "-" else "+") + abs
}

// 등락률(%) 소수 둘째 자리
const val CHANGE_PCT_DECIMALS = 2

fun formatChangePct(pct: Double): String = "${formatChange(pct, CHANGE_PCT_DECIMALS)}%"

// 월물 → `DEC26`(CME 월물 표기라 언어와 상관없다)
private val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

fun formatContractMonth(contractMonth: YearMonth): String =
    MONTHS[contractMonth.monthValue - 1] + twoDigits(contractMonth.year % CENTURY)

// 월물 → `2612`(C-b x축은 월물이 많아 숫자로 줄인다)
fun formatContractMonthShort(contractMonth: YearMonth): String =
    twoDigits(contractMonth.year % CENTURY) + twoDigits(contractMonth.monthValue)

private const val CENTURY = 100
private const val GROUP = 3

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

// |value|를 소수 decimals 자리로 반올림한 문자열(JS `Math.abs(value).toFixed(decimals)`)
internal fun toFixedAbs(
    value: Double,
    decimals: Int,
): String = BigDecimal(kotlin.math.abs(value)).setScale(decimals, RoundingMode.HALF_UP).toPlainString()

private fun groupThousands(int: String): String =
    int
        .reversed()
        .chunked(GROUP)
        .joinToString(",")
        .reversed()

/** rare-level 값(RN `String(number)`: `1`, `2.5`). B 입력칸·B-a 적용 기준에 쓴다 */
fun formatRareLevel(level: Double): String =
    if (level == level.roundToLong().toDouble()) level.roundToLong().toString() else level.toString()
