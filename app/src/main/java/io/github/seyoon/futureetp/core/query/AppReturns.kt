package io.github.seyoon.futureetp.core.query

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * 앱이 백그라운드에서 다시 화면에 올라올 때마다 내보낸다(RN focusManager의 AppState `active`).
 * 프로세스 lifecycle(ProcessLifecycleOwner)을 넘긴다. 화면 회전 같은 Activity 재생성에는 내보내지 않는다.
 * 모으기 시작한 때의 상태는 복귀로 보지 않는다
 */
fun appReturns(processLifecycle: Lifecycle): Flow<Unit> =
    processLifecycle.currentStateFlow
        .map { it.isAtLeast(Lifecycle.State.STARTED) }
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .map { }
