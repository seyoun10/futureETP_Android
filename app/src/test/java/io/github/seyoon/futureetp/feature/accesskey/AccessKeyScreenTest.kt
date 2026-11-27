package io.github.seyoon.futureetp.feature.accesskey

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

    private fun show(
        state: AccessKeyUiState = AccessKeyUiState(),
        rejected: Boolean = false,
        onBack: (() -> Unit)? = null,
    ) = compose.setContent { AccessKeyScreen(state, rejected, onBack, onSubmit = { submitted += it }) }

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
}
