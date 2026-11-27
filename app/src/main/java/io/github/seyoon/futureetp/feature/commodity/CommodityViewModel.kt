package io.github.seyoon.futureetp.feature.commodity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** C title-bar 상태. 설정을 받기 전·첫 조회 실패면 🔔·빨간 점이 없다(D-124) */
data class CommodityTitleState(
    val isAlert: Boolean = false,
    val alertOff: Boolean = false,
)

/** C 원자재 화면(screens.md C). 지금은 title-bar만 있고 탭(C-a~C-d)은 4단계에서 붙인다 */
class CommodityViewModel(
    val code: CommodityCode,
    settings: SettingsRepository,
) : ViewModel() {
    val title: StateFlow<CommodityTitleState> =
        settings.state
            .map { s ->
                val alert = s.data?.alertCommodity
                if (s.data == null) CommodityTitleState() else CommodityTitleState(alert == code, alert == null)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, CommodityTitleState())

    init {
        settings.refresh()
    }
}
