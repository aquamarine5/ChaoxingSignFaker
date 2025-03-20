/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRCodeScanScreen() {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    if (cameraPermission.status == PermissionStatus.Granted) {
        LocalContext.current.let { context ->
            val barcodeScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE
                ).build()
            )
            val previewView= PreviewView(context)
            val cameraController = LifecycleCameraController(context).apply {
                setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context), MlKitAnalyzer(
                    listOf(barcodeScanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context),
                ) {
                    it?.let { result ->
                        val barcodeResult = result.getValue(barcodeScanner) ?: return@MlKitAnalyzer
                        if (barcodeResult.size > 0) {
                            val barcode = barcodeResult[0]

                        }
                    }
                })
            }
            AndroidView(
                factory = {previewView}
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val text = if (cameraPermission.status.shouldShowRationale) {
                "相机权限已拒绝，点击按钮再次请求"
            } else {
                "相机权限已被禁止"
            }
            Text(text = text)
            Button(onClick = {
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

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}