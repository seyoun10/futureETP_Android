package io.github.seyoon.futureetp.feature.accesskey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 서버에 묻기 전에 앱이 거른 경우의 안내 */
enum class AccessKeyNotice { BAD_FORMAT, }

/**
 * @property failure 새 키 확인·저장 실패. UNAUTHORIZED면 "키가 맞지 않습니다", 그 밖은 공용 에러 문구
 * @property saved 저장 끝. 화면이 한 번 보고 들어온 곳으로 돌아간다
 */
data class AccessKeyUiState(
    val verifying: Boolean = false,
    val notice: AccessKeyNotice? = null,
    val failure: AppErrorCode? = null,
    val saved: Boolean = false,
)

/**
 * B-b 접근 키(screens.md B-b, D-044, D-077). 입력한 키를 [verify](`GET /v1/settings`를 그 키로)로 확인한 뒤에만 [save]한다.
 * 어느 단계에서 실패해도 저장하지 않는다
 */
class AccessKeyViewModel(
    private val verify: suspend (String) -> Unit,
    private val save: suspend (String) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(AccessKeyUiState())
    val state: StateFlow<AccessKeyUiState> = _state.asStateFlow()

    fun submit(input: String) {
        if (_state.value.verifying) return
        val key = parseAccessKey(input)
        if (key == null) {
            _state.update { it.copy(notice = AccessKeyNotice.BAD_FORMAT, failure = null) }
            return
        }
        verifyAndSave(key)
    }

    private fun verifyAndSave(key: String) {
        _state.update { it.copy(verifying = true, notice = null, failure = null) }
        viewModelScope.launch {
            val failure =
                try {
                    verify(key)
                    save(key)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    failureCode(e)
                }
            _state.update { it.copy(verifying = false, failure = failure, saved = failure == null) }
        }
    }
}

// 저장소 쓰기 실패(Keystore 오류 등)처럼 AppError가 아니면 RN과 같이 INTERNAL 문구
private fun failureCode(e: Exception): AppErrorCode = (e as? AppError)?.code ?: AppErrorCode.INTERNAL
