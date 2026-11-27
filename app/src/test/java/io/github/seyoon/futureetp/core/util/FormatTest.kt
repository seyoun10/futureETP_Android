package io.github.seyoon.futureetp.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

// RN lib/formatNumber·formatDateTime·formatAmount 테스트와 같은 기대값 + JS toFixed 반올림 경계
class FormatTest {
    @Test
    fun `formatPrice는 천 단위 쉼표와 소수 자릿수를 맞춘다`() {
        assertEquals("2,405.1", formatPrice(2405.1, 1))
        assertEquals("1,234,567.89", formatPrice(1234567.891, 2))
        assertEquals("2.766", formatPrice(2.7656, 3))
        assertEquals("80.00", formatPrice(80.0, 2))
        assertEquals("999", formatPrice(999.0, 0))
        assertEquals("1,000", formatPrice(1000.0, 0))
        assertEquals("100", formatPrice(100.0, 0))
    }

    @Test
    fun `formatPrice 음수는 앞에 -를 붙이고 반올림해 0이면 붙이지 않는다`() {
        assertEquals("-1,234.5", formatPrice(-1234.5, 1))
        assertEquals("0.00", formatPrice(-0.001, 2))
        assertEquals("0.0", formatPrice(-0.0, 1))
    }

    @Test
    fun `formatPrice 반올림은 JS toFixed와 같다`() {
        // node -e '(1.005).toFixed(2), (2.5).toFixed(0), (-2.5).toFixed(0), (0.125).toFixed(2)'
        assertEquals("1.00", formatPrice(1.005, 2))
        assertEquals("3", formatPrice(2.5, 0))
        assertEquals("-3", formatPrice(-2.5, 0))
        assertEquals("0.13", formatPrice(0.125, 2))
        assertEquals("1,000,000,000,000,001", formatPrice(1e15 + 0.5, 0))
    }

    @Test
    fun `formatChange는 상승 +, 하락 -를 붙인다`() {
        assertEquals("+12.2", formatChange(12.15, 1))
        assertEquals("-0.029", formatChange(-0.029, 3))
        assertEquals("-1,250.0", formatChange(-1250.0, 1))
    }

    @Test
    fun `formatChange는 0이거나 반올림해 0이면 부호가 없다`() {
        assertEquals("0.00", formatChange(0.0, 2))
        assertEquals("0.00", formatChange(0.004, 2))
        assertEquals("0.00", formatChange(-0.004, 2))
    }

    @Test
    fun `formatChangePct는 소수 둘째 자리 등락률`() {
        assertEquals("+0.51%", formatChangePct(0.5075))
        assertEquals("-1.04%", formatChangePct(-1.04))
    }

    @Test
    fun `formatContractMonth는 CME 표기 월물`() {
        assertEquals("DEC26", formatContractMonth(YearMonth.of(2026, 12)))
        assertEquals("JAN27", formatContractMonth(YearMonth.of(2027, 1)))
        assertEquals("MAR05", formatContractMonth(YearMonth.of(2105, 3)))
    }

    @Test
    fun `formatContractMonthShort는 연도 두 자리 + 월 두 자리`() {
        assertEquals("2612", formatContractMonthShort(YearMonth.of(2026, 12)))
        assertEquals("2702", formatContractMonthShort(YearMonth.of(2027, 2)))
    }

    @Test
    fun `formatLocalDateTime은 지정한 시간대의 MM_DD HH_mm`() {
        val seoul = ZoneId.of("Asia/Seoul")
        assertEquals("10/22 09:00", formatLocalDateTime(Instant.parse("2026-10-22T00:00:00Z"), seoul))
        assertEquals(
            "10/22 09:05",
            formatLocalDateTime(Instant.parse("2026-10-22T14:05:00Z"), ZoneId.of("America/Chicago")),
        )
        // 자정은 24가 아니라 00, 날짜가 바뀌면 현지 날짜
        assertEquals("10/23 00:00", formatLocalDateTime(Instant.parse("2026-10-22T15:00:00Z"), seoul))
    }

    @Test
    fun `formatTradeDate는 MM_DD`() {
        assertEquals("01/05", formatTradeDate(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun `compactAmount 억·만은 정수로 줄인다`() {
        assertEquals("15,550억", compactAmount(1_555_015_000_000.0, AmountScheme.EOK))
        assertEquals("12억", compactAmount(1_198_418_445.0, AmountScheme.EOK))
        assertEquals("4,500만", compactAmount(45_000_000.0, AmountScheme.EOK))
        assertEquals("9,999", compactAmount(9_999.0, AmountScheme.EOK))
    }

    @Test
    fun `compactAmount T·B·M·K는 소수 한 자리로 줄인다`() {
        assertEquals("12.3M", compactAmount(12_345_678.0, AmountScheme.SI))
        assertEquals("1.6T", compactAmount(1_555_015_000_000.0, AmountScheme.SI))
        assertEquals("850", compactAmount(850.0, AmountScheme.SI))
        assertEquals("-12.3M", compactAmount(-12_345_678.0, AmountScheme.SI))
    }

    @Test
    fun `compactAmount 반올림해 위 단위에 닿으면 위 단위로 쓴다`() {
        assertEquals("1억", compactAmount(99_999_999.0, AmountScheme.EOK))
        assertEquals("1.0M", compactAmount(999_960.0, AmountScheme.SI))
        assertEquals("999.9K", compactAmount(999_940.0, AmountScheme.SI))
    }
}
