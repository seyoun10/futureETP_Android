package io.github.seyoon.futureetp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.data.DISPARITY_SETTINGS_DEFAULTS
import io.github.seyoon.futureetp.core.data.SettingsRepository
import io.github.seyoon.futureetp.core.query.QueryState
import io.github.seyoon.futureetp.core.settings.UpDownStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

/** 표시 설정(기기)을 읽고 바꾸는 쪽. 언어는 per-app language, 등락 색은 DataStore(AOS-014, AOS-015) */
class DisplayControls(
    val upDownStyle: Flow<UpDownStyle>,
    val setUpDownStyle: suspend (UpDownStyle) -> Unit,
    val resetUpDownStyle: suspend () -> Unit,
    val language: () -> AppLanguage,
    val setLanguage: (AppLanguage) -> Unit,
    val defaultLanguage: () -> AppLanguage,
)

/**
 * B 설정(screens.md B, RN `SettingsScreen`). 표시 설정은 기기에, 이격도차트·알림 설정은 서버에 기기별로 둔다(D-047, D-120).
 * 언어는 푸시 문구에 쓰므로 서버에도 보낸다
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val display: DisplayControls,
) : ViewModel() {
    val settings: StateFlow<QueryState<DeviceSettings>> = repository.state
    val saveError: StateFlow<Throwable?> = repository.saveError
    val upDownStyle: StateFlow<UpDownStyle?> =
        display.upDownStyle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** 언어를 바꾸면 Activity가 다시 만들어지므로 그릴 때마다 읽으면 된다 */
    val language: AppLanguage get() = display.language()

    init {
        repository.refresh()
    }

    fun retry() = repository.retry()

    fun update(patch: SettingsPatch) = repository.update(patch)

    fun setUpDownStyle(style: UpDownStyle) {
        viewModelScope.launch { display.setUpDownStyle(style) }
    }

    /** PATCH를 먼저 줄에 넣고 언어를 바꾼다(바꾸면 Activity가 다시 만들어진다) */
    fun setLanguage(language: AppLanguage) {
        repository.update(SettingsPatch(locale = language))
        display.setLanguage(language)
    }

    /** 표시·이격도차트 설정만 기본값으로 되돌린다. 알림(B-a)과 접근 키는 그대로다 */
    fun reset() {
        val language = display.defaultLanguage()
        repository.update(DISPARITY_SETTINGS_DEFAULTS.copy(locale = language))
        viewModelScope.launch {
            display.resetUpDownStyle()
            display.setLanguage(language)
        }
    }
}
