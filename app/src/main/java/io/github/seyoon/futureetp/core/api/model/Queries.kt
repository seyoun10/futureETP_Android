package io.github.seyoon.futureetp.core.api.model

import java.math.BigDecimal
import java.time.LocalDate

// query 문자열(`schemas/disparityQuery.json`·`contractsQuery.json`·`curveQuery.json`·`calendarQuery.json`).
// ApiClient.request의 query로 넘긴다. 값이 null인 항목은 빠진다

/** `GET /v1/commodities/{code}/disparity`. before가 있으면 그 거래일보다 이른 3년(D-104) */
data class DisparityQuery(
    val basis: Basis,
    val maPeriod: MaPeriod,
    val window: ChartWindow,
    val levels: List<Double>,
    val before: LocalDate? = null,
) {
    fun toQuery(): Map<String, String?> =
        mapOf(
            "basis" to basis.name,
            "ma" to maPeriod.days.toString(),
            "window" to window.name,
            "levels" to levels.joinToString(",", transform = ::formatLevel),
            "before" to before?.toString(),
        )
}

/** `GET /v1/commodities/{code}/contracts` */
data class ContractsQuery(
    val before: LocalDate? = null,
) {
    fun toQuery(): Map<String, String?> = mapOf("before" to before?.toString())
}

/** `GET /v1/commodities/{code}/curve`. baseDate가 없으면 서버 기본 기준일(D-090) */
data class CurveQuery(
    val baseDate: LocalDate? = null,
) {
    fun toQuery(): Map<String, String?> = mapOf("baseDate" to baseDate?.toString())
}

/** `GET /v1/calendar`. to는 from 이후, from부터 366일 이내 */
data class CalendarQuery(
    val from: LocalDate,
    val to: LocalDate,
) {
    fun toQuery(): Map<String, String?> = mapOf("from" to from.toString(), "to" to to.toString())
}

/**
 * rare 레벨 하나를 숫자 기본 문자열로(`1`, `2.5`). 지수·끝 0 없이 써서 계약 pattern `^\d+(\.\d+)?$`를 지킨다.
 * Double.toString은 `1.0`을 만들어 RN(`String(1)` → `1`)과 캐시 키·요청이 달라진다
 */
fun formatLevel(level: Double): String = BigDecimal.valueOf(level).stripTrailingZeros().toPlainString()
