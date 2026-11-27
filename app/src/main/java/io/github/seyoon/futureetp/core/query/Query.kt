package io.github.seyoon.futureetp.core.query

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/** 배치 완료 예상 시각 뒤 여유(RN `useAutoRefresh.ts`, architecture.md freshness) */
const val AUTO_REFRESH_MARGIN_MS = 60_000L

/** 마지막 조회(성공·실패)부터 다음 자동 조회까지의 최소 간격. 예상 시각이 지났거나 조회가 계속 실패할 때 반복을 막는다 */
const val AUTO_REFRESH_MIN_INTERVAL_MS = 60_000L

/**
 * 조회 하나의 상태(RN TanStack Query 결과 대응). 새로고침이 실패해도 [data]는 남는다.
 * 시각은 [Query]의 `now` 기준 밀리초, 0이면 아직 없음
 */
data class QueryState<T>(
    val data: T? = null,
    val error: Throwable? = null,
    val isFetching: Boolean = false,
    val dataUpdatedAt: Long = 0,
    val errorUpdatedAt: Long = 0,
)

/** 다음 자동 조회 시각. `nextUpdateAt`이 null이면 자동 새로고침하지 않는다(D-048, D-075) */
fun autoRefreshAt(
    nextUpdateAt: Instant?,
    lastFetchedAt: Long,
): Long? =
    nextUpdateAt?.let {
        maxOf(it.toEpochMilli() + AUTO_REFRESH_MARGIN_MS, lastFetchedAt + AUTO_REFRESH_MIN_INTERVAL_MS)
    }

/**
 * ViewModel이 갖는 조회 하나(RN `useQuery` + `useAutoRefresh` + focusManager + `invalidateQueries`).
 * 만들 때 바로 조회한다. 재시도·UNAUTHORIZED 처리는 ApiClient가 맡는다(D-042, D-079).
 * - [appReturns]: 앱이 화면에 다시 올라오면 [staleTimeMs]가 지난 조회만 다시(RN focusManager, D-030)
 * - [invalidations]: 새 접근 키 저장 뒤 진행 중인 조회를 취소하고 다시(RN `invalidateQueries`, D-044)
 * - 자동 새로고침은 화면이 보이는 동안 [autoRefresh]를 부른다(AutoRefreshEffect)
 */
class Query<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend () -> T,
    private val nextUpdateAt: (T) -> Instant? = { null },
    private val staleTimeMs: (T) -> Long = { 0 },
    appReturns: Flow<Unit> = emptyFlow(),
    invalidations: Flow<Unit> = emptyFlow(),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(QueryState<T>())
    val state: StateFlow<QueryState<T>> = _state.asStateFlow()
    private var job: Job? = null

    init {
        refetch()
        scope.launch { appReturns.collect { refetchIfStale() } }
        scope.launch { invalidations.collect { refetch() } }
    }

    /**
     * 다시 조회한다. [cancelRunning]이면 진행 중인 조회를 취소하고 새로(RN `refetch()` 기본값),
     * 아니면 진행 중일 때 아무것도 하지 않는다(자동 새로고침·앱 복귀)
     */
    fun refetch(cancelRunning: Boolean = true) {
        val running = job
        if (running?.isActive == true) {
            if (!cancelRunning) return
            running.cancel()
        }
        _state.update { it.copy(isFetching = true) }
        job =
            scope.launch {
                try {
                    val data = fetch()
                    _state.update { it.copy(data = data, error = null, isFetching = false, dataUpdatedAt = now()) }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    // AppError가 아닌 실패도 오류 화면(INTERNAL 문구)으로 보인다(RN errorText)
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    _state.update { it.copy(error = e, isFetching = false, errorUpdatedAt = now()) }
                }
            }
    }

    /** 진행 중인 조회를 끊는다. 늦게 끝난 조회가 새 값을 옛 값으로 덮지 않게 한다(RN `cancelQueries`) */
    fun cancel() {
        job?.cancel()
        job = null
        _state.update { it.copy(isFetching = false) }
    }

    /** 다른 요청(PATCH 응답 등)으로 받은 서버 값을 넣는다(RN `setQueryData`) */
    fun setData(data: T) {
        _state.update { it.copy(data = data, error = null, dataUpdatedAt = now()) }
    }

    /**
     * 화면이 보이는 동안 부른다. `nextUpdateAt` + 1분(마지막 조회 + 1분보다 이르지 않게)에 다시 조회하고,
     * 조회가 끝날 때마다(성공·실패) 다시 예약한다. 화면이 가려지면 취소된다
     */
    suspend fun autoRefresh() {
        state.collectLatest { s ->
            if (s.isFetching) return@collectLatest
            val runAt =
                autoRefreshAt(s.data?.let(nextUpdateAt), maxOf(s.dataUpdatedAt, s.errorUpdatedAt))
                    ?: return@collectLatest
            delay(runAt - now())
            refetch(cancelRunning = false)
        }
    }

    private fun refetchIfStale() {
        val s = _state.value
        val data = s.data
        if (data == null || now() - s.dataUpdatedAt >= staleTimeMs(data)) refetch(cancelRunning = false)
    }
}
