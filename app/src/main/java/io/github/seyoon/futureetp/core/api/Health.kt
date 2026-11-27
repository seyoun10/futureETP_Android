package io.github.seyoon.futureetp.core.api

import kotlinx.serialization.Serializable

/** 계약 `schemas/health.json`: status는 "ok" 고정, commit은 nullable 필수 */
@Serializable
data class Health(
    val status: String,
    val commit: String?,
)
