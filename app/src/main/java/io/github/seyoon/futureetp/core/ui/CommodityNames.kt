package io.github.seyoon.futureetp.core.ui

import androidx.annotation.StringRes
import io.github.seyoon.futureetp.R
import io.github.seyoon.futureetp.core.api.model.CommodityCode

/** 원자재 이름 문구(RN `commodities.*`) */
@get:StringRes
val CommodityCode.nameRes: Int
    get() =
        when (this) {
            CommodityCode.GC -> R.string.commodity_gc
            CommodityCode.SI -> R.string.commodity_si
            CommodityCode.CL -> R.string.commodity_cl
            CommodityCode.NG -> R.string.commodity_ng
        }
