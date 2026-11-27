package io.github.seyoon.futureetp.feature.accesskey

import android.content.Context
import android.util.Log
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import io.github.seyoon.futureetp.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation

/**
 * B-b 카메라 영역. 테스트는 가짜로 바꾼다(AOS-013)
 * @param onScanned 읽은 QR 내용. 받아들였으면 true를 돌려주고, 그 뒤 들어오는 값은 넘기지 않는다
 * @param onUnavailable 뒤 카메라가 없거나 열지 못함
 */
typealias QrScannerSlot = @Composable (
    onScanned: (String) -> Boolean,
    onUnavailable: () -> Unit,
    modifier: Modifier,
) -> Unit

/** 정사각형 미리보기에서 QR만 읽는다(D-078). 권한은 띄우기 전에 받는다 */
@Composable
fun QrCameraScanner(
    onScanned: (String) -> Boolean,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnScanned by rememberUpdatedState(onScanned)
    val latestOnUnavailable by rememberUpdatedState(onUnavailable)
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    val label = stringResource(R.string.access_key_scanner_label)

    LaunchedEffect(lifecycleOwner) {
        // 카메라는 QR이 보이는 동안 계속 결과를 보내므로 한 번만 넘긴다
        var accepted = false
        try {
            bindCamera(context, lifecycleOwner, onSurface = { surfaceRequest = it }) { value ->
                if (!accepted) accepted = latestOnScanned(value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (
            // 카메라 없음(IllegalArgumentException)·연결 실패(CameraInfoUnavailableException 등)
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Log.w("QrCameraScanner", "camera unavailable", e)
            latestOnUnavailable()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { contentDescription = label },
    ) {
        surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }
    }
}

/** 미리보기·분석을 묶고 취소될 때까지 기다린 뒤 푼다 */
private suspend fun bindCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    onSurface: (SurfaceRequest) -> Unit,
    onValue: (String) -> Unit,
) {
    val provider = ProcessCameraProvider.awaitInstance(context)
    require(provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) { "no back camera" }
    val executor = ContextCompat.getMainExecutor(context)
    val scanner =
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    val preview = Preview.Builder().build().apply { setSurfaceProvider(onSurface) }
    val analysis =
        ImageAnalysis
            .Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    analysis.setAnalyzer(
        executor,
        MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
            result
                .getValue(scanner)
                ?.firstNotNullOfOrNull { it.rawValue }
                ?.let(onValue)
        },
    )
    try {
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        awaitCancellation()
    } finally {
        provider.unbind(preview, analysis)
        analysis.clearAnalyzer()
        scanner.close()
    }
}
