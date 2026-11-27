package io.github.seyoon.futureetp.core.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientTest {
    private fun client(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onPath: (String) -> Unit = {},
    ): ApiClient {
        val engine =
            MockEngine { request ->
                onPath(request.url.toString())
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        return ApiClient(createHttpClient(engine), "http://server")
    }

    @Test
    fun healthDecodesTheEnvelope() =
        runTest {
            var url = ""
            val health = client("""{"success":true,"data":{"status":"ok","commit":"abc123"}}""") { url = it }.health()
            assertEquals("http://server/v1/health", url)
            assertEquals(Health(status = "ok", commit = "abc123"), health)
        }

    @Test
    fun healthAcceptsNullCommit() =
        runTest {
            val health = client("""{"success":true,"data":{"status":"ok","commit":null}}""").health()
            assertNull(health.commit)
        }

    @Test
    fun failureEnvelopeThrowsServerCode() =
        runTest {
            val api =
                client(
                    """{"success":false,"error":{"code":"INTERNAL","message":"db"}}""",
                    HttpStatusCode.InternalServerError,
                )
            val e = assertThrows(ApiException::class.java) { kotlinx.coroutines.runBlocking { api.health() } }
            assertEquals("INTERNAL", e.code)
        }
}
