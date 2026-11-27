package io.github.seyoon.futureetp.feature.commoditylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.data.CommodityListRepository
import io.github.seyoon.futureetp.core.data.SettingsRepository
import io.github.seyoon.futureetp.core.query.Query
import io.github.seyoon.futureetp.core.query.QueryState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 당겨서 새로고침이 이 시간 안에 끝나지 않으면 인디케이터를 거두고 안내를 띄운다(D-138) */
const val PULL_REFRESH_TIMEOUT_MS = 5_000L

/**
 * A 원자재 목록(screens.md A, RN `CommodityListScreen`, D-085).
 * 당겨서 새로고침은 끝날 때까지 [pullRefreshing]을 유지하고, 시간 초과·실패면 [refreshFailed]를 켠다.
 * 시간 초과로 거둔 뒤에도 조회는 계속되어 늦게 끝나면 목록이 바뀐다
 */
class CommodityListViewModel(
    private val list: CommodityListRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<QueryState<CommodityList>> = list.state

    /** 화면이 보이는 동안 자동 새로고침할 조회 */
    val query: Query<CommodityList> get() = list.query

    /** 이 기기의 알림 대상이 없으면 true. 설정을 받기 전·첫 조회 실패면 false(D-124) */
    val alertOff: StateFlow<Boolean> =
        settings.state
            .map { s -> s.data?.let { it.alertCommodity == null } ?: false }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _pullRefreshing = MutableStateFlow(false)
    val pullRefreshing: StateFlow<Boolean> = _pullRefreshing.asStateFlow()

    private val _refreshFailed = MutableStateFlow(false)
    val refreshFailed: StateFlow<Boolean> = _refreshFailed.asStateFlow()

    private var pullJob: Job? = null

    init {
        settings.refresh()
    }

    fun retry() = list.retry()

    fun pullRefresh() {
        pullJob?.cancel()
        _pullRefreshing.value = true
        pullJob =
            viewModelScope.launch {
                val ok = withTimeoutOrNull(PULL_REFRESH_TIMEOUT_MS) { list.pullRefresh() } ?: false
                _pullRefreshing.value = false
                if (!ok) _refreshFailed.value = true
            }
    }

    /** 안내의 [확인]. 닫기만 하고 다시 조회하지 않는다 */
    fun dismissRefreshFailed() {
        _refreshFailed.value = false
    }
}
