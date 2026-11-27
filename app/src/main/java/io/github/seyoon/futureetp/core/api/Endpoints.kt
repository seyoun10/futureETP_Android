package io.github.seyoon.futureetp.core.api

import io.github.seyoon.futureetp.core.api.model.CalendarQuery
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.api.model.ContractsChart
import io.github.seyoon.futureetp.core.api.model.ContractsQuery
import io.github.seyoon.futureetp.core.api.model.Curve
import io.github.seyoon.futureetp.core.api.model.CurveQuery
import io.github.seyoon.futureetp.core.api.model.Device
import io.github.seyoon.futureetp.core.api.model.DeviceRegistration
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.DisparityChart
import io.github.seyoon.futureetp.core.api.model.DisparityQuery
import io.github.seyoon.futureetp.core.api.model.EtpDetail
import io.github.seyoon.futureetp.core.api.model.EtpList
import io.github.seyoon.futureetp.core.api.model.MarketCalendar
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.ktor.http.HttpMethod

// 계약 `api.json`의 endpoints(health는 ApiClient.health). RN `apps/mobile/src/api/*.ts`의 get*/patch*/put* 함수에 대응한다

suspend fun ApiClient.commodities(): CommodityList = request("/commodities", CommodityList.serializer())

suspend fun ApiClient.disparityChart(
    code: CommodityCode,
    query: DisparityQuery,
): DisparityChart = request("/commodities/${code.name}/disparity", DisparityChart.serializer(), query = query.toQuery())

suspend fun ApiClient.contractsChart(
    code: CommodityCode,
    query: ContractsQuery = ContractsQuery(),
): ContractsChart = request("/commodities/${code.name}/contracts", ContractsChart.serializer(), query = query.toQuery())

suspend fun ApiClient.curve(
    code: CommodityCode,
    query: CurveQuery = CurveQuery(),
): Curve = request("/commodities/${code.name}/curve", Curve.serializer(), query = query.toQuery())

suspend fun ApiClient.etps(code: CommodityCode): EtpList =
    request("/commodities/${code.name}/etps", EtpList.serializer())

suspend fun ApiClient.etpDetail(id: String): EtpDetail = request("/etps/$id", EtpDetail.serializer())

suspend fun ApiClient.calendar(query: CalendarQuery): MarketCalendar =
    request("/calendar", MarketCalendar.serializer(), query = query.toQuery())

/** @param token 저장 전 키 확인(B-b)에서만 넘긴다(D-044) */
suspend fun ApiClient.settings(token: String? = null): DeviceSettings =
    request("/settings", DeviceSettings.serializer(), token = token)

suspend fun ApiClient.patchSettings(patch: SettingsPatch): DeviceSettings =
    request("/settings", DeviceSettings.serializer(), HttpMethod.Patch, body = patch.toJson())

suspend fun ApiClient.registerDevice(registration: DeviceRegistration): Device =
    request("/devices", Device.serializer(), HttpMethod.Put, body = registration.toJson())
