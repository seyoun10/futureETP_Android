package io.github.seyoon.futureetp.core.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

// 앱은 세로로 고정하고, C-a~C-c에서만 [⟲]로 가로를 잠근다(screens.md C, D-076).
// 잠금은 제품 결정(D-076, 개인용 폰 앱)이라 lint SourceLockedOrientationActivity를 이 함수에서만 끈다.
// Android 16+ 큰 화면처럼 시스템이 잠금을 무시해도 레이아웃은 창 모양을 따른다.

/** RN `lockPortrait`(expo `PORTRAIT_UP`). */
@SuppressLint("SourceLockedOrientationActivity")
fun Activity.lockPortrait() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

/** RN `lockLandscape`(expo `LANDSCAPE`: 가로 두 방향은 센서를 따른다). */
fun Activity.lockLandscape() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

/** 레이아웃은 잠금 상태가 아니라 실제 창 모양으로 정한다(RN `useIsLandscape`). */
@Composable
fun isLandscape(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.width > size.height
}
