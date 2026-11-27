package io.github.seyoon.futureetp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.seyoon.futureetp.core.theme.FutureEtpTheme
import io.github.seyoon.futureetp.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as FutureEtpApplication).container
        setContent {
            FutureEtpTheme {
                AppNavigation(container)
            }
        }
    }
}
