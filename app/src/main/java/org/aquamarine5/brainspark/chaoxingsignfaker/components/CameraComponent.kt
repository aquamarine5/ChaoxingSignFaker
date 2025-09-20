/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraComponent(
    pictureCount: Int = 1,
    content: @Composable (() -> Unit)? = null,
    onNextPhoto: (() -> Unit)? = null,
    onPictureResult: (List<Bitmap>) -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val snackbarHost = LocalSnackbarHostState.current
        if (cameraPermission.status == PermissionStatus.Granted) {
            val application = LocalActivity.current!!
            val future = ProcessCameraProvider.getInstance(application)
            val imageCapture = remember { ImageCapture.Builder().build() }
            val previewView = remember { PreviewView(application) }
            val preview = remember { Preview.Builder().build() }
            var takeImage by remember { mutableStateOf<Bitmap?>(null) }
            var isBackCamera = true
            val lifecycleOwner = LocalLifecycleOwner.current
            val photoList = remember { mutableListOf<Bitmap>() }
            var needTakePictureCount by remember { mutableIntStateOf(pictureCount) }
            future.addListener({
                val cameraProvider = future.get()
                preview.surfaceProvider = previewView.surfaceProvider
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }, ContextCompat.getMainExecutor(application))
            DisposableEffect(Unit) {
                onDispose {
                    future.get().unbindAll()
                }
            }
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            var job: Job? = null
            Box(modifier = Modifier.align(Alignment.Center)) {
                AnimatedContent(
                    takeImage,
                    modifier = Modifier.zIndex(2f),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(220, delayMillis = 90)
                                ) +
                                slideInHorizontally(
                                    animationSpec = tween(220, delayMillis = 90),
                                    initialOffsetX = { -it / 4 })
                                )
                            .togetherWith(
                                fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                                    animationSpec = tween(180),
                                    targetOffsetX = { it / 4 }
                                ))
                    }) {
                    it?.let { img ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.8f)
                                .border(4.dp, Color.White, RectangleShape)
                        ) {
                            Image(
                                img.asImageBitmap(),
                                null
                            )
                        }
                    }
                    LaunchedEffect(it) {
                        job = launch {
                            delay(500L)
                            takeImage = null
                        }
                    }
                }
            }
            val gallery = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val image = application.contentResolver.openInputStream(uri).use {
                        BitmapFactory.decodeStream(it)
                    }
                    photoList.add(image)
                    job?.cancel()
                    takeImage = image
                    needTakePictureCount--
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    if (needTakePictureCount <= 0) {
                        onPictureResult(photoList)
                    } else {
                        onNextPhoto?.invoke()
                    }
                }
            )
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f)
                        .padding(22.dp)
                ) {
                    FloatingActionButton(onClick = {
                        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_images), null
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .zIndex(1f)
                    .align(Alignment.TopEnd)
            ) {
                IconButton(onClick = {
                    future.get().let {
                        it.unbindAll()
                        it.bindToLifecycle(
                            lifecycleOwner,
                            if (isBackCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    }
                    isBackCamera = isBackCamera.not()
                }) {
                    Icon(painterResource(R.drawable.ic_switch_camera), null, tint = Color.White)
                }
            }
            content?.let {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    it()
                }
            }
            Surface(
                onClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(application),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                super.onCaptureSuccess(image)
                                image.use {
                                    val bitmap=it.toBitmap()
                                    photoList.add(bitmap)
                                    job?.cancel()
                                    takeImage = bitmap
                                    needTakePictureCount--
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    if (needTakePictureCount <= 0) {
                                        onPictureResult(photoList)
                                    } else {
                                        onNextPhoto?.invoke()
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "拍照失败",
                                    hapticFeedback
                                )
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                super.onError(exception)
                            }
                        })
                }, color = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(18.dp)
                    .size(60.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Crossfade(needTakePictureCount) {
                        Text(
                            text = it.toString(),
                            color = Color.Black, fontSize = 32.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily(Font(R.font.gilroy))
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("请授予应用拍照权限")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        cameraPermission.launchPermissionRequest()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("授予")
                }
            }
        }
    }
}