package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 계약 문자열 값 ↔ java.time. 형식이 다르면 SerializationException이라 ApiClient가 SCHEMA_MISMATCH로 바꾼다(D-042).
 * 값 형식은 `../futureETP_ReactNative/docs/architecture.md` "값 형식": 날짜 `YYYY-MM-DD`, 시각 UTC ISO 8601(`Z`), 월물 `YYYY-MM`
 */
abstract class StringValueSerializer<T : Any>(
    name: String,
    private val parse: (String) -> T,
    private val format: (T) -> String = { it.toString() },
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val text = decoder.decodeString()
        return try {
            parse(text)
        } catch (e: DateTimeParseException) {
            throw SerializationException("Invalid ${descriptor.serialName}: $text", e)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = encoder.encodeString(format(value))
}

/** `YYYY-MM-DD`(거래일·기준일 등) */
object LocalDateSerializer : StringValueSerializer<LocalDate>("LocalDate", LocalDate::parse)

/** UTC ISO 8601 `…Z`(asOf·nextUpdateAt 등) */
object InstantSerializer : StringValueSerializer<Instant>("Instant", Instant::parse)

/** 월물 `YYYY-MM`(contractMonth) */
object YearMonthSerializer : StringValueSerializer<YearMonth>("YearMonth", YearMonth::parse)

private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

/** 시장 현지 시각 `HH:mm`(휴장 달력 at) */
object HourMinuteSerializer : StringValueSerializer<LocalTime>(
    "HourMinute",
    { LocalTime.parse(it, hourMinute) },
    { it.format(hourMinute) },
)
