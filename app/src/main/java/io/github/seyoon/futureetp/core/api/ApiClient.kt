package io.github.seyoon.futureetp.core.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIMEOUT_MS = 15_000L

val apiJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

/** 타임아웃 15초(D-072). 재시도·AppError·헤더는 3단계 API client 항목에서 넣는다 */
fun createHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(apiJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }
    }

class ApiClient(
    private val http: HttpClient,
    private val baseUrl: String,
) {
    /** `GET /v1/health`. 인증 헤더가 필요 없는 유일한 엔드포인트(D-036) */
    suspend fun health(): Health {
        val envelope: ApiEnvelope<Health> = http.get("$baseUrl/v1/health").body()
        return envelope.dataOrThrow()
    }
}
