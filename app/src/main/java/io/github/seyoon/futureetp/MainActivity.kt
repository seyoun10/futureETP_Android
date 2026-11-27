package io.github.seyoon.futureetp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.seyoon.futureetp.core.settings.DisplaySettings
import io.github.seyoon.futureetp.core.theme.AppColors
import io.github.seyoon.futureetp.core.theme.FutureEtpTheme
import io.github.seyoon.futureetp.core.ui.lockPortrait
import io.github.seyoon.futureetp.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 시작할 때 세로로 잠근다(RN 루트 _layout, D-076)
        lockPortrait()
        val container = (application as FutureEtpApplication).container
        setContent {
            // 표시 설정을 읽기 전에는 빈 배경이다. 기본값으로 먼저 그렸다가 색이 바뀌며 깜빡이지 않게 한다(AOS-014)
            val display: DisplaySettings? by container.displaySettings.settings
                .collectAsStateWithLifecycle(initialValue = null)
            val current = display
            if (current == null) {
                Box(Modifier.fillMaxSize().background(AppColors.background))
            } else {
                FutureEtpTheme(upDownStyle = current.upDownStyle) {
                    AppNavigation(container)
                }
            }
        }
    }
}
