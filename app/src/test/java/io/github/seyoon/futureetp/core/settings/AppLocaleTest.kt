package io.github.seyoon.futureetp.core.settings

import io.github.seyoon.futureetp.core.api.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleTest {
    private class Fake(
        var stored: String?,
        var device: String?,
    ) {
        val sets = mutableListOf<String>()
        val locale =
            AppLocale(
                appLocales = { stored },
                setAppLocales = {
                    sets += it
                    stored = it
                },
                deviceLanguage = { device },
            )
    }

    @Test
    fun defaultFollowsDeviceLanguageKoOrElseEn() {
        assertEquals(AppLanguage.KO, defaultAppLanguage("ko"))
        assertEquals(AppLanguage.EN, defaultAppLanguage("en"))
        assertEquals(AppLanguage.EN, defaultAppLanguage("ja"))
        assertEquals(AppLanguage.EN, defaultAppLanguage(null))
    }

    @Test
    fun readsLanguageTags() {
        assertEquals(AppLanguage.KO, appLanguageOf("ko-KR"))
        assertEquals(AppLanguage.EN, appLanguageOf("en"))
        assertNull(appLanguageOf("ja"))
        assertNull(appLanguageOf(null))
    }

    @Test
    fun firstDefaultIsKeptWhenDeviceLanguageChanges() {
        val f = Fake(stored = null, device = "ja")
        f.locale.ensureDefault()
        f.device = "ko"
        f.locale.ensureDefault()
        assertEquals(listOf("en"), f.sets)
        assertEquals(AppLanguage.EN, f.locale.current)
    }

    @Test
    fun unsupportedStoredLanguageIsReplacedByDefault() {
        val f = Fake(stored = "ja", device = "ko")
        assertEquals(AppLanguage.KO, f.locale.current)
        f.locale.ensureDefault()
        assertEquals(listOf("ko"), f.sets)
    }

    @Test
    fun setOnlyWritesWhenChanged() {
        val f = Fake(stored = "ko", device = "ko")
        f.locale.set(AppLanguage.KO)
        f.locale.set(AppLanguage.EN)
        assertEquals(listOf("en"), f.sets)
        assertEquals(AppLanguage.EN, f.locale.current)
    }
}
