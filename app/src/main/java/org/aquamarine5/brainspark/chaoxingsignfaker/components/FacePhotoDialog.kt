/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacePhotoDialog(
    onDismissRequest: () -> Unit,
    onStartCamera: () -> Unit,
    pendingCapturedBitmap: Bitmap? = null,
    onPendingCapturedBitmapHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun reload() {
        val records = context.chaoxingDataStore.data.first()
            .faceRecognitionConfiguresMap[ChaoxingHttpClient.instance!!.userEntity.phoneNumber]
            ?.imagesList.orEmpty().take(ChaoxingFaceHelper.MAX_FACE_IMAGES)
        files = records.map { record ->
            ChaoxingFaceHelper.getFaceImageFile(context, record.objectId)
        }
    }

    fun save(bitmap: Bitmap) {
        if (files.size >= 3) {
            snackbarHost.displaySnackbar(
                "最多只能保存 ${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张人脸照片",
                coroutineScope
            )
            return
        }
        coroutineScope.launch {
            isLoading = true
            runCatching {
                ChaoxingFaceHelper.saveFaceImage(
                    ChaoxingHttpClient.instance!!,
                    context,
                    bitmap,
                )
                reload()
            }.onSuccess {
                snackbarHost.displaySnackbar("人脸照片上传成功", coroutineScope)
            }.onFailure {
                it.snackbarReport(snackbarHost, coroutineScope, "上传人脸照片失败", hapticFeedback)
            }
            isLoading = false
        }
    }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        .use { BitmapFactory.decodeStream(it) }
                        ?: error("无法读取照片")
                }.onSuccess(::save).onFailure {
                    it.snackbarReport(snackbarHost, coroutineScope, "读取照片失败", hapticFeedback)
                }
            }
        }

    LaunchedEffect(Unit) {
        runCatching { reload() }.onFailure {
            it.snackbarReport(snackbarHost, coroutineScope, "加载人脸照片失败", hapticFeedback)
        }
        isLoading = false
    }

    LaunchedEffect(pendingCapturedBitmap) {
        if (pendingCapturedBitmap != null) {
            save(pendingCapturedBitmap)
            onPendingCapturedBitmapHandled()
        }
    }


    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("管理人脸照片") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 440.dp),
                ) {
                    if (isLoading) {
                        CenterCircularProgressIndicator()
                    } else if (files.isEmpty()) {
                        Text("暂无人脸照片，最多可保存 ${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { Text("已保存 ${files.size}/${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张") }
                            items(files) { file ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = "已上传的人脸照片",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4f / 3f),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = false,
                        enabled = (!isLoading && files.size < ChaoxingFaceHelper.MAX_FACE_IMAGES),
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("上传照片") }
                    SegmentedButton(
                        selected = false,
                        enabled = (!isLoading && files.size < ChaoxingFaceHelper.MAX_FACE_IMAGES),
                        onClick = onStartCamera,
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("拍摄照片") }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("关闭")
            }
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_scan_face),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
    )
}