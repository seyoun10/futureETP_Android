package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// 계약 enum(`schemas/*.json`). 이름이 서버 값과 같아 @SerialName 없이 그대로 쓴다. 모르는 값은 SCHEMA_MISMATCH

/** 원자재 코드(경로 `{code}`, D-002) */
@Serializable
enum class CommodityCode { GC, SI, CL, NG }

/** 장 상태(D-022) */
@Serializable
enum class MarketState { OPEN, CLOSED, SETTLEMENT_PENDING }

/** 이격도 기준 월물(대표월물·국내 ETP·미국 ETP 보유 월물) */
@Serializable
enum class Basis { FRONT, KR, US }

/** 분포 기간 */
@Serializable
enum class ChartWindow { Y3, Y5, Y10, ALL }

/** rare 레벨·꼬리 방향 */
@Serializable
enum class Direction { UP, DOWN }

/** 월물커브 상태 */
@Serializable
enum class CurveState { CONTANGO, BACKWARDATION, FLAT }

@Serializable
enum class EtpMarket { KRX, US }

@Serializable
enum class EtpType { ETF, ETN }

@Serializable
enum class FxHedge { HEDGED, UNHEDGED, NA }

/** 휴장 달력 시장(D-143) */
@Serializable
enum class CalendarMarket { CME, KRX, NYSE }

/** 휴장 달력 예외 종류 */
@Serializable
enum class CalendarKind { CLOSED, HALT, EARLY_CLOSE, LATE_OPEN }

/** 서버 기기 설정 언어(D-137) */
@Serializable
enum class AppLanguage {
    @SerialName("ko")
    KO,

    @SerialName("en")
    EN,
}

@Serializable
enum class Platform { IOS, ANDROID }

/** 이동평균 기간. 응답은 숫자(`maPeriod`), 이격도 query는 문자열(`ma`) */
@Serializable(with = MaPeriodSerializer::class)
enum class MaPeriod(
    val days: Int,
) {
    MA20(20),
    MA50(50),
    MA100(100),
    MA200(200),
    ;

    companion object {
        fun of(days: Int): MaPeriod? = entries.firstOrNull { it.days == days }
    }
}

object MaPeriodSerializer : KSerializer<MaPeriod> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MaPeriod", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): MaPeriod {
        val days = decoder.decodeInt()
        return MaPeriod.of(days) ?: throw SerializationException("Unknown maPeriod: $days")
    }

    override fun serialize(
        encoder: Encoder,
        value: MaPeriod,
    ) = encoder.encodeInt(value.days)
}
