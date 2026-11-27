package io.github.seyoon.futureetp.core.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import java.util.Locale

/** 첫 기기 언어가 한국어면 한국어, 그 밖은 영어(D-073, RN `displaySettings.ts`) */
fun defaultAppLanguage(deviceLanguage: String?): AppLanguage =
    when (deviceLanguage) {
        "ko" -> AppLanguage.KO
        else -> AppLanguage.EN
    }

/** 언어 태그(`ko`, `en-US` 등)를 앱 언어로 읽는다. 앱이 지원하지 않는 언어면 null */
fun appLanguageOf(tag: String?): AppLanguage? =
    when (tag?.let { Locale.forLanguageTag(it).language }) {
        "ko" -> AppLanguage.KO
        "en" -> AppLanguage.EN
        else -> null
    }

private val AppLanguage.tag: String
    get() =
        when (this) {
            AppLanguage.KO -> "ko"
            AppLanguage.EN -> "en"
        }

/**
 * 앱 안 언어(per-app language, AOS-011, AOS-015). 값은 시스템(Android 13+ LocaleManager, 그 아래는 AppCompat 저장)이 갖는다.
 * 처음 실행할 때 그때의 기기 언어로 기본값을 저장해, 나중에 기기 언어가 바뀌어도 첫 값을 유지한다(RN persist와 같음)
 */
class AppLocale(
    private val appLocales: () -> String?,
    private val setAppLocales: (String) -> Unit,
    private val deviceLanguage: () -> String?,
) {
    /** 지금 앱 언어. 아직 정하지 않았거나 지원하지 않는 값이면 기기 기본값 */
    val current: AppLanguage
        get() = appLanguageOf(appLocales()) ?: defaultAppLanguage(deviceLanguage())

    /** 저장된 앱 언어가 없으면 기기 기본값을 저장한다. 바꾸면 Activity가 다시 만들어진다 */
    fun ensureDefault() {
        if (appLanguageOf(appLocales()) == null) setAppLocales(defaultAppLanguage(deviceLanguage()).tag)
    }

    /** 지금 기기 언어의 기본값(B [초기화]) */
    val deviceDefault: AppLanguage
        get() = defaultAppLanguage(deviceLanguage())

    fun set(language: AppLanguage) {
        if (appLanguageOf(appLocales()) != language) setAppLocales(language.tag)
    }
}

/** AppCompat per-app language. 기기 언어는 앱 언어 설정과 상관없는 시스템 언어(expo-localization `getLocales()[0]`) */
fun createAppLocale(context: Context): AppLocale =
    AppLocale(
        appLocales = { AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() },
        setAppLocales = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it)) },
        deviceLanguage = { LocaleManagerCompat.getSystemLocales(context)[0]?.language },
    )
