package io.github.seyoon.futureetp.core.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ko")
class SettingsHeaderButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun alertOffShowsRedDot() {
        compose.setContent { SettingsHeaderButton(alertOff = true, onClick = {}) }
        compose.onNodeWithTag(HEADER_BADGE_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun alertOnHasNoDot() {
        compose.setContent { SettingsHeaderButton(alertOff = false, onClick = {}) }
        compose.onNodeWithTag(HEADER_BADGE_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}
