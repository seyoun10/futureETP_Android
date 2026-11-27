@file:UseSerializers(LocalDateSerializer::class, InstantSerializer::class)

package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate

// ETP 응답(`schemas/etpList.json`·`etpDetail.json`, D-017)

/** `GET /v1/commodities/{code}/etps` (C-d). 필터·정렬은 앱에서 한다 */
@Serializable
data class EtpList(
    val asOf: Instant?,
    val etps: List<EtpSummary>,
)

/** aum·avgTradingValue는 원화(KRX) 또는 달러(US) 원값 */
@Serializable
data class EtpSummary(
    val id: String,
    val market: EtpMarket,
    val type: EtpType,
    val leverage: Double,
    val fxHedge: FxHedge,
    val name: String,
    val tickers: List<String>,
    val aum: Double?,
    val avgTradingValue: Double?,
)

/** `GET /v1/etps/{id}` (C-d-a). 목록 항목 필드 + 상세 필드 */
@Serializable
data class EtpDetail(
    val id: String,
    val market: EtpMarket,
    val type: EtpType,
    val leverage: Double,
    val fxHedge: FxHedge,
    val name: String,
    val tickers: List<String>,
    val aum: Double?,
    val avgTradingValue: Double?,
    val commodityCode: CommodityCode,
    val nameLang: AppLanguage,
    val issuer: String?,
    val underlyingIndex: String?,
    val expenseRatio: Double?,
    val issuerUrl: String?,
    val listedDate: LocalDate?,
    val series: List<EtpSeries>,
    val collectedAt: Instant,
)

@Serializable
data class EtpSeries(
    val ticker: String,
    val seriesCode: String?,
    val maturityDate: LocalDate?,
)
