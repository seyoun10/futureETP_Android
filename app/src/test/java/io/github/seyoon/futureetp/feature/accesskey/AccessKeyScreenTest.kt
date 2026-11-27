package io.github.seyoon.futureetp.feature.accesskey

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.seyoon.futureetp.core.api.AppErrorCode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class AccessKeyScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val submitted = mutableListOf<String>()
    private val events = mutableListOf<String>()

    // 실제 카메라 대신 버튼 두 개로 읽기 결과·카메라 없음을 넣는다
    private val fakeScanner: QrScannerSlot = { onScanned, onUnavailable, _ ->
        TextButton(onClick = { events += "accepted:${onScanned("scanned-qr")}" }) { Text("가짜 QR") }
        TextButton(onClick = onUnavailable) { Text("가짜 카메라 없음") }
    }

    private fun show(
        state: AccessKeyUiState = AccessKeyUiState(),
        rejected: Boolean = false,
        onBack: (() -> Unit)? = null,
    ) = compose.setContent {
        AccessKeyScreen(
            state,
            rejected,
            onBack,
            onSubmit = { submitted += it },
            onScanQr = { events += "scanQr" },
            onScanned = {
                events += "scanned:$it"
                true
            },
            onCameraUnavailable = { events += "unavailable" },
            onCancelScan = { events += "cancel" },
            scanner = fakeScanner,
        )
    }

    @Test
    fun confirmIsBlockedUntilSomethingIsPasted() {
        show()
        compose.onNodeWithText("확인").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("pasted-key")
        compose.onNodeWithText("확인").assertIsEnabled().performClick()
        assertEquals(listOf("pasted-key"), submitted)
        // 첫 실행(키 없음)에는 [←]가 없다
        compose.onNodeWithContentDescription("뒤로").assertDoesNotExist()
    }

    @Test
    fun pastedKeyIsHidden() {
        show()
        // 화면에는 가린 글자만 그린다. 실제 값은 접근성 트리에 Password 표시와 함께만 있다
        compose.onNode(hasSetTextAction()).performTextInput("secret-key")
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun storedKeyRejectionIsShownUntilVerifying() {
        show(rejected = true, onBack = {})
        compose.onNodeWithText("키가 맞지 않습니다.").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로").assertIsDisplayed()
    }

    @Test
    fun verifyingHidesRejectionAndShowsProgress() {
        show(AccessKeyUiState(verifying = true), rejected = true)
        compose.onNodeWithText("키가 맞지 않습니다.").assertDoesNotExist()
        compose.onNodeWithContentDescription("확인 중").assertIsDisplayed()
    }

    @Test
    fun failuresUseTheirOwnText() {
        show(AccessKeyUiState(failure = AppErrorCode.NETWORK))
        compose.onNodeWithText("네트워크에 연결할 수 없습니다.").assertIsDisplayed()
    }

    @Test
    fun badFormatNotice() {
        show(AccessKeyUiState(notice = AccessKeyNotice.BAD_FORMAT))
        compose.onNodeWithText("키 형식이 맞지 않습니다.").assertIsDisplayed()
    }

    @Test
    fun scanButtonAsksForTheCamera() {
        show()
        compose.onNodeWithText("가짜 QR").assertDoesNotExist()
        compose.onNodeWithText("QR 읽기").performClick()
        assertEquals(listOf("scanQr"), events)
    }

    @Test
    fun scannerPassesReadsAndCanBeCancelled() {
        show(AccessKeyUiState(scanning = true))
        compose.onNodeWithText("QR 읽기").assertDoesNotExist()
        compose.onNodeWithText("가짜 QR").performClick()
        compose.onNodeWithText("가짜 카메라 없음").performClick()
        compose.onNodeWithText("취소").performClick()
        assertEquals(listOf("scanned:scanned-qr", "accepted:true", "unavailable", "cancel"), events)
    }

    @Test
    fun scanIsBlockedWhileVerifying() {
        show(AccessKeyUiState(verifying = true))
        compose.onNodeWithText("QR 읽기").assertIsNotEnabled()
    }

    @Test
    fun qrNotices() {
        var notice by mutableStateOf(AccessKeyNotice.NOT_KEY_QR)
        compose.setContent {
            AccessKeyScreen(
                AccessKeyUiState(notice = notice),
                false,
                null,
                {},
                {},
                { false },
                {},
                {},
                scanner = fakeScanner,
            )
        }
        compose.onNodeWithText("접근 키 QR이 아닙니다.").assertIsDisplayed()
        notice = AccessKeyNotice.CAMERA_UNAVAILABLE
        compose.onNodeWithText("카메라를 쓸 수 없습니다. 키를 붙여넣어 주세요.").assertIsDisplayed()
    }
}
