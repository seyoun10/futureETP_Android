package io.github.seyoon.futureetp.core.api

import androidx.annotation.StringRes
import io.github.seyoon.futureetp.R

/** 서버 코드(계약 `schemas/error.json`) + 앱 전용 코드(D-042). 화면은 `code`만 보고 문구를 고른다 */
enum class AppErrorCode(
    @param:StringRes val messageRes: Int,
    /** 조회 자동 재시도 대상(D-072). 4xx·SCHEMA_MISMATCH는 다시 보내도 같다 */
    val isRetryable: Boolean = false,
) {
    UNAUTHORIZED(R.string.errors_unauthorized),
    RATE_LIMITED(R.string.errors_rate_limited),
    VALIDATION_FAILED(R.string.errors_validation_failed),
    NOT_FOUND(R.string.errors_not_found),
    INTERNAL(R.string.errors_internal, isRetryable = true),
    NETWORK(R.string.errors_network, isRetryable = true),
    TIMEOUT(R.string.errors_timeout, isRetryable = true),
    SCHEMA_MISMATCH(R.string.errors_schema_mismatch),
    ;

    companion object {
        /** 서버가 보낸 코드. 계약에 없는 코드는 SCHEMA_MISMATCH로 본다(RN 봉투 스키마 검사와 같음) */
        private val SERVER_CODES = listOf(UNAUTHORIZED, RATE_LIMITED, VALIDATION_FAILED, NOT_FOUND, INTERNAL)

        fun fromServer(code: String): AppErrorCode? = SERVER_CODES.firstOrNull { it.name == code }
    }
}

/** API 호출의 모든 실패(D-042). 호출자 취소(CancellationException)만 이 형태가 아니다 */
class AppError(
    val code: AppErrorCode,
    message: String = code.name,
    val status: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** 화면에 보일 에러 문구. AppError가 아니면 INTERNAL 문구(RN `errorText`) */
@StringRes
fun errorTextRes(error: Throwable): Int = ((error as? AppError)?.code ?: AppErrorCode.INTERNAL).messageRes
