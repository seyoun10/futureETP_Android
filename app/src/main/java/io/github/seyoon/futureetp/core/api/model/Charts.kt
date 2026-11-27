@file:UseSerializers(LocalDateSerializer::class, InstantSerializer::class, YearMonthSerializer::class)

package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// 시세·차트 응답(`schemas/commodities.json`·`disparityChart.json`·`contractsChart.json`·`curve.json`).
// 필드 이름·nullable은 계약 그대로다. 필드 뜻은 원본 zod(`../futureETP_ReactNative/packages/shared/src/*.ts`) 주석에 있다

/** 기준 시각 줄(D-048). nextUpdateAt은 다음 갱신 예정 시각, 없으면 null */
@Serializable
data class Freshness(
    val asOf: Instant,
    val tradeDate: LocalDate,
    val delayed: Boolean,
    val marketState: MarketState,
    val nextUpdateAt: Instant?,
)

/** `GET /v1/commodities` (A 화면) */
@Serializable
data class CommodityList(
    val freshness: Freshness,
    val commodities: List<CommoditySummary>,
)

@Serializable
data class CommoditySummary(
    val code: CommodityCode,
    val priceDecimals: Int,
    val alert: Boolean,
    val contracts: List<ContractQuote>,
)

@Serializable
data class ContractQuote(
    val symbol: String,
    val contractMonth: YearMonth,
    val price: Double?,
    val change: Double?,
    val changePct: Double?,
    val noTrade: Boolean,
    val isFront: Boolean,
)

/** `GET /v1/commodities/{code}/disparity` (C-a) */
@Serializable
data class DisparityChart(
    val freshness: Freshness,
    val priceDecimals: Int,
    val basis: Basis,
    val fallback: Boolean,
    val maPeriod: MaPeriod,
    val window: ChartWindow,
    val distribution: Distribution?,
    val rareLevels: List<RareLevel>,
    val currentTail: CurrentTail?,
    val provisional: Boolean,
    val bars: List<DisparityBar>,
    val hasMore: Boolean,
)

@Serializable
data class Distribution(
    val actualStartDate: LocalDate,
    val sampleCount: Int,
)

@Serializable
data class RareLevel(
    val direction: Direction,
    val level: Double,
    val disparity: Double,
    val price: Double?,
)

@Serializable
data class CurrentTail(
    val side: Direction,
    val pct: Double,
)

@Serializable
data class DisparityBar(
    val tradeDate: LocalDate,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val close: Double?,
    val ma: Double?,
    val disparity: Double?,
    val inRoll: Boolean,
    val holdings: List<Holding>,
)

@Serializable
data class Holding(
    val contractMonth: YearMonth,
    val weight: Double,
)

/** `GET /v1/commodities/{code}/contracts` (C-c). 각 월물의 prices는 dates와 같은 순번 */
@Serializable
data class ContractsChart(
    val freshness: Freshness,
    val priceDecimals: Int,
    val dates: List<LocalDate>,
    val hasMore: Boolean,
    val provisional: Boolean,
    val contracts: List<ContractSeries>,
)

@Serializable
data class ContractSeries(
    val symbol: String,
    val contractMonth: YearMonth,
    val lastTradeDate: LocalDate,
    val isFront: Boolean,
    val prices: List<Double?>,
)

/** `GET /v1/commodities/{code}/curve` (C-b) */
@Serializable
data class Curve(
    val freshness: Freshness,
    val priceDecimals: Int,
    val baseDate: LocalDate,
    val baseOffset: Int,
    val prevBaseDate: LocalDate?,
    val nextBaseDate: LocalDate?,
    val minBaseDate: LocalDate,
    val maxBaseDate: LocalDate,
    val compareDate: LocalDate,
    val points: List<CurvePoint>,
    val current: CurveSummary?,
    val base: CurveSummary?,
)

@Serializable
data class CurvePoint(
    val symbol: String,
    val contractMonth: YearMonth,
    val basePrice: Double?,
    val currentPrice: Double?,
    val changePct: Double?,
)

/** M1·M2 스프레드와 롤 수익률 */
@Serializable
data class CurveSummary(
    val m1ContractMonth: YearMonth,
    val m2ContractMonth: YearMonth,
    val state: CurveState,
    val spreadPct: Double,
    val rollYieldPct: Double,
)
