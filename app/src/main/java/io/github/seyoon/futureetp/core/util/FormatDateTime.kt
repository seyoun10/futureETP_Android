package io.github.seyoon.futureetp.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 시각·거래일 표시 포맷. 언어와 상관없이 숫자 형식(MM/DD HH:mm)을 쓴다(RN lib/formatDateTime.ts, screens.md 기준 시각 줄)

private val LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("MM/dd HH:mm")
private val TRADE_DATE = DateTimeFormatter.ofPattern("MM/dd")

// 시각 → 기기 현지(테스트는 zone 지정) `MM/DD HH:mm`
fun formatLocalDateTime(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
): String = LOCAL_DATE_TIME.format(instant.atZone(zone))

// CME 거래일 → `MM/DD`. 달력 날짜라 시간대 변환을 하지 않는다
fun formatTradeDate(tradeDate: LocalDate): String = TRADE_DATE.format(tradeDate)
