package io.github.seyoon.futureetp.core.api

import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.CalendarQuery
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.api.model.DeviceRegistration
import io.github.seyoon.futureetp.core.api.model.DisparityQuery
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.Platform
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class EndpointsTest {
    private val requests = mutableListOf<HttpRequestData>()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val levels = listOf(1.0, 2.0, 5.0, 10.0, 20.0)

    private fun client(data: String): ApiClient {
        val engine =
            MockEngine { request ->
                requests += request
                respond("""{"success":true,"data":$data}""", headers = jsonHeaders)
            }
        val credentials =
            object : ApiCredentials {
                override suspend fun accessToken() = "key"

                override suspend fun deviceId() = "device-1"
            }
        return ApiClient(createHttpClient(engine), "http://server", credentials)
    }

    private fun fixture(name: String) = File("src/test/resources/contract/fixtures/$name.json").readText()

    @Test
    fun commoditiesDecodesFixture() =
        runTest {
            val list: CommodityList = client(fixture("commodities")).commodities()
            assertEquals("/v1/commodities", requests.single().url.encodedPath)
            assertEquals(CommodityCode.entries.toSet(), list.commodities.map { it.code }.toSet())
            assertEquals(
                YearMonth::class,
                list.commodities
                    .first()
                    .contracts
                    .first()
                    .contractMonth::class,
            )
        }

    @Test
    fun disparitySendsQuery() =
        runTest {
            val query =
                DisparityQuery(Basis.KR, MaPeriod.MA200, ChartWindow.ALL, levels, LocalDate.of(2018, 1, 2))
            client(fixture("disparityChart.before")).disparityChart(CommodityCode.CL, query)
            val url = requests.single().url
            assertEquals("/v1/commodities/CL/disparity", url.encodedPath)
            assertEquals("KR", url.parameters["basis"])
            assertEquals("200", url.parameters["ma"])
            assertEquals("ALL", url.parameters["window"])
            assertEquals("1,2,5,10,20", url.parameters["levels"])
            assertEquals("2018-01-02", url.parameters["before"])
        }

    @Test
    fun patchAndPutSendJsonBodies() =
        runTest {
            client(fixture("settings")).patchSettings(SettingsPatch(maPeriod = MaPeriod.MA50))
            client(fixture("device")).registerDevice(DeviceRegistration(null, Platform.ANDROID, "aos-1.0.0"))
            val (patch, put) = requests
            assertEquals(HttpMethod.Patch, patch.method)
            assertEquals("""{"maPeriod":50}""", (patch.body as TextContent).text)
            assertEquals(HttpMethod.Put, put.method)
            assertEquals("/v1/devices", put.url.encodedPath)
            val putBody = (put.body as TextContent).text
            assertEquals("""{"pushToken":null,"platform":"ANDROID","appVersion":"aos-1.0.0"}""", putBody)
        }

    private suspend fun assertSchemaMismatch(
        label: String,
        call: suspend () -> Unit,
    ) {
        try {
            call()
            fail("$label should fail")
        } catch (e: AppError) {
            assertEquals(label, AppErrorCode.SCHEMA_MISMATCH, e.code)
        }
    }

    @Test
    fun unknownEnumOrBadDateIsSchemaMismatch() =
        runTest {
            val settings = fixture("settings")
            assertSchemaMismatch("unknown enum") { client(settings.replace("\"FRONT\"", "\"BACK\"")).settings() }
            val badMa = settings.replace("\"maPeriod\": 100", "\"maPeriod\": 30")
            assertSchemaMismatch("unknown maPeriod") { client(badMa).settings() }
            val calendar = fixture("calendar.missing").replace("\"2027-06-01\"", "\"2027-6-1\"")
            assertSchemaMismatch("bad date") {
                client(calendar).calendar(CalendarQuery(LocalDate.of(2027, 6, 1), LocalDate.of(2028, 5, 31)))
            }
        }
}
