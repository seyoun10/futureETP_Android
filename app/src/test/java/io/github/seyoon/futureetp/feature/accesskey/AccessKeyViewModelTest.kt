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

    @Test
    fun scanStartClearsEarlierMessages() {
        val vm = viewModel()
        vm.submit("short")
        vm.startScan()
        assertEquals(AccessKeyUiState(scanning = true), vm.state.value)
        vm.cancelScan()
        assertEquals(AccessKeyUiState(), vm.state.value)
    }

    @Test
    fun qrWithoutPrefixIsNotAcceptedAndScanningGoesOn() {
        val vm = viewModel()
        vm.startScan()
        // 붙여넣기와 달리 QR은 접두사가 있어야 한다(D-078)
        assertEquals(false, vm.scanned(key))
        assertEquals(AccessKeyUiState(scanning = true, notice = AccessKeyNotice.NOT_KEY_QR), vm.state.value)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun acceptedQrClosesTheCameraAndSavesOnce() {
        val vm = viewModel()
        vm.startScan()
        assertEquals(true, vm.scanned("$ACCESS_KEY_QR_PREFIX$key"))
        // 카메라가 닫힌 뒤 늦게 온 결과는 무시한다
        assertEquals(false, vm.scanned("$ACCESS_KEY_QR_PREFIX$key"))
        assertEquals(listOf("verify:$key", "save:$key"), calls)
        assertEquals(AccessKeyUiState(saved = true), vm.state.value)
    }

    @Test
    fun cameraUnavailableClosesTheCameraWithPasteNotice() {
        val vm = viewModel(verify = { throw AppError(AppErrorCode.NETWORK) })
        vm.submit(key)
        vm.startScan()
        vm.cameraUnavailable()
        assertEquals(AccessKeyUiState(notice = AccessKeyNotice.CAMERA_UNAVAILABLE), vm.state.value)
    }
}
