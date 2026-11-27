@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.seyoon.futureetp.feature.commoditylist

import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.CommodityList
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.data.CommodityListRepository
import io.github.seyoon.futureetp.core.data.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private val settings =
    DeviceSettings(
        locale = AppLanguage.KO,
        timezone = "Asia/Seoul",
        basis = Basis.FRONT,
        maPeriod = MaPeriod.MA100,
        window = ChartWindow.Y10,
        rareLevels = listOf(1.0, 2.0, 4.0, 8.0, 16.0),
        alertCommodity = null,
        quietTime = null,
    )

class CommodityListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    /** 목록 조회마다 다음 응답을 기다린다(없으면 [listValue]를 바로 준다) */
    private val listGates = ArrayDeque<CompletableDeferred<Result<CommodityList>>>()
    private var listValue = commodityList()
    private var settingsGate: CompletableDeferred<DeviceSettings>? = null

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): CommodityListViewModel {
        val list =
            CommodityListRepository(
                backgroundScope,
                fetch = { listGates.removeFirstOrNull()?.await()?.getOrThrow() ?: listValue },
                now = { testScheduler.currentTime },
            )
        val settingsRepository =
            SettingsRepository(
                backgroundScope,
                fetch = { settingsGate?.await() ?: settings },
                patch = { settings },
            )
        return CommodityListViewModel(list, settingsRepository).also { runCurrent() }
    }

    @Test
    fun pullRefreshKeepsIndicatorUntilDone() =
        runTest(dispatcher) {
            val vm = viewModel()
            val gate = CompletableDeferred<Result<CommodityList>>().also(listGates::add)
            vm.pullRefresh()
            runCurrent()
            assertTrue(vm.pullRefreshing.value)
            val fresh = commodityList(asOf = "2026-10-22T01:00:00Z")
            gate.complete(Result.success(fresh))
            runCurrent()
            assertFalse(vm.pullRefreshing.value)
            assertFalse(vm.refreshFailed.value)
            assertEquals(fresh, vm.state.value.data)
        }

    @Test
    fun timeoutShowsFailureAndLateResultStillUpdatesList() =
        runTest(dispatcher) {
            val vm = viewModel()
            val gate = CompletableDeferred<Result<CommodityList>>().also(listGates::add)
            vm.pullRefresh()
            advanceTimeBy(PULL_REFRESH_TIMEOUT_MS - 1)
            runCurrent()
            assertTrue(vm.pullRefreshing.value)
            advanceTimeBy(1)
            runCurrent()
            assertFalse(vm.pullRefreshing.value)
            assertTrue(vm.refreshFailed.value)

            vm.dismissRefreshFailed()
            val late = commodityList(asOf = "2026-10-22T02:00:00Z")
            gate.complete(Result.success(late))
            runCurrent()
            assertEquals(late, vm.state.value.data)
            assertFalse(vm.refreshFailed.value)
        }

    @Test
    fun failedRefreshShowsFailureAndKeepsList() =
        runTest(dispatcher) {
            val vm = viewModel()
            val before = vm.state.value.data
            listGates += CompletableDeferred(Result.failure(AppError(AppErrorCode.NETWORK, "offline")))
            vm.pullRefresh()
            runCurrent()
            assertTrue(vm.refreshFailed.value)
            assertEquals(before, vm.state.value.data)
            vm.dismissRefreshFailed()
            assertFalse(vm.refreshFailed.value)
        }

    @Test
    fun alertOffOnlyAfterSettingsArrive() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<DeviceSettings>()
            settingsGate = gate
            val vm = viewModel()
            assertFalse(vm.alertOff.value)
            gate.complete(settings)
            runCurrent()
            assertTrue(vm.alertOff.value)
        }

    @Test
    fun alertTargetHidesTheDot() =
        runTest(dispatcher) {
            settingsGate = CompletableDeferred(settings.copy(alertCommodity = CommodityCode.CL))
            val vm = viewModel()
            assertFalse(vm.alertOff.value)
        }
}
