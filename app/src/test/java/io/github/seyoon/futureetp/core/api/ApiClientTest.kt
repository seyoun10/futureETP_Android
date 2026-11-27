package io.github.seyoon.futureetp.core.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class ApiClientTest {
    @Serializable
    private data class Item(
        val id: Int,
    )

    private class FakeCredentials(
        var token: String? = "stored-key",
    ) : ApiCredentials {
        override suspend fun accessToken() = token

        override suspend fun deviceId() = "device-1"

        var rejections = 0

        override fun reportRejected() {
            rejections++
        }
    }

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        credentials: ApiCredentials = FakeCredentials(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ApiClient {
        val engine =
            MockEngine { request ->
                requests += request
                handler(request)
            }
        return ApiClient(createHttpClient(engine), "http://server", credentials)
    }

    private fun MockRequestHandleScope.json(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun failure(code: String) = """{"success":false,"error":{"code":"$code","message":"m-$code"}}"""

    private suspend fun ApiClient.item(method: HttpMethod = HttpMethod.Get) = request("/x", Item.serializer(), method)

    private suspend fun expectAppError(block: suspend () -> Unit): AppError {
        try {
            block()
        } catch (e: AppError) {
            return e
        }
        fail("AppError expected")
        error("unreachable")
    }

    @Test
    fun healthDecodesTheEnvelopeWithoutAuthHeaders() =
        runTest {
            val health = client { json("""{"success":true,"data":{"status":"ok","commit":"abc123"}}""") }.health()
            assertEquals(Health(status = "ok", commit = "abc123"), health)
            val request = requests.single()
            assertEquals("http://server/v1/health", request.url.toString())
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[DEVICE_ID_HEADER])
        }

    @Test
    fun healthAcceptsNullCommit() =
        runTest {
            val health = client { json("""{"success":true,"data":{"status":"ok","commit":null}}""") }.health()
            assertNull(health.commit)
        }

    @Test
    fun authRequestSendsBearerDeviceIdAndQuery() =
        runTest {
            val api = client { json("""{"success":true,"data":{"id":1}}""") }
            val query = mapOf("baseDate" to "2026-11-20", "x" to null)
            val item = api.request("/commodities/GC/curve", Item.serializer(), query = query)
            assertEquals(Item(1), item)
            val request = requests.single()
            assertEquals("http://server/v1/commodities/GC/curve?baseDate=2026-11-20", request.url.toString())
            assertEquals("Bearer stored-key", request.headers[HttpHeaders.Authorization])
            assertEquals("device-1", request.headers[DEVICE_ID_HEADER])
            assertEquals("application/json", request.headers[HttpHeaders.Accept])
        }

    @Test
    fun tokenOverrideReplacesStoredKey() =
        runTest {
            client { json("""{"success":true,"data":{"id":1}}""") }
                .request("/settings", Item.serializer(), token = "new-key")
            assertEquals("Bearer new-key", requests.single().headers[HttpHeaders.Authorization])
        }

    @Test
    fun missingKeyFailsWithoutSending() =
        runTest {
            val api = client(FakeCredentials(token = null)) { json("""{"success":true,"data":{"id":1}}""") }
            val e = expectAppError { api.request("/settings", Item.serializer()) }
            assertEquals(AppErrorCode.UNAUTHORIZED, e.code)
            assertTrue(requests.isEmpty())
        }

    @Test
    fun bodyIsSentAsJson() =
        runTest {
            client { json("""{"success":true,"data":{"id":1}}""") }
                .request("/x", Item.serializer(), HttpMethod.Patch, body = buildJsonObject { put("locale", "en") })
            val content = requests.single().body as TextContent
            assertEquals("""{"locale":"en"}""", content.text)
            assertEquals("application/json", content.contentType.toString())
            assertEquals(HttpMethod.Patch, requests.single().method)
        }

    @Test
    fun failureEnvelopeThrowsServerCode() =
        runTest {
            val api = client { json(failure("NOT_FOUND"), HttpStatusCode.NotFound) }
            val e = expectAppError { api.item() }
            assertEquals(AppErrorCode.NOT_FOUND, e.code)
            assertEquals("m-NOT_FOUND", e.message)
            assertEquals(404, e.status)
        }

    @Test
    fun unknownServerCodeIsSchemaMismatch() =
        runTest {
            val api = client { json(failure("NETWORK"), HttpStatusCode.BadRequest) }
            assertEquals(AppErrorCode.SCHEMA_MISMATCH, expectAppError { api.item() }.code)
        }

    @Test
    fun nonEnvelopeBodyIsInternalOn5xxAndSchemaMismatchOtherwise() =
        runTest {
            val proxy = client { respond("<html>Bad Gateway</html>", HttpStatusCode.BadGateway) }
            assertEquals(AppErrorCode.INTERNAL, expectAppError { proxy.item(HttpMethod.Put) }.code)
            val plain = client { json("""{"ok":true}""") }
            assertEquals(AppErrorCode.SCHEMA_MISMATCH, expectAppError { plain.request("/x", Item.serializer()) }.code)
        }

    @Test
    fun dataNotMatchingModelIsSchemaMismatch() =
        runTest {
            val api = client { json("""{"success":true,"data":{"id":"one"}}""") }
            assertEquals(AppErrorCode.SCHEMA_MISMATCH, expectAppError { api.request("/x", Item.serializer()) }.code)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getRetriesRetryableErrorsTwiceWithBackoff() =
        runTest {
            val api =
                client { json(failure("INTERNAL"), HttpStatusCode.InternalServerError) }
            val e = expectAppError { api.request("/x", Item.serializer()) }
            assertEquals(AppErrorCode.INTERNAL, e.code)
            assertEquals(3, requests.size)
            assertEquals(3_000L, currentTime)
        }

    @Test
    fun getSucceedsAfterNetworkFailure() =
        runTest {
            var calls = 0
            val api =
                client {
                    calls++
                    if (calls == 1) throw IOException("offline")
                    json("""{"success":true,"data":{"id":2}}""")
                }
            assertEquals(Item(2), api.request("/x", Item.serializer()))
            assertEquals(2, requests.size)
        }

    @Test
    fun timeoutIsTimeout() =
        runTest {
            val api = client(FakeCredentials()) { throw HttpRequestTimeoutException(it.url.toString(), 15_000) }
            val e = expectAppError { api.request("/x", Item.serializer(), HttpMethod.Patch) }
            assertEquals(AppErrorCode.TIMEOUT, e.code)
            assertEquals(1, requests.size)
        }

    @Test
    fun nonRetryableAndMutationErrorsAreNotRetried() =
        runTest {
            val unauthorized =
                client { json(failure("UNAUTHORIZED"), HttpStatusCode.Unauthorized) }
            assertEquals(AppErrorCode.UNAUTHORIZED, expectAppError { unauthorized.item() }.code)
            assertEquals(1, requests.size)
            requests.clear()
            val mutation = client { throw IOException("offline") }
            assertEquals(AppErrorCode.NETWORK, expectAppError { mutation.item(HttpMethod.Put) }.code)
            assertEquals(1, requests.size)
        }

    @Test
    fun callerCancellationIsNotWrapped() =
        runTest {
            val api = client { throw CancellationException("left screen") }
            try {
                api.request("/x", Item.serializer())
                fail("CancellationException expected")
            } catch (e: CancellationException) {
                assertEquals("left screen", e.message)
            }
            assertEquals(1, requests.size)
        }

    @Test
    fun errorTextFallsBackToInternal() {
        assertEquals(AppErrorCode.TIMEOUT.messageRes, errorTextRes(AppError(AppErrorCode.TIMEOUT)))
        assertEquals(AppErrorCode.INTERNAL.messageRes, errorTextRes(IllegalStateException()))
    }

    @Test
    fun serverRejectionOfStoredKeyIsReported() =
        runTest {
            val creds = FakeCredentials()
            val api = client(creds) { json(failure("UNAUTHORIZED"), HttpStatusCode.Unauthorized) }
            assertEquals(AppErrorCode.UNAUTHORIZED, expectAppError { api.item() }.code)
            assertEquals(1, creds.rejections)
        }

    @Test
    fun newKeyCheckAndMissingKeyAreNotReportedAsRejection() =
        runTest {
            val creds = FakeCredentials()
            val api = client(creds) { json(failure("UNAUTHORIZED"), HttpStatusCode.Unauthorized) }
            // B-b에서 저장 전에 확인하는 새 키(D-079)
            expectAppError { api.request("/settings", Item.serializer(), token = "new-key") }
            // 키가 없어 보내지 않은 요청
            creds.token = null
            expectAppError { api.item() }
            assertEquals(0, creds.rejections)
        }
}
