package io.github.seyoon.futureetp.core.settings

import android.content.Context
import android.content.res.Resources
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transform
import java.io.IOException

/** 등락 색(D-031): 한국식 상승 빨강·하락 파랑, 미국식 상승 초록·하락 빨강 */
enum class UpDownStyle { KR, US }

/**
 * 기기별 UI 설정(D-073, RN `stores/displaySettings.ts`). 서버 데이터는 두지 않는다(D-047).
 * 언어는 per-app language(AOS-011)가 시스템에 저장하므로 여기 두지 않는다
 */
data class DisplaySettings(
    val upDownStyle: UpDownStyle,
)

/** 등락 색은 기기 지역을 따른다. 한국이 아니면 미국식(D-033) */
fun defaultDisplaySettings(regionCode: String?): DisplaySettings =
    DisplaySettings(upDownStyle = if (regionCode == "KR") UpDownStyle.KR else UpDownStyle.US)

private const val FILE_NAME = "display_settings"
private val UP_DOWN_STYLE = stringPreferencesKey("upDownStyle")

/**
 * 표시 설정 저장소(Preferences DataStore, AOS-014). 처음 읽을 때 값이 없으면 그때의 기기 기본값을 저장해
 * 나중에 기기 지역이 바뀌어도 첫 값을 유지한다(RN persist와 같음). 읽을 수 없으면 기본값으로 보인다
 */
class DisplaySettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val regionCode: () -> String?,
) {
    val settings: Flow<DisplaySettings> =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .transform { prefs ->
                val stored = prefs[UP_DOWN_STYLE]?.let { name -> UpDownStyle.entries.find { it.name == name } }
                if (stored != null) {
                    emit(DisplaySettings(stored))
                } else {
                    // 내보내기 전에 저장한다. 받는 쪽이 첫 값만 받고 끊어도 첫 기본값이 남는다
                    val defaults = defaultDisplaySettings(regionCode())
                    remember(defaults)
                    emit(defaults)
                }
            }.distinctUntilChanged()

    suspend fun setUpDownStyle(style: UpDownStyle) {
        dataStore.edit { it[UP_DOWN_STYLE] = style.name }
    }

    private suspend fun remember(defaults: DisplaySettings) {
        try {
            dataStore.edit { if (it[UP_DOWN_STYLE] == null) it[UP_DOWN_STYLE] = defaults.upDownStyle.name }
        } catch (_: IOException) {
            // 저장하지 못하면 다음 실행 때 다시 기본값을 고른다
        }
    }
}

/** 앱 언어 설정과 상관없는 기기 지역(expo-localization `getLocales()[0].regionCode`) */
fun createDisplaySettingsStore(context: Context): DisplaySettingsStore =
    DisplaySettingsStore(
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) },
        regionCode = {
            Resources
                .getSystem()
                .configuration.locales[0]
                .country
                .ifEmpty { null }
        },
    )
