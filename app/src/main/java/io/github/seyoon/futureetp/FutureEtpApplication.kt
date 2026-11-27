package io.github.seyoon.futureetp

import android.app.Application

class FutureEtpApplication : Application() {
    // 수동 DI 컨테이너(AOS-005)
    val container: AppContainer by lazy { AppContainer() }
}
