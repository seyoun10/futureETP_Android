package io.github.seyoon.futureetp.feature.devhealth

import io.github.seyoon.futureetp.core.api.Health
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevHealthViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun okShowsCommit() {
        val vm = DevHealthViewModel { Health("ok", "abc") }
        assertEquals(DevHealthState.Ok("abc"), vm.state.value)
    }

    @Test
    fun failureShowsReason() {
        val vm = DevHealthViewModel { error("refused") }
        assertEquals(DevHealthState.Failed("refused"), vm.state.value)
    }
}
