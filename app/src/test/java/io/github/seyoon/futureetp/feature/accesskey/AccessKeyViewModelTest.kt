package io.github.seyoon.futureetp.feature.accesskey

import io.github.seyoon.futureetp.core.api.AppError
import io.github.seyoon.futureetp.core.api.AppErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessKeyViewModelTest {
    private val key = "k".repeat(43)
    private val calls = mutableListOf<String>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        verify: suspend (String) -> Unit = { calls += "verify:$it" },
        save: suspend (String) -> Unit = { calls += "save:$it" },
    ) = AccessKeyViewModel(verify, save)

    @Test
    fun badFormatIsNotSentToTheServer() {
        val vm = viewModel()
        vm.submit("short")
        assertEquals(AccessKeyUiState(notice = AccessKeyNotice.BAD_FORMAT), vm.state.value)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun verifiedKeyIsSaved() {
        val vm = viewModel()
        vm.submit(" $ACCESS_KEY_QR_PREFIX$key ")
        assertEquals(listOf("verify:$key", "save:$key"), calls)
        assertEquals(AccessKeyUiState(saved = true), vm.state.value)
    }

    @Test
    fun rejectedNewKeyIsNotSaved() {
        val vm = viewModel(verify = { throw AppError(AppErrorCode.UNAUTHORIZED, status = 401) })
        vm.submit(key)
        assertEquals(AccessKeyUiState(failure = AppErrorCode.UNAUTHORIZED), vm.state.value)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun networkFailureCanBeRetried() {
        var fail = true
        val vm =
            viewModel(verify = {
                if (fail) throw AppError(AppErrorCode.NETWORK)
                calls += "verify:$it"
            })
        vm.submit(key)
        assertEquals(AppErrorCode.NETWORK, vm.state.value.failure)
        fail = false
        vm.submit(key)
        assertEquals(AccessKeyUiState(saved = true), vm.state.value)
    }

    @Test
    fun storeFailureShowsInternal() {
        val vm = viewModel(save = { error("keystore unavailable") })
        vm.submit(key)
        assertEquals(AccessKeyUiState(failure = AppErrorCode.INTERNAL), vm.state.value)
    }
}
