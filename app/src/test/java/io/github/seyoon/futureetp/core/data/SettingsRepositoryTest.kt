@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.seyoon.futureetp.core.data

import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import io.github.seyoon.futureetp.core.api.model.AppLanguage
import io.github.seyoon.futureetp.core.api.model.Basis
import io.github.seyoon.futureetp.core.api.model.Change
import io.github.seyoon.futureetp.core.api.model.ChartWindow
import io.github.seyoon.futureetp.core.api.model.CommodityCode
import io.github.seyoon.futureetp.core.api.model.DeviceSettings
import io.github.seyoon.futureetp.core.api.model.MaPeriod
import io.github.seyoon.futureetp.core.api.model.SettingsPatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

private val server =
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

private val network = AppError(AppErrorCode.NETWORK, "offline")

/** 서버 흉내. PATCH마다 [gates]의 다음 응답을 기다린다(없으면 바로 반영) */
private class FakeServer {
    var value = server
    var fetches = 0
    val patches = mutableListOf<SettingsPatch>()
    val gates = ArrayDeque<CompletableDeferred<Throwable?>>()
    var fetchGate: CompletableDeferred<Unit>? = null

    suspend fun fetch(): DeviceSettings {
        fetches++
        fetchGate?.await()
        return value
    }

    suspend fun patch(p: SettingsPatch): DeviceSettings {
        patches += p
        gates.removeFirstOrNull()?.await()?.let { throw it }
        value = value.applying(p)
        return value
    }
}

private fun TestScope.repository(
    fake: FakeServer,
    onAlert: () -> Unit = {},
) = SettingsRepository(
    scope = backgroundScope,
    fetch = fake::fetch,
    patch = fake::patch,
    now = { testScheduler.currentTime },
    onAlertCommodityChanged = onAlert,
)

class SettingsRepositoryTest {
    @Test
    fun pendingPatchesShowOnTopOfServerValueInOrder() =
        runTest {
            val fake = FakeServer()
            val first = CompletableDeferred<Throwable?>()
            fake.gates += first
            val repo = repository(fake)
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.KR))
            repo.update(SettingsPatch(basis = Basis.US, maPeriod = MaPeriod.MA20))
            runCurrent()
            assertEquals(
                Basis.US,
                repo.state.value.data
                    ?.basis,
            )
            assertEquals(
                MaPeriod.MA20,
                repo.state.value.data
                    ?.maPeriod,
            )
            // 두 번째 PATCH는 첫 번째가 끝난 뒤에 간다
            assertEquals(1, fake.patches.size)
            first.complete(null)
            runCurrent()
            assertEquals(listOf(Basis.KR, Basis.US), fake.patches.map { it.basis })
            assertEquals(
                Basis.US,
                repo.state.value.data
                    ?.basis,
            )
        }

    @Test
    fun failedPatchDropsOnlyThatChangeAndRefetches() =
        runTest {
            val fake = FakeServer()
            fake.gates += CompletableDeferred(network)
            val repo = repository(fake)
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.KR))
            repo.update(SettingsPatch(window = ChartWindow.Y3))
            runCurrent()
            assertEquals(
                Basis.FRONT,
                repo.state.value.data
                    ?.basis,
            )
            assertEquals(
                ChartWindow.Y3,
                repo.state.value.data
                    ?.window,
            )
            assertSame(network, repo.saveError.value)
        }

    @Test
    fun failedPatchRefetchesServerValue() =
        runTest {
            val fake = FakeServer()
            fake.gates += CompletableDeferred(network)
            val repo = repository(fake)
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.KR))
            runCurrent()
            assertEquals(2, fake.fetches)
            assertEquals(
                Basis.FRONT,
                repo.state.value.data
                    ?.basis,
            )
        }

    @Test
    fun newChangeClearsSaveError() =
        runTest {
            val fake = FakeServer()
            fake.gates += CompletableDeferred(network)
            val repo = repository(fake)
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.KR))
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.US))
            assertNull(repo.saveError.value)
        }

    @Test
    fun slowFetchDoesNotOverwritePatchResult() =
        runTest {
            val fake = FakeServer()
            val repo = repository(fake)
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            fake.fetchGate = gate
            repo.refresh()
            runCurrent()
            val stale = fake.value
            repo.update(SettingsPatch(basis = Basis.KR))
            runCurrent()
            fake.value = stale
            gate.complete(Unit)
            runCurrent()
            assertEquals(
                Basis.KR,
                repo.state.value.data
                    ?.basis,
            )
        }

    @Test
    fun alertCommodityChangeNotifiesList() =
        runTest {
            val fake = FakeServer()
            var notified = 0
            val repo = repository(fake) { notified++ }
            runCurrent()
            repo.update(SettingsPatch(basis = Basis.KR))
            repo.update(SettingsPatch(alertCommodity = Change(CommodityCode.CL)))
            runCurrent()
            assertEquals(1, notified)
            assertEquals(
                CommodityCode.CL,
                repo.state.value.data
                    ?.alertCommodity,
            )
        }
}
