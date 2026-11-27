package io.github.seyoon.futureetp.feature.commoditylist

import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.api.model.CommoditySummary
import io.github.seyoon.futureetp.core.api.model.ContractQuote
import io.github.seyoon.futureetp.core.api.model.Freshness
import io.github.seyoon.futureetp.core.api.model.MarketState
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

private fun quote(
    symbol: String,
    month: String,
    price: Double?,
    change: Double? = null,
    changePct: Double? = null,
    noTrade: Boolean = false,
    isFront: Boolean = false,
) = ContractQuote(symbol, YearMonth.parse(month), price, change, changePct, noTrade, isFront)

/** 금 2줄(대표월물 DEC26, 상승) + 원유 2줄(알림 대상, 한 줄 거래 없음) */
fun commodityList(asOf: String = "2026-10-22T00:00:00Z") =
    CommodityList(
        freshness =
            Freshness(
                asOf = Instant.parse(asOf),
                tradeDate = LocalDate.parse("2026-10-22"),
                delayed = true,
                marketState = MarketState.OPEN,
                nextUpdateAt = null,
            ),
        commodities =
            listOf(
                CommoditySummary(
                    code = CommodityCode.GC,
                    priceDecimals = 1,
                    alert = false,
                    contracts =
                        listOf(
                            quote("GCV26", "2026-10", 2405.1, 12.2, 0.51),
                            quote("GCZ26", "2026-12", 2418.2, 12.1, 0.5, isFront = true),
                        ),
                ),
                CommoditySummary(
                    code = CommodityCode.CL,
                    priceDecimals = 2,
                    alert = true,
                    contracts =
                        listOf(
                            quote("CLF27", "2027-01", 70.12, -0.5, -0.71, isFront = true),
                            quote("CLG27", "2027-02", 69.8, noTrade = true),
                        ),
                ),
            ),
    )
