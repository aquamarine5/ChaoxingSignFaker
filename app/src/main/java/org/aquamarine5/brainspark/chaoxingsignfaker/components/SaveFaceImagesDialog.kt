/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClientPool
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@Composable
fun SaveFaceImagesDialog(
    faceRecognitionData: FaceRecognitionData,
    otherUserSessionList: List<ChaoxingOtherUserSession?>,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    SnackbarAlertDialog(
        onDismissRequest = {
            faceRecognitionData.newImagePhones.clear()
            onFinished()
        },
        title = { Text("保存人脸照片？") },
        text = { Text("是否保存刚才拍摄的 ${faceRecognitionData.newImagePhones.size} 张人脸照片，以便下次签到使用？") },
        confirmButton = {
            Button(onClick = {
                coroutineScope.launch {
                    faceRecognitionData.newImagePhones.toList().forEach { phoneNumber ->
                        faceRecognitionData.capturedBitmaps[phoneNumber]?.let { bitmap ->
                            runCatching {
                                val savedImage = ChaoxingFaceHelper.saveFaceImage(
                                    ChaoxingHttpClientPool.get(context, phoneNumber),
                                    context,
                                    bitmap,
                                    phoneNumber
                                )
                                runCatching {
                                    ChaoxingFaceHelper.afterUsingFaceImage(
                                        context,
                                        phoneNumber,
                                        savedImage.objectId,
                                        false
                                    )
                                }
                                faceRecognitionData.capturedBitmaps.remove(phoneNumber)
                                faceRecognitionData.setStatus(
                                    FaceRecognitionImageStatus.HaveImage,
                                    phoneNumber,
                                    otherUserSessionList
                                )
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "保存人脸照片失败",
                                    hapticFeedback
                                )
                            }
                        }
                    }
                    faceRecognitionData.newImagePhones.clear()
                    onFinished()
                }
            }) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                faceRecognitionData.newImagePhones.clear()
                onFinished()
            }) { Text("不保存") }
        },
        icon = {
            Icon(
                painterResource(R.drawable.ic_image_plus),
                null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
