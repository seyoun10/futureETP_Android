package io.github.seyoon.futureetp.feature.devhealth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class DevHealthScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun okStateShowsCommitAndRetries() {
        var retries = 0
        compose.setContent {
            DevHealthScreen(DevHealthState.Ok("abc"), "http://10.0.2.2:3000", onRetry = { retries++ })
        }
        compose.onNodeWithText("정상 (커밋 abc)").assertIsDisplayed()
        compose.onNodeWithText("서버: http://10.0.2.2:3000").assertIsDisplayed()
        compose.onNodeWithText("다시 확인").performClick()
        assertEquals(1, retries)
    }
}
