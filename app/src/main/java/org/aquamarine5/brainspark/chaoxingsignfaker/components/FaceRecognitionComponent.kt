/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClientPool
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import java.io.ByteArrayOutputStream
import java.net.URL
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val PROFILE_IMAGE_COMPRESS_QUALITY = 90

private suspend fun randomizeProfileFaceImage(
    bitmap: Bitmap,
    cropLeft: Int,
    cropTop: Int,
    cropWidth: Int,
    cropHeight: Int,
    angle: Float
): Bitmap = withContext(Dispatchers.IO) {
    val matrix = Matrix().apply {
        postTranslate(-cropLeft.toFloat(), -cropTop.toFloat())
        postRotate(angle)
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val radians = Math.toRadians(abs(angle.toDouble()))
    val sin = sin(radians)
    val cos = cos(radians)
    val width = cropWidth.toDouble()
    val height = cropHeight.toDouble()
    val safeWidth = minOf(
        width / (cos + sin * width / height),
        height / (sin + cos * width / height)
    ).toInt().coerceIn(1, minOf(rotated.width, cropWidth))
    val safeHeight = (safeWidth * height / width).toInt()
        .coerceIn(1, minOf(rotated.height, cropHeight))
    ByteArrayOutputStream().use { out ->
        Bitmap.createBitmap(
            rotated,
            (rotated.width - safeWidth) / 2,
            (rotated.height - safeHeight) / 2,
            safeWidth,
            safeHeight
        ).compress(Bitmap.CompressFormat.JPEG, PROFILE_IMAGE_COMPRESS_QUALITY, out)
        BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
            ?: rotated
    }
}

@Composable
fun FaceRecognitionComponent(
    signUserName: List<Pair<String, String>>,
    onCancel: () -> Unit,
    onFinish: (Map<String, Bitmap>, isUseProfileImage: Boolean) -> Unit
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var faceImageCapturedIndex by remember { mutableIntStateOf(0) }
    var useProfileImage by remember { mutableStateOf<Boolean?>(null) }
    var isProcessingProfileImage by remember { mutableStateOf(false) }
    var profileImageProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    BackHandler(enabled = !isProcessingProfileImage) {
        onCancel()
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessingProfileImage) onCancel() },
        title = { Text("使用默认人脸识别照片？") },
        icon = {
            Icon(
                painterResource(R.drawable.ic_triangle_alert),
                contentDescription = "警告",
                tint = Color(0xFFFCC307),
                modifier = Modifier.size(40.dp)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(buildAnnotatedString {
                    append("是否使用代签用户的学习通默认人脸识别照片代替拍摄？")
                    withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                        append("此方式存在风险：使用账号原有照片可能被学习通风控校验识别，导致代签失败甚至影响账号安全。")
                    }
                })
                if (isProcessingProfileImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "正在获取默认人脸识别照片...(" +
                                    "${profileImageProgress?.first ?: 0}/${profileImageProgress?.second ?: signUserName.size})"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isProcessingProfileImage,
                onClick = { useProfileImage = true }
            ) { Text("是") }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isProcessingProfileImage,
                onClick = { useProfileImage = false }
            ) { Text("否") }
        }
    )

    if (useProfileImage == true) {
        LaunchedEffect(useProfileImage) {
            isProcessingProfileImage = true
            profileImageProgress = 0 to signUserName.size
            runCatching {
                buildMap {
                    signUserName.forEachIndexed { index, (phoneNumber, _) ->
                        val url = ChaoxingFaceHelper.getUserProfileFaceImageUrl(
                            ChaoxingHttpClientPool.get(context, phoneNumber)
                        )
                        val bitmap = withContext(Dispatchers.IO) {
                            URL(url).openStream().use { stream ->
                                BitmapFactory.decodeStream(stream)
                                    ?: throw IllegalStateException("默认人脸识别照片下载失败")
                            }
                        }
                        val cropRatio = 0.90f + Random.nextFloat() * 0.09f
                        put(
                            phoneNumber,
                            randomizeProfileFaceImage(
                                bitmap,
                                Random.nextInt(bitmap.width - (bitmap.width * cropRatio).toInt() + 1),
                                Random.nextInt(bitmap.height - (bitmap.height * cropRatio).toInt() + 1),
                                (bitmap.width * cropRatio).toInt().coerceAtLeast(1),
                                (bitmap.height * cropRatio).toInt().coerceAtLeast(1),
                                Random.nextFloat() * 10f - 5f
                            )
                        )
                        profileImageProgress = index + 1 to signUserName.size
                    }
                }
            }.onSuccess {
                isProcessingProfileImage = false
                profileImageProgress = null
                onFinish(it, true)
            }.onFailure {
                it.snackbarReport(
                    snackbarHost,
                    coroutineScope,
                    "获取默认人脸识别照片失败",
                    hapticFeedback
                )
                isProcessingProfileImage = false
                profileImageProgress = null
                useProfileImage = null
            }
        }
    } else if (useProfileImage == false) {
        CameraComponent(signUserName.size, isDefaultBackCamera = false, onNextPhoto = {
            faceImageCapturedIndex++
        }, content = {
            Row(
                modifier = Modifier
                    .animateContentSize()
                    .background(
                        Color(0x88888888),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        BorderStroke(
                            2.dp, Color(0xFF444444)
                        ), RoundedCornerShape(14.dp)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("拍摄给 ${signUserName[faceImageCapturedIndex].second} 人脸识别的图片")
            }
        }) {
            onFinish(
                it.mapIndexed { index, bitmap -> signUserName[index].first to bitmap }
                    .associate { it },
                false
            )
        }
    }
}
