package io.github.seyoon.futureetp.feature.accesskey

/** QR 내용 접두사(D-044). 키 생성 스크립트가 `futureetp-key:v1:<키>`로 띄운다 */
const val ACCESS_KEY_QR_PREFIX = "futureetp-key:v1:"

// 32바이트 난수 base64url(패딩 없음) = 43자(D-077)
private val KEY_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")

/**
 * QR은 접두사가 있어야 받고([requirePrefix]), 붙여넣기는 키만 넣어도 받는다. 형식이 아니면 null이고 서버에 묻지 않는다
 * (RN `parseAccessKey`)
 */
fun parseAccessKey(
    input: String,
    requirePrefix: Boolean = false,
): String? {
    val text = input.trim()
    val hasPrefix = text.startsWith(ACCESS_KEY_QR_PREFIX)
    if (requirePrefix && !hasPrefix) return null
    val key = if (hasPrefix) text.removePrefix(ACCESS_KEY_QR_PREFIX) else text
    return key.takeIf { KEY_PATTERN.matches(it) }
}
