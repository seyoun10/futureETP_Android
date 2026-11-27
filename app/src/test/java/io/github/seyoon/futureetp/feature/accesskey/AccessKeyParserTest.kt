package io.github.seyoon.futureetp.feature.accesskey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessKeyParserTest {
    private val key = "A".repeat(20) + "b-_9" + "z".repeat(19)

    @Test
    fun pasteAcceptsBareKeyOrPrefixedKeyAndTrims() {
        assertEquals(43, key.length)
        assertEquals(key, parseAccessKey("  $key\n"))
        assertEquals(key, parseAccessKey("$ACCESS_KEY_QR_PREFIX$key"))
    }

    @Test
    fun qrNeedsThePrefix() {
        assertNull(parseAccessKey(key, requirePrefix = true))
        assertEquals(key, parseAccessKey("$ACCESS_KEY_QR_PREFIX$key", requirePrefix = true))
    }

    @Test
    fun rejectsWrongLengthOrCharacters() {
        assertNull(parseAccessKey(key.dropLast(1)))
        assertNull(parseAccessKey(key + "a"))
        assertNull(parseAccessKey(key.dropLast(1) + "="))
        assertNull(parseAccessKey(key.dropLast(1) + "+"))
        assertNull(parseAccessKey("futureetp-key:v2:$key"))
        assertNull(parseAccessKey(""))
    }
}
