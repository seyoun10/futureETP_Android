package io.github.seyoon.futureetp.core.api.model

import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import io.github.seyoon.futureetp.core.api.Health
import io.github.seyoon.futureetp.core.api.apiJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * 계약 테스트(D-142, AOS-012).
 * - `contract/schemas`: 원본 `../futureETP_ReactNative/packages/shared/contract` 복사본(`scripts/sync-contract.sh`).
 *   원본이 옆에 있으면 같은지 본다
 * - `contract/fixtures/<스키마>[.<변형>].json`: 로컬 dev 서버 실제 응답의 data(`scripts/capture-fixtures.sh`, 차트는 마지막 60거래일)
 * 각 예시가 JSON Schema를 통과하고, 모델로 디코딩되고, 모델을 다시 인코딩하면 원래 JSON과 같은지(빠진 필드가 없는지) 본다.
 * 요청 본문·query는 모델이 만든 값이 요청 스키마를 통과하는지 본다
 */
class ContractTest {
    // Gradle 단위 테스트 작업 디렉터리는 app/
    private val contractDir = File("src/test/resources/contract")
    private val schemaDir = File(contractDir, "schemas")
    private val fixtureDir = File(contractDir, "fixtures")
    private val upstreamDir = File("../../futureETP_ReactNative/packages/shared/contract")

    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
    private val schemas = mutableMapOf<String, Schema>()

    private fun schema(name: String): Schema =
        schemas.getOrPut(name) { registry.getSchema(File(schemaDir, "$name.json").readText(), InputFormat.JSON) }

    private fun assertValid(
        schemaName: String,
        json: String,
        label: String = schemaName,
    ) {
        val errors = schema(schemaName).validate(json, InputFormat.JSON)
        assertTrue("$label: ${errors.joinToString("\n") { "${it.instanceLocation} ${it.message}" }}", errors.isEmpty())
    }

    private fun assertInvalid(
        schemaName: String,
        json: String,
    ) {
        assertTrue("$schemaName should reject $json", schema(schemaName).validate(json, InputFormat.JSON).isNotEmpty())
    }

    /** 응답 스키마 이름 → 모델 serializer */
    private val responses: Map<String, KSerializer<*>> =
        mapOf(
            "health" to Health.serializer(),
            "commodities" to CommodityList.serializer(),
            "disparityChart" to DisparityChart.serializer(),
            "contractsChart" to ContractsChart.serializer(),
            "curve" to Curve.serializer(),
            "etpList" to EtpList.serializer(),
            "etpDetail" to EtpDetail.serializer(),
            "calendar" to MarketCalendar.serializer(),
            "settings" to DeviceSettings.serializer(),
            "device" to Device.serializer(),
        )

    private val fixtures: List<File>
        get() = fixtureDir.listFiles { file -> file.extension == "json" }.orEmpty().sortedBy { it.name }

    @Test
    fun schemaCopyMatchesUpstream() {
        if (!upstreamDir.isDirectory) return // 원본 저장소가 없는 환경(CI 등)에서는 복사본만 본다
        val upstream = listOf(File(upstreamDir, "api.json")) + File(upstreamDir, "schemas").listFiles().orEmpty()
        val copied =
            schemaDir
                .listFiles()
                .orEmpty()
                .map { it.name }
                .toSet()
        assertEquals("run scripts/sync-contract.sh", upstream.map { it.name }.toSet(), copied)
        upstream.forEach { file ->
            val copy = File(schemaDir, file.name).readText()
            assertEquals("run scripts/sync-contract.sh: ${file.name}", file.readText(), copy)
        }
    }

    @Test
    fun everyResponseSchemaHasModelAndFixture() {
        val endpoints = apiJson.parseToJsonElement(File(schemaDir, "api.json").readText()) as JsonObject
        val dataSchemas =
            (endpoints["endpoints"] as JsonArray)
                .mapNotNull { ((it as JsonObject)["data"] as? JsonPrimitive)?.content }
                .map { it.removePrefix("schemas/").removeSuffix(".json") }
                .toSet()
        assertEquals(dataSchemas, responses.keys)
        val captured = fixtures.map { it.name.substringBefore('.') }.toSet()
        assertEquals("missing fixtures", emptySet<String>(), dataSchemas - captured)
    }

    @Test
    fun fixturesMatchSchemaAndModel() {
        assertTrue(fixtures.isNotEmpty())
        fixtures.forEach { file ->
            val name = file.name.substringBefore('.')
            val serializer = requireNotNull(responses[name]) { "${file.name}: unknown schema $name" }
            val text = file.readText()
            assertValid(name, text, file.name)

            val original = apiJson.parseToJsonElement(text)
            val model = apiJson.decodeFromJsonElement(serializer, original)

            @Suppress("UNCHECKED_CAST")
            val encoded = roundTripJson.encodeToJsonElement(serializer as KSerializer<Any?>, model)
            assertSameJson(file.name, original, encoded)
        }
    }

