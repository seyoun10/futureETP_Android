package io.github.seyoon.futureetp.feature.devhealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seyoon.futureetp.core.api.Health
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DevHealthState {
    data object Loading : DevHealthState

    data class Ok(
        val commit: String?,
    ) : DevHealthState

    data class Failed(
        val reason: String,
    ) : DevHealthState
}

/** 2단계 임시 화면: dev 서버 연결 확인. B 화면을 만들 때 지운다 */
class DevHealthViewModel(
    private val loadHealth: suspend () -> Health,
) : ViewModel() {
    private val _state = MutableStateFlow<DevHealthState>(DevHealthState.Loading)
    val state: StateFlow<DevHealthState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = DevHealthState.Loading
        viewModelScope.launch {
            _state.value =
                try {
                    DevHealthState.Ok(loadHealth().commit)
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    DevHealthState.Failed(e.message ?: e::class.simpleName.orEmpty())
                }
        }
    }
}
