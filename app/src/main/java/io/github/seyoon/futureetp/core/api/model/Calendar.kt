@file:UseSerializers(LocalDateSerializer::class, HourMinuteSerializer::class)

package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate
import java.time.LocalTime

/** `GET /v1/calendar?from&to` (A-a 휴장 달력, D-143, `schemas/calendar.json`) */
@Serializable
data class MarketCalendar(
    val from: LocalDate,
    val to: LocalDate,
    val exceptions: List<CalendarException>,
    /** 범위에 걸친 연도 중 휴장 일정이 입력되지 않은 (시장, 연도) */
    val missing: List<MissingYear>,
)

@Serializable
data class CalendarException(
    val market: CalendarMarket,
    /** 그 시장의 날짜. 기기 로컬 칸으로 옮기는 것은 화면 쪽(D-144) */
    val date: LocalDate,
    val kind: CalendarKind,
    /** 그 시장 현지 시각(CME CT, KRX KST, NYSE ET). CLOSED는 null */
    val at: LocalTime?,
    /** 휴일 이름 입력 원문(CME·NYSE 영어, KRX 한국어) */
    val note: String,
)

@Serializable
data class MissingYear(
    val market: CalendarMarket,
    val year: Int,
)
