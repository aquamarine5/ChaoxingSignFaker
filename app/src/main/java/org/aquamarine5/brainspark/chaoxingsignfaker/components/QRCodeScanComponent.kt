/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.activity.compose.LocalActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
inline fun QRCodeScanComponent(
    isPause: MutableState<Boolean>,
    isLoading: MutableState<Boolean>,
    noinline onClose: () -> Unit,
    crossinline onScanResult: @DisallowComposableCalls (Barcode) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val hapticFeedback= LocalHapticFeedback.current
    if (cameraPermission.status == PermissionStatus.Granted) {
        val application = LocalActivity.current!!
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = ContextCompat.getMainExecutor(application)
        LocalContext.current.let { context ->
            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE
                ).build()
            )
            val previewView = remember { PreviewView(context) }
            val preview = remember { Preview.Builder().build() }
            val controller = remember {
                LifecycleCameraController(context).apply {
                    this.bindToLifecycle(lifecycleOwner)
                    previewView.controller = this
                    setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
                    preview.surfaceProvider = previewView.surfaceProvider
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    setImageAnalysisAnalyzer(
                        cameraExecutor, MlKitAnalyzer(
                            listOf(barcodeScanner),
                            ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                            cameraExecutor,
                        ) {
                            it?.let { result ->
                                val barcodeResult =
                                    result.getValue(barcodeScanner) ?: return@MlKitAnalyzer
                                if (barcodeResult.isNotEmpty()) {
                                    val barcode = barcodeResult[0]
                                    previewView.overlay.clear()
                                    previewView.overlay.add(QRCodeDrawable(barcode))
                                    if (!isPause.value) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        onScanResult(barcode)
                                    }
                                }
                            }
                        })
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (isLoading.value) {
                    CenterCircularProgressIndicator()
                }
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .offset(y = 38.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_left), "返回")
                }

                content()
            }
            DisposableEffect(Unit) {
                onDispose {
                    barcodeScanner.close()
                    controller.unbind()
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val text = if (cameraPermission.status.shouldShowRationale) {
                "相机权限已拒绝，点击按钮再次请求"
            } else {
                "相机权限已被禁止"
            }
            Text(text = text)
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                cameraPermission.launchPermissionRequest()
            }) {
                Text("点击获取权限")
            }
        }
    }
}

class QRCodeDrawable(private val barcode: Barcode) : Drawable() {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    override fun draw(canvas: Canvas) {
        barcode.boundingBox?.let {
            val rectF = RectF(it)
            canvas.drawRoundRect(rectF, 16F, 16F, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}