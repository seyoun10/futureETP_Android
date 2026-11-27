package io.github.seyoon.futureetp.core.util

import java.math.BigDecimal
import java.math.RoundingMode

// 순자산·거래대금 같은 큰 금액을 단위로 줄인다(RN lib/formatAmount.ts, screens.md C-d 예: `1,234억 원`, `$12.3M`).
// 통화 표기는 문구 리소스가 붙인다. 단위는 월물 표기처럼 숫자 표기로 보고 여기 둔다
enum class AmountScheme(
    val decimals: Int,
    val units: List<Pair<String, Double>>,
) {
    // 억·만은 정수(1,234억), 그 아래는 원 단위
    EOK(0, listOf("억" to 1e8, "만" to 1e4)),

    // T·B·M·K는 소수 한 자리(12.3M)
    SI(1, listOf("T" to 1e12, "B" to 1e9, "M" to 1e6, "K" to 1e3)),
}

fun compactAmount(
    value: Double,
    scheme: AmountScheme,
): String {
    val units = scheme.units
    val index = units.indexOfFirst { (_, size) -> kotlin.math.abs(value) >= size }
    if (index == -1) return formatPrice(value, 0)
    val size = units[index].second
    val upper = units.getOrNull(index - 1)
    // 반올림해서 위 단위에 닿으면(9,999.6만 → 10,000만) 위 단위로 쓴다(1억)
    val rounded = BigDecimal(value / size).setScale(scheme.decimals, RoundingMode.HALF_UP).abs().toDouble()
    val (unit, divisor) = if (upper != null && rounded >= upper.second / size) upper else units[index]
    return formatPrice(value / divisor, scheme.decimals) + unit
}
