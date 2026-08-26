/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.attafitamim.krop.core.crop.AspectRatio
import com.attafitamim.krop.core.crop.CropResult
import com.attafitamim.krop.core.crop.crop
import com.attafitamim.krop.core.crop.cropperStyle
import com.attafitamim.krop.core.crop.flipHorizontal
import com.attafitamim.krop.core.crop.flipVertical
import com.attafitamim.krop.core.crop.rememberImageCropper
import com.attafitamim.krop.ui.ButtonsBar
import com.attafitamim.krop.ui.ImageCropperDialog
import com.attafitamim.krop.ui.LocalVerticalControls
import com.attafitamim.krop.ui.isVerticalPickerControls
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClientPool
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingFaceRecognitionImage
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingFaceImageException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.decodePhotoBitmap
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacePhotoControlComponent(
    phoneNumber: String,
    onStartCamera: () -> Unit,
    modifier: Modifier = Modifier,
    pendingCapturedBitmap: Bitmap? = null,
    onPendingCapturedBitmapHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var records by remember(phoneNumber) {
        mutableStateOf<List<ChaoxingFaceRecognitionImage>>(emptyList())
    }
    var isLoading by remember(phoneNumber) { mutableStateOf(true) }
    var removingObjectIds by remember(phoneNumber) { mutableStateOf(emptySet<String>()) }
    var inspectedObjectId by remember { mutableStateOf<String?>(null) }
    var requestedDeleteObjectId by remember { mutableStateOf<String?>(null) }
    val deleteMutex = remember(phoneNumber) { Mutex() }

    suspend fun reload() {
        records = context.chaoxingDataStore.data.first()
            .faceRecognitionConfiguresMap[phoneNumber]
            ?.imagesList.orEmpty().take(ChaoxingFaceHelper.MAX_FACE_IMAGES)
    }

    fun delete(
        record: ChaoxingFaceRecognitionImage
    ) {
        if (record.objectId in removingObjectIds) return
        removingObjectIds = removingObjectIds + record.objectId
        requestedDeleteObjectId = null
        coroutineScope.launch {
            deleteMutex.withLock {
                runCatching {
                    ChaoxingFaceHelper.deleteFaceImage(
                        context,
                        phoneNumber,
                        record.objectId,
                    )
                    reload()
                }.onSuccess {
                    snackbarHost.displaySnackbar("人脸照片删除成功", coroutineScope)
                }.onFailure {
                    it.snackbarReport(
                        snackbarHost,
                        coroutineScope,
                        "删除人脸照片失败",
                        hapticFeedback,
                    )
                }
            }
            removingObjectIds = removingObjectIds - record.objectId
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
            runCatching {
                val client = ChaoxingHttpClientPool.get(context, phoneNumber)
                ChaoxingFaceHelper.saveFaceImage(
                    client,
                    context,
                    bitmap,
                    phoneNumber = phoneNumber,
                )
                reload()
            }.onSuccess {
                snackbarHost.displaySnackbar("人脸照片上传成功", coroutineScope)
            }.onFailure {
                it.snackbarReport(snackbarHost, coroutineScope, "上传人脸照片失败", hapticFeedback)
            }
        }
    }

    val imageCropper = rememberImageCropper()
    val facePhotoCropperStyle = remember {
        cropperStyle(aspects = listOf(AspectRatio(3, 4)))
    }

    fun cropAndSave(bitmap: Bitmap) {
        coroutineScope.launch {
            when (val result = imageCropper.crop(bitmap.asImageBitmap())) {
                is CropResult.Success -> save(result.bitmap.asAndroidBitmap())
                CropResult.Cancelled -> Unit
                else -> snackbarHost.displaySnackbar("裁剪人脸照片失败", coroutineScope)
            }
        }
    }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.decodePhotoBitmap(uri)
                        ?: throw ChaoxingFaceImageException("无法读取照片")
                }.onSuccess(::cropAndSave).onFailure {
                    it.snackbarReport(snackbarHost, coroutineScope, "读取照片失败", hapticFeedback)
                }
            }
        }

    LaunchedEffect(phoneNumber) {
        runCatching { reload() }.onFailure {
            it.snackbarReport(snackbarHost, coroutineScope, "加载人脸照片失败", hapticFeedback)
        }
        isLoading = false
    }

    LaunchedEffect(pendingCapturedBitmap) {
        if (pendingCapturedBitmap != null) {
            cropAndSave(pendingCapturedBitmap)
            onPendingCapturedBitmapHandled()
        }
    }

    imageCropper.cropState?.let { cropState ->
        BackHandler {
            cropState.done(accept = false)
        }
        ImageCropperDialog(
            state = cropState,
            style = facePhotoCropperStyle,
            cropControls = { state ->
                val verticalControls = isVerticalPickerControls()
                CompositionLocalProvider(LocalVerticalControls provides verticalControls) {
                    ButtonsBar(
                        modifier = Modifier
                            .align(if (!verticalControls) Alignment.BottomCenter else Alignment.CenterEnd)
                            .padding(12.dp)
                    ) {
                        IconButton(onClick = { state.flipHorizontal() }) {
                            Icon(painterResource(R.drawable.ic_flip_horizontal), null)
                        }
                        IconButton(onClick = { state.flipVertical() }) {
                            Icon(painterResource(R.drawable.ic_flip_vertical), null)
                        }
                    }
                }
            })
    }

    inspectedObjectId?.let { objectId ->
        val record = records.firstOrNull { it.objectId == objectId }
        if (record != null) {
            SnackbarAlertDialog(
                onDismissRequest = { inspectedObjectId = null },
                title = { Text("人脸照片详情") },
                text = { dialogSnackbarHost ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = remember(record) {
                                ChaoxingFaceHelper.getFaceImageFile(
                                    context,
                                    record.objectId
                                )
                            },
                            contentDescription = "人脸识别照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f),
                            onError = {
                                it.result.throwable.snackbarReport(
                                    dialogSnackbarHost,
                                    coroutineScope,
                                    "图片加载失败",
                                    hapticFeedback
                                )
                                it.result.throwable.printStackTrace()
                            }
                        )
                        Text("图片ID: ${record.objectId}")
                        Text("使用次数: ${record.useCount}")
                        Text(buildAnnotatedString {
                            append("此前是否人脸识别失败过: ")
                            if (record.isFailureBefore) {
                                withStyle(
                                    SpanStyle(
                                        color = Color.Red,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("是")
                                }
                            } else append("否")
                        })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
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
            SnackbarAlertDialog(
                onDismissRequest = { requestedDeleteObjectId = null },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        null,
                        tint = Color.Red,
                        modifier = Modifier.size(40.dp),
                    )
                },
                title = { Text("是否删除照片？") },
                text = {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(
                                0xFFFCC307
                            )
                        ),
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        if (isLoading) {
            CenterCircularProgressIndicator()
        } else if (records.isEmpty()) {
            Text(
                "暂无人脸照片，最多可保存 ${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张",
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                modifier = Modifier.padding(6.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("已保存 ${records.size}/${ChaoxingFaceHelper.MAX_FACE_IMAGES} 张")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(
                        items = records,
                        key = { it.objectId },
                    ) { displayedRecord ->
                        AnimatedVisibility(
                            visible = displayedRecord.objectId !in removingObjectIds,
                            enter = fadeIn(
                                animationSpec = tween(
                                    500,
                                    easing = LinearOutSlowInEasing,
                                ),
                            ),
                            exit = fadeOut(
                                animationSpec = tween(
                                    500,
                                    easing = LinearOutSlowInEasing,
                                ),
                            ),
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    inspectedObjectId = displayedRecord.objectId
                                },
                            ) {
                                AsyncImage(
                                    model = remember(displayedRecord.objectId) {
                                        ChaoxingFaceHelper.getFaceImageFile(
                                            context,
                                            displayedRecord.objectId
                                        )
                                    },
                                    contentDescription = "已保存的人脸照片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .aspectRatio(3f / 4f),
                                    onError = {
                                        it.result.throwable.snackbarReport(
                                            snackbarHost,
                                            coroutineScope,
                                            "加载人脸照片失败",
                                            hapticFeedback
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        val isPhotoReachLimited by remember { derivedStateOf { records.size >= ChaoxingFaceHelper.MAX_FACE_IMAGES } }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = false,
                enabled = !isPhotoReachLimited,
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text("上传照片") }
            SegmentedButton(
                selected = false,
                enabled = !isPhotoReachLimited,
                onClick = onStartCamera,
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text("拍摄照片") }
        }
        if (isPhotoReachLimited) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_info),
                    null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 5.dp)
                )
                Text(
                    "目前存储的人脸识别照片最多为${ChaoxingFaceHelper.MAX_FACE_IMAGES}张，无法继续添加。",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
