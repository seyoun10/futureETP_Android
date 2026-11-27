@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.seyoon.futureetp.core.query

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

private const val MINUTE = 60_000L

/** 조회마다 [responses]를 차례로 쓴다(마지막 것을 되풀이). 값이면 성공, 예외면 실패 */
private class FakeFetch(
    vararg responses: Any,
    private val latencyMs: Long = 0,
) {
    private val responses = responses.toList()
    var calls = 0

    suspend operator fun invoke(): Result {
        val response = responses[minOf(calls, responses.lastIndex)]
        calls++
        if (latencyMs > 0) delay(latencyMs)
        if (response is Throwable) throw response
        return response as Result
    }
}

private data class Result(
    val nextUpdateAt: Instant?,
    val tag: String = "",
)

private fun TestScope.query(
    fetch: FakeFetch,
    scope: CoroutineScope = backgroundScope,
    staleTimeMs: Long = 0,
    appReturns: MutableSharedFlow<Unit> = MutableSharedFlow(),
    invalidations: MutableSharedFlow<Unit> = MutableSharedFlow(),
) = Query(
    scope = scope,
    fetch = { fetch() },
    nextUpdateAt = { it.nextUpdateAt },
    staleTimeMs = { staleTimeMs },
    appReturns = appReturns,
    invalidations = invalidations,
    now = { testScheduler.currentTime },
)

private val network = AppError(AppErrorCode.NETWORK, "offline")

class QueryTest {
    @Test
    fun fetchesOnCreation() =
        runTest {
            val q = query(FakeFetch(Result(null, "a")))
            runCurrent()
            assertEquals(
                "a",
                q.state.value.data
                    ?.tag,
            )
            assertEquals(false, q.state.value.isFetching)
        }

    @Test
    fun failedRefreshKeepsData() =
        runTest {
            val q = query(FakeFetch(Result(null, "a"), network))
            runCurrent()
            q.refetch()
            runCurrent()
            assertEquals(
                "a",
                q.state.value.data
                    ?.tag,
            )
            assertSame(network, q.state.value.error)
        }

    @Test
    fun autoRefreshRunsOneMinuteAfterNextUpdateAt() =
        runTest {
            val fetch = FakeFetch(Result(Instant.ofEpochMilli(10 * MINUTE)))
            val q = query(fetch)
            backgroundScope.launch { q.autoRefresh() }
            advanceTimeBy(11 * MINUTE - 1)
            assertEquals(1, fetch.calls)
            advanceTimeBy(2)
            assertEquals(2, fetch.calls)
        }

    @Test
    fun noAutoRefreshWithoutNextUpdateAt() =
        runTest {
            val fetch = FakeFetch(Result(null))
            val q = query(fetch)
            backgroundScope.launch { q.autoRefresh() }
            advanceTimeBy(24 * 60 * MINUTE)
            assertEquals(1, fetch.calls)
        }

    @Test
    fun failingAutoRefreshRetriesEveryMinute() =
        runTest {
            // 예상 시각이 이미 지났어도 마지막 조회 + 1분보다 이르지 않다
            val fetch = FakeFetch(Result(Instant.EPOCH), network)
            val q = query(fetch)
            backgroundScope.launch { q.autoRefresh() }
            advanceTimeBy(MINUTE + 1)
            assertEquals(2, fetch.calls)
            advanceTimeBy(MINUTE)
            assertEquals(3, fetch.calls)
            assertEquals(
                Instant.EPOCH,
                q.state.value.data
                    ?.nextUpdateAt,
            )
        }

    @Test
    fun appReturnRefetchesOnlyStaleData() =
        runTest {
            val fetch = FakeFetch(Result(null))
            val returns = MutableSharedFlow<Unit>()
            query(fetch, staleTimeMs = 5 * MINUTE, appReturns = returns)
            runCurrent()
            advanceTimeBy(MINUTE)
            returns.emit(Unit)
            runCurrent()
            assertEquals(1, fetch.calls)
            advanceTimeBy(5 * MINUTE)
            returns.emit(Unit)
            runCurrent()
            assertEquals(2, fetch.calls)
        }

    @Test
    fun appReturnRefetchesFailedFirstLoad() =
        runTest {
            val fetch = FakeFetch(network, Result(null, "b"))
            val returns = MutableSharedFlow<Unit>()
            val q = query(fetch, staleTimeMs = 5 * MINUTE, appReturns = returns)
            runCurrent()
            returns.emit(Unit)
            runCurrent()
            assertEquals(
                "b",
                q.state.value.data
                    ?.tag,
            )
            assertNull(q.state.value.error)
        }

    @Test
    fun invalidationCancelsRunningFetchAndRefetches() =
        runTest {
            val fetch = FakeFetch(Result(null, "old"), Result(null, "new"), latencyMs = 1_000)
            val invalidations = MutableSharedFlow<Unit>()
            val q = query(fetch, invalidations = invalidations)
            advanceTimeBy(500)
            invalidations.emit(Unit)
            advanceTimeBy(2_000)
            assertEquals(2, fetch.calls)
            assertEquals(
                "new",
                q.state.value.data
                    ?.tag,
            )
        }

    @Test
    fun refetchWithoutCancelSkipsWhileRunning() =
        runTest {
            val fetch = FakeFetch(Result(null), latencyMs = 1_000)
            val q = query(fetch)
            advanceTimeBy(500)
            q.refetch(cancelRunning = false)
            advanceTimeBy(2_000)
            assertEquals(1, fetch.calls)
        }

    @Test
    fun appReturnsEmitsOnlyWhenComingBackToForeground() =
        runTest {
            val owner =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry.createUnsafe(this)
                }
            val registry = owner.lifecycle
            registry.currentState = Lifecycle.State.RESUMED
            val emitted = mutableListOf<Unit>()
            backgroundScope.launch { appReturns(registry).collect { emitted += it } }
            runCurrent()
            registry.currentState = Lifecycle.State.STARTED
            runCurrent()
            assertEquals(0, emitted.size)
            registry.currentState = Lifecycle.State.CREATED
            runCurrent()
            registry.currentState = Lifecycle.State.RESUMED
            runCurrent()
            assertEquals(1, emitted.size)
        }
}
