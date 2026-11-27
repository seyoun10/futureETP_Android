package io.github.seyoon.futureetp.feature.alertsettings

import androidx.lifecycle.ViewModel
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import io.github.seyoon.futureetp.core.data.SettingsRepository
import io.github.seyoon.futureetp.core.query.QueryState
import kotlinx.coroutines.flow.StateFlow

/** B-a 알림 설정(screens.md B-a, RN `AlertSettingsScreen`). B와 같은 설정 저장소를 쓰므로 바꾼 값은 B에도 바로 보인다 */
class AlertSettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<QueryState<DeviceSettings>> = repository.state
    val saveError: StateFlow<Throwable?> = repository.saveError

    init {
        repository.refresh()
    }

    fun retry() = repository.retry()

    fun update(patch: SettingsPatch) = repository.update(patch)
}