    @Test
    fun settingsPatchMatchesSchema() {
        val patches =
            listOf(
                SettingsPatch(basis = Basis.KR),
                SettingsPatch(alertCommodity = Change(null), quietTime = Change(null)),
                SettingsPatch(
                    locale = AppLanguage.EN,
                    timezone = "Asia/Seoul",
                    basis = Basis.US,
                    maPeriod = MaPeriod.MA200,
                    window = ChartWindow.ALL,
                    rareLevels = listOf(0.1, 2.5, 5.0, 10.0, 49.9),
                    alertCommodity = Change(CommodityCode.NG),
                    quietTime = Change(QuietTime(startMin = 1380, endMin = 420)),
                ),
            )
        patches.forEach { assertValid("settingsPatch", it.toJson().toString()) }
        assertEquals("""{"alertCommodity":null,"quietTime":null}""", patches[1].toJson().toString())
        assertEquals("""{"maPeriod":200}""", SettingsPatch(maPeriod = MaPeriod.MA200).toJson().toString())
        assertInvalid("settingsPatch", SettingsPatch(rareLevels = listOf(1.0, 2.0, 3.0, 4.0, 50.0)).toJson().toString())
    }

    @Test
    fun deviceRegistrationKeepsNullFields() {
        val unregistered = DeviceRegistration(pushToken = null, platform = Platform.ANDROID, appVersion = null)
        assertEquals("""{"pushToken":null,"platform":"ANDROID","appVersion":null}""", unregistered.toJson().toString())
        assertValid("deviceRegistration", unregistered.toJson().toString())
        assertValid(
            "deviceRegistration",
            DeviceRegistration(pushToken = "fcm-token", platform = Platform.ANDROID, appVersion = "aos-1.0.0")
                .toJson()
                .toString(),
        )
    }

    @Test
    fun queriesMatchSchemas() {
        val disparity =
            DisparityQuery(
                Basis.FRONT,
                MaPeriod.MA20,
                ChartWindow.Y3,
                listOf(1.0, 2.5, 5.0, 10.0, 20.0),
                date(2018, 1, 2),
            )
        assertEquals(
            mapOf(
                "basis" to "FRONT",
                "ma" to "20",
                "window" to "Y3",
                "levels" to "1,2.5,5,10,20",
                "before" to "2018-01-02",
            ),
            disparity.toQuery(),
        )
        assertValid("disparityQuery", queryJson(disparity.toQuery()))
        assertValid("disparityQuery", queryJson(disparity.copy(before = null).toQuery()))
        assertValid("contractsQuery", queryJson(ContractsQuery().toQuery()))
        assertValid("contractsQuery", queryJson(ContractsQuery(LocalDate.of(2018, 1, 2)).toQuery()))
        assertValid("curveQuery", queryJson(CurveQuery(LocalDate.of(2020, 4, 20)).toQuery()))
        assertValid("calendarQuery", queryJson(CalendarQuery(date(2026, 1, 1), date(2026, 12, 31)).toQuery()))
    }

    @Test
    fun levelsUseNumberDefaultStrings() {
        assertEquals("1", formatLevel(1.0))
        assertEquals("2.5", formatLevel(2.5))
        assertEquals("10", formatLevel(10.0))
        assertEquals("0.1", formatLevel(0.1))
        assertEquals("49.9", formatLevel(49.9))
        // 계약이 거절하는 형식(지수·공백)을 만들지 않는다
        assertInvalid("disparityQuery", """{"basis":"FRONT","ma":"20","window":"Y3","levels":"1e1,2"}""")
    }

    private fun queryJson(query: Map<String, String?>): String =
        JsonObject(query.filterValues { it != null }.mapValues { JsonPrimitive(it.value) }).toString()

    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ): LocalDate = LocalDate.of(year, month, day)

    /** 숫자는 값으로(`20`과 `20.0`), 시각은 같은 순간인지로(`…00.000Z`와 `…00Z`) 비교한다 */
    private fun assertSameJson(
        path: String,
        expected: JsonElement,
        actual: JsonElement,
    ) {
        when {
            expected is JsonObject && actual is JsonObject -> {
                assertEquals("$path keys", expected.keys, actual.keys)
                expected.forEach { (key, value) -> assertSameJson("$path.$key", value, actual.getValue(key)) }
            }
            expected is JsonArray && actual is JsonArray -> {
                assertEquals("$path size", expected.size, actual.size)
                expected.indices.forEach { assertSameJson("$path[$it]", expected[it], actual[it]) }
            }
            expected is JsonNull || actual is JsonNull -> assertEquals(path, expected, actual)
            expected is JsonPrimitive && actual is JsonPrimitive -> assertSamePrimitive(path, expected, actual)
            else -> assertEquals(path, expected, actual)
        }
    }

    private fun assertSamePrimitive(
        path: String,
        expected: JsonPrimitive,
        actual: JsonPrimitive,
    ) {
        assertEquals("$path isString", expected.isString, actual.isString)
        when {
            expected.content == actual.content -> Unit
            expected.isString -> assertEquals(path, instantOrNull(expected.content), Instant.parse(actual.content))
            else -> assertEquals(path, expected.doubleOrNull, actual.doubleOrNull)
        }
    }

    private fun instantOrNull(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()

    private companion object {
        val roundTripJson = Json { explicitNulls = true }
    }
}
