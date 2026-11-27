package io.github.seyoon.futureetp.core.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private const val TIMEOUT_MS = 15_000L
private const val MAX_RETRIES = 2
private const val RETRY_BASE_DELAY_MS = 1_000L
private const val SERVER_ERROR_STATUS = 500

/** 서버가 기기별로 설정·알림을 나누는 헤더(D-120, 계약 `headers`) */
const val DEVICE_ID_HEADER = "X-Device-Id"

val apiJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

/** 타임아웃 15초(D-072). 응답 상태와 상관없이 본문을 읽어 봉투로 판단한다 */
fun createHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }
    }

/** 요청 헤더에 넣을 값. 접근 키·installation ID 저장소가 구현한다(D-044, D-120) */
interface ApiCredentials {
    /** 저장된 접근 키. 없으면 null */
    suspend fun accessToken(): String?

    suspend fun deviceId(): String
}

/**
 * 모든 API 호출의 유일한 경로(D-042). 실패는 모두 [AppError]로 던지고, 호출자 취소만 CancellationException 그대로 던진다.
 * GET은 NETWORK·TIMEOUT·INTERNAL만 최대 2회 다시 보내고(1초·2초 뒤, TanStack Query 기본값), 변경 요청은 다시 보내지 않는다(D-072).
 */
class ApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
    private val credentials: ApiCredentials,
) {
    /** `GET /v1/health`. 인증 헤더가 필요 없는 유일한 엔드포인트(D-036) */
    suspend fun health(): Health = request("/health", Health.serializer(), auth = false)

    /**
     * @param path `/v1` 뒤 경로. 각 조각은 URL 인코딩한다
     * @param query 값이 null인 항목은 빠진다
     * @param body 있으면 JSON으로 보낸다
     * @param auth false면 Authorization·X-Device-Id 없이 보낸다
     * @param token 저장된 키 대신 보낼 키(B-b에서 저장 전에 키 확인, D-044)
     */
    @Suppress("LongParameterList")
    suspend fun <T> request(
        path: String,
        deserializer: DeserializationStrategy<T>,
        method: HttpMethod = HttpMethod.Get,
        query: Map<String, String?> = emptyMap(),
        body: JsonElement? = null,
        auth: Boolean = true,
        token: String? = null,
    ): T {
        val maxRetries = if (method == HttpMethod.Get) MAX_RETRIES else 0
        var attempt = 0
        while (true) {
            try {
                return send(path, deserializer, method, query, body, auth, token)
            } catch (e: AppError) {
                if (attempt >= maxRetries || !e.code.isRetryable) throw e
                delay(RETRY_BASE_DELAY_MS shl attempt)
                attempt++
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun <T> send(
        path: String,
        deserializer: DeserializationStrategy<T>,
        method: HttpMethod,
        query: Map<String, String?>,
        body: JsonElement?,
        auth: Boolean,
        token: String?,
    ): T {
        val headers = if (auth) authHeaders(token) else emptyMap()
        val url =
            URLBuilder()
                .takeFrom(baseUrl)
                .appendPathSegments("v1")
                .appendPathSegments(path.trim('/').split('/'))
                .apply { query.forEach { (key, value) -> if (value != null) parameters.append(key, value) } }
                .build()
        val (status, text) =
            try {
                val response =
                    http.request(url) {
                        this.method = method
                        header(HttpHeaders.Accept, "application/json")
                        headers.forEach { (name, value) -> header(name, value) }
                        if (body != null) {
                            contentType(ContentType.Application.Json)
                            setBody(body.toString())
                        }
                    }
                response.status.value to response.bodyAsText()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                throw transportError(e)
            }
        return decodeEnvelope(path, status, text, deserializer)
    }

    private suspend fun authHeaders(token: String?): Map<String, String> {
        // 키가 없으면 보내도 401이라 요청하지 않는다(D-072)
        val bearer = token ?: credentials.accessToken() ?: throw AppError(AppErrorCode.UNAUTHORIZED, "No access token")
        return mapOf(HttpHeaders.Authorization to "Bearer $bearer", DEVICE_ID_HEADER to credentials.deviceId())
    }
}

private fun transportError(e: Exception): AppError =
    when (e) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            AppError(AppErrorCode.TIMEOUT, "Timed out after ${TIMEOUT_MS}ms", cause = e)
        else -> AppError(AppErrorCode.NETWORK, "Network request failed", cause = e)
    }

/** 응답 봉투 `{ success: true, data } | { success: false, error }`를 풀고 `data`를 모델로 디코딩한다(D-042) */
internal fun <T> decodeEnvelope(
    path: String,
    status: Int,
    text: String,
    deserializer: DeserializationStrategy<T>,
): T {
    val envelope = runCatching { apiJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
    val success = (envelope?.get("success") as? JsonPrimitive)?.booleanOrNull
    val error = (envelope?.get("error") as? JsonObject)?.let(::parseErrorBody)
    val data = envelope?.get("data")
    val unexpected = "Unexpected response body (HTTP $status)"
    val failure =
        when {
            success == true && data != null -> return decodeData(path, status, data, deserializer)
            success == false && error != null -> AppError(error.first, error.second, status)
            // 프록시 오류(502 HTML 등)처럼 봉투가 아닌 응답은 HTTP 상태로 구분한다(D-042)
            status >= SERVER_ERROR_STATUS -> AppError(AppErrorCode.INTERNAL, unexpected, status)
            else -> AppError(AppErrorCode.SCHEMA_MISMATCH, unexpected, status)
        }
    throw failure
}

private fun <T> decodeData(
    path: String,
    status: Int,
    data: JsonElement,
    deserializer: DeserializationStrategy<T>,
): T =
    try {
        apiJson.decodeFromJsonElement(deserializer, data)
    } catch (e: IllegalArgumentException) {
        // SerializationException도 여기 포함된다
        throw AppError(AppErrorCode.SCHEMA_MISMATCH, "Response data does not match schema: $path", status, e)
    }

/** 계약 `schemas/error.json`: code는 서버 코드 enum, message는 문자열. 아니면 null(봉투가 아님) */
private fun parseErrorBody(error: JsonObject): Pair<AppErrorCode, String>? {
    val code = (error["code"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(AppErrorCode::fromServer)
    val message = (error["message"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    return if (code != null && message != null) code to message else null
}
