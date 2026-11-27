package io.github.seyoon.futureetp.core.ui

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class OrientationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun locksPortraitAndLandscape() {
        val activity = compose.activity
        activity.lockLandscape()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)
        activity.lockPortrait()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp")
    fun landscapeFollowsWindowShape() {
        var landscape: Boolean? = null
        compose.setContent { landscape = isLandscape() }
        compose.waitForIdle()
        assertEquals(true, landscape)
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp")
    fun portraitWindowIsNotLandscape() {
        var landscape: Boolean? = null
        compose.setContent { landscape = isLandscape() }
        compose.waitForIdle()
        assertEquals(false, landscape)
    }
}
