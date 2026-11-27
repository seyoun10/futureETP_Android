package io.github.seyoon.futureetp.core.api.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

// 기기 설정·기기 등록(`schemas/settings.json`·`settingsPatch.json`·`device.json`·`deviceRegistration.json`, D-120)

/** 요청 본문 인코딩. 필수 nullable 필드(`pushToken: null` 등)를 빼지 않도록 null을 그대로 쓴다 */
val requestJson = Json { explicitNulls = true }

/** `GET`·`PATCH /v1/settings` 응답. 서버에 둔 기기별 설정(D-137) */
@Serializable
data class DeviceSettings(
    val locale: AppLanguage,
    /** IANA 시간대(D-084) */
    val timezone: String,
    val basis: Basis,
    val maPeriod: MaPeriod,
    val window: ChartWindow,
    /** 5개, 오름차순(D-083) */
    val rareLevels: List<Double>,
    val alertCommodity: CommodityCode?,
    val quietTime: QuietTime?,
)

/** 방해 금지 시간. 자정부터 분, 30분 단위(0~1410) */
@Serializable
data class QuietTime(
    val startMin: Int,
    val endMin: Int,
)

/** 값을 바꾸는 필드를 감싼다. 필드가 null이면 보내지 않고, `Change(null)`이면 null로 바꾼다 */
data class Change<out T>(
    val value: T,
)

/**
 * `PATCH /v1/settings` 본문. 보낼 필드만 채운다(계약 `additionalProperties: false`, 빈 본문은 서버가 400).
 * rareLevels 범위(0.1~49.9)·중복 검사는 B 입력 검증이 먼저 한다(D-083)
 */
data class SettingsPatch(
    val locale: AppLanguage? = null,
    val timezone: String? = null,
    val basis: Basis? = null,
    val maPeriod: MaPeriod? = null,
    val window: ChartWindow? = null,
    val rareLevels: List<Double>? = null,
    val alertCommodity: Change<CommodityCode?>? = null,
    val quietTime: Change<QuietTime?>? = null,
) {
    fun toJson(): JsonObject =
        buildJsonObject {
            locale?.let { put("locale", requestJson.encodeToJsonElement(it)) }
            timezone?.let { put("timezone", JsonPrimitive(it)) }
            basis?.let { put("basis", requestJson.encodeToJsonElement(it)) }
            maPeriod?.let { put("maPeriod", requestJson.encodeToJsonElement(it)) }
            window?.let { put("window", requestJson.encodeToJsonElement(it)) }
            rareLevels?.let { levels -> put("rareLevels", JsonArray(levels.map(::JsonPrimitive))) }
            alertCommodity?.let { put("alertCommodity", it.value?.let(requestJson::encodeToJsonElement) ?: JsonNull) }
            quietTime?.let { put("quietTime", it.value?.let(requestJson::encodeToJsonElement) ?: JsonNull) }
        }
}

/** `PUT /v1/devices` 본문. 세 필드 모두 필수이고 pushToken·appVersion은 null일 수 있다(D-037) */
@Serializable
data class DeviceRegistration(
    val pushToken: String?,
    val platform: Platform,
    /** `aos-<버전>`(AOS-002) */
    val appVersion: String?,
) {
    fun toJson(): JsonElement = requestJson.encodeToJsonElement(this)
}

/** `PUT /v1/devices` 응답 */
@Serializable
data class Device(
    val id: String,
)
