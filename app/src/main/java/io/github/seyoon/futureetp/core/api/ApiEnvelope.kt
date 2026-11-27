package io.github.seyoon.futureetp.core.api

import kotlinx.serialization.Serializable

/** 응답 봉투 `{ success, data } | { success: false, error }`(D-042) */
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiErrorBody? = null,
) {
    fun dataOrThrow(): T =
        if (success && data != null) {
            data
        } else {
            throw ApiException(error?.code ?: "SCHEMA_MISMATCH", error?.message)
        }
}

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
)

class ApiException(
    val code: String,
    override val message: String?,
) : Exception(message)
