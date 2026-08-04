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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingFaceRecognitionImage
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport


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
    var records by remember { mutableStateOf<List<ChaoxingFaceRecognitionImage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var inspectedObjectId by remember { mutableStateOf<String?>(null) }
    var requestedDeleteObjectId by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        records = context.chaoxingDataStore.data.first()
            .faceRecognitionConfiguresMap[ChaoxingHttpClient.instance!!.userEntity.phoneNumber]
            ?.imagesList.orEmpty().take(ChaoxingFaceHelper.MAX_FACE_IMAGES)
    }

    fun delete(record: ChaoxingFaceRecognitionImage) {
        coroutineScope.launch {
            isLoading = true
            runCatching {
                ChaoxingFaceHelper.deleteFaceImage(
                    context,
                    ChaoxingHttpClient.instance!!.userEntity.phoneNumber,
                    record.objectId,
                )
                reload()
            }.onSuccess {
                snackbarHost.displaySnackbar("人脸照片删除成功", coroutineScope)
            }.onFailure {
                it.snackbarReport(snackbarHost, coroutineScope, "删除人脸照片失败", hapticFeedback)
            }
            isLoading = false
            requestedDeleteObjectId = null
        }
    }

    fun save(bitmap: Bitmap) {
        if (records.size >= ChaoxingFaceHelper.MAX_FACE_IMAGES) {
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

    inspectedObjectId?.let { objectId ->
        val record = records.firstOrNull { it.objectId == objectId }
        if (record != null) {
            AlertDialog(
                onDismissRequest = { inspectedObjectId = null },
                title = { Text("人脸照片详情") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = ChaoxingFaceHelper.getFaceImageFile(context, record.objectId),
                            contentDescription = "人脸识别照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f),
                        )
                        Text("图片ID: ${record.objectId}")
                        Text("使用次数: ${record.useCount}")
                        Text("此前是否人脸识别失败过: ${if (record.isFailureBefore) "是" else "否"}")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        inspectedObjectId = null
                        requestedDeleteObjectId = record.objectId
                    }) { Text("删除此照片") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        inspectedObjectId = null
                    }) { Text("关闭") }
                },
            )
        }
    }

    requestedDeleteObjectId?.let { objectId ->
        val record = records.firstOrNull { it.objectId == objectId }
        if (record != null) {
            AlertDialog(
                onDismissRequest = { requestedDeleteObjectId = null },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        null,
                        tint = Color.Red,
                        modifier = Modifier.size(40.dp),
                    )
                },
                title = { Text("删除照片") },
                text = {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCC307)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 6.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painterResource(R.drawable.ic_triangle_alert),
                                contentDescription = "Alert",
                                tint = Color.White,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "在此删除照片并不会在其他已经导入你的人脸识别照片的设备上一并删除。",
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.W500,
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        delete(record)
                    }) { Text("删除") }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        requestedDeleteObjectId = null
                    }) { Text("取消") }
                },
            )
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
                    } else if (records.isEmpty()) {
                        Text("暂无人脸照片，最多可保存 ${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item { Text("已保存 ${records.size}/${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张") }
                            items(records) { record ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.ContextClick
                                            )
                                            inspectedObjectId = record.objectId
                                        },
                                ) {
                                    AsyncImage(
                                        model = ChaoxingFaceHelper.getFaceImageFile(
                                            context,
                                            record.objectId
                                        ),
                                        contentDescription = "已上传的人脸照片",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(3f / 4f),
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
                        enabled = (!isLoading && records.size < ChaoxingFaceHelper.MAX_FACE_IMAGES),
                        onClick = {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("上传照片") }
                    SegmentedButton(
                        selected = false,
                        enabled = (!isLoading && records.size < ChaoxingFaceHelper.MAX_FACE_IMAGES),
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