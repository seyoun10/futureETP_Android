package io.github.seyoon.futureetp.core.push

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * 알림 권한 상태(RN `usePushPermission`). [granted]는 앱 알림이 켜져 있는지(권한·앱 알림 스위치 모두).
 * [canAsk]면 OS 권한 창을 띄울 수 있고, 아니면 OS 앱 알림 설정 화면을 연다
 */
data class NotificationPermission(
    val granted: Boolean,
    val canAsk: Boolean,
)

class NotificationPermissionState(
    /** 확인 전이면 null */
    val status: NotificationPermission?,
    val request: () -> Unit,
)

/**
 * 화면이 올라올 때마다(설정 앱에서 돌아올 때 포함) 다시 읽는다.
 * Android 13+는 권한 창을 두 번 거절하면 다시 묻지 않는데, 처음 묻기 전과 구분할 수 없어 [허용]을 눌러 창 없이 거절되면 그때부터 [설정 열기]로 바꾼다
 */
@Composable
fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var askBlocked by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = notificationsEnabled(context)
            val activity = context.findActivity()
            if (!ok && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, PERMISSION)) {
                askBlocked = true
            }
        }
    LifecycleResumeEffect(Unit) {
        granted = notificationsEnabled(context)
        onPauseOrDispose {}
    }
    val canAsk = !askBlocked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionGranted(context)
    return NotificationPermissionState(
        status = granted?.let { NotificationPermission(it, canAsk) },
        request = { if (canAsk) launcher.launch(PERMISSION) else openNotificationSettings(context) },
    )
}

/** `Manifest.permission.POST_NOTIFICATIONS`(API 33). 12 이하에서는 묻지 않고 설정 화면만 연다 */
private const val PERMISSION = "android.permission.POST_NOTIFICATIONS"

private fun notificationsEnabled(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun permissionGranted(context: Context) =
    ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
