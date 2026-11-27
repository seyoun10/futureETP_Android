package io.github.seyoon.futureetp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

/** AppCompatActivity라야 Android 12 이하에서도 앱 안 언어가 적용된다(AOS-015) */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 시작할 때 세로로 잠근다(RN 루트 _layout, D-076)
        lockPortrait()
        val container = (application as FutureEtpApplication).container
        // 처음 실행이면 기기 언어로 앱 언어를 정해 둔다. 바뀌면 Activity가 한 번 다시 만들어진다(AOS-015)
        container.appLocale.ensureDefault()
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
