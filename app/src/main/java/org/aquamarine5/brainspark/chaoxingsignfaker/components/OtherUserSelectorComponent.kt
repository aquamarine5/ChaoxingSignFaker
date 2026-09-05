/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.content.ClipboardManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.OtherUserTagType
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.TAG_COLOR_UNSPECIFIED
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.OnlyAppDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import java.io.File

@Composable
fun OtherUserSelectorComponent(
    navToOtherUser: () -> Unit,
    signStatus: MutableList<ChaoxingSignStatus>,
    isCloneSession: Boolean,
    isCurrentAlreadySigned: Boolean,
    isSigning: MutableState<Boolean>,
    userSelections: SnapshotStateList<Boolean>,
    onRetrySignAction: suspend (index: Int, session: ChaoxingOtherUserSession, bypassChecking: Boolean) -> Unit,
    userContent: @Composable ((index: Int) -> Unit)? = null,
    prefixTipsContent: @Composable (() -> Unit),
    suffixContent: @Composable (() -> Unit)? = null,
    faceRecognitionData: FaceRecognitionData? = null,
    onSignAction: (isSelf: Boolean, otherUserSessionList: List<ChaoxingOtherUserSession?>, indexList: List<Int>) -> Unit
) {
    LocalContext.current.let { context ->
        val signUserList = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
        val hapticFeedback = LocalHapticFeedback.current
        val snackbarHost = LocalSnackbarHostState.current
        val coroutineScope = rememberCoroutineScope()
        var tagEntities by remember { mutableStateOf<List<OtherUserTagType>?>(null) }
        var tagContainedUserIndexList by remember { mutableStateOf<List<List<Int>>?>(null) }
        val tagClickState = remember { mutableListOf<MutableState<Boolean>>() }
        var selfPhoneNumber by remember { mutableStateOf<String?>(null) }
        var success by signStatus[0].isSuccess
        var ignoreExceptionUserIndex by remember {
            mutableStateOf<Pair<Int, ChaoxingOtherUserSession>?>(
                null
            )
        }
        var repairSessionIndex by remember { mutableStateOf<Int?>(null) }

        @OnlyAppDevelopedMode
        var inspectingFaceImagePhoneNumber by remember { mutableStateOf<String?>(null) }
        var inspectedFailedFaceImagePhoneNumber by remember { mutableStateOf<String?>(null) }

        @OnlyAppDevelopedMode
        var inspectedFaceImage by remember { mutableStateOf<Any?>(null) }

        @OnlyAppDevelopedMode
        var isSavingFaceImage by remember { mutableStateOf(false) }
        val imageLoader = LocalImageLoader.current
        val allSelected by remember(isCurrentAlreadySigned) {
            derivedStateOf {
                userSelections.subList(1, userSelections.size)
                    .all { it } && (isCurrentAlreadySigned || userSelections[0])
            }
        }
        if (repairSessionIndex != null) {
            SnackbarAlertDialog(onDismissRequest = {
                repairSessionIndex = null
            }, title = {
                Text("修复用户 ${signUserList[repairSessionIndex!!].name} 的登录状态")
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_wrench),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, text = {
                Column {
                    Text("在最近一次的签到过程中检测到用户 ${signUserList[repairSessionIndex!!].name} 的登录状态异常，重新登录后可修复此问题。")
                    var password by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = signUserList[repairSessionIndex!!].phoneNumber,
                        onValueChange = { },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        label = { Text("手机号") }
                    )
                    var isPasswordVisible by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    val clip =
                                        context.getSystemService(ClipboardManager::class.java)?.primaryClip
                                    val result = if (clip != null && clip.itemCount > 0) {
                                        clip.getItemAt(0).text
                                    } else {
                                        null
                                    }
                                    if (result.isNullOrEmpty()) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                        Toast.makeText(
                                            context,
                                            "读取剪切板失败",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                        password = result.toString()
                                        isPasswordVisible = true
                                    }
                                }) {
                                    Icon(painterResource(R.drawable.ic_clipboard_copy), null)
                                }
                                IconButton(onClick = {
                                    isPasswordVisible = !isPasswordVisible
                                }) {
                                    Icon(
                                        if (isPasswordVisible) painterResource(R.drawable.ic_eye) else painterResource(
                                            R.drawable.ic_eye_closed
                                        ), null
                                    )
                                }
                            }
                        }
                    )
                    if (errorMessage.isNotBlank())
                        Text(
                            errorMessage,
                            color = Color(0xFFF1441D),
                            modifier = Modifier.padding(0.dp, 4.dp)
                        )
                    Button(onClick = {
                        coroutineScope.launch {
                            val sessionIndex = repairSessionIndex ?: return@launch
                            val sessionToRepair = signUserList[sessionIndex]
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    ChaoxingOtherUserHelper.repairOtherUserSession(
                                        context,
                                        sessionToRepair,
                                        password
                                    )
                                }
                            }.onSuccess { repairedSession ->
                                signUserList[sessionIndex] = repairedSession
                                signStatus[sessionIndex].isObsoleteSession.value = false
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                snackbarHost.displaySnackbar(
                                    "用户 ${repairedSession.name} 已成功修复",
                                    coroutineScope
                                )
                                repairSessionIndex = null
                            }.onFailure {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                if (it !is ChaoxingPredictableException) {
                                    Sentry.captureException(it)
                                }
                                errorMessage = "登录失败：" + (it.message ?: "未知错误")
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("重新登录") }
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    repairSessionIndex = null
                }) {
                    Text("关闭")
                }
            })
        }

        if (ignoreExceptionUserIndex != null) {
            SnackbarAlertDialog(onDismissRequest = {
                ignoreExceptionUserIndex = null
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_refresh_rounded),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, text = {
                Text(buildAnnotatedString {
                    append("随地大小签在签到前的检测中判断此用户不在签到班级或已签到过，因此拒绝了此次签到操作，")
                    withStyle(SpanStyle(color = Color.Red)) {
                        append("此时直接重试签到依然会被同样的检测拒绝")
                    }
                    append("。\n如果你认为随地大小签的判断存在问题，请点击【强制重试签到】按钮，随地大小签会忽略所有应用内的判断条件，直接进行签到，")
                    withStyle(SpanStyle(color = Color.Red)) {
                        append("但这会导致老师的已签名单中出现不在这个班级的学生，或产生重复的签到记录")
                    }
                    append("。")
                })
            }, confirmButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    ignoreExceptionUserIndex = null
                }) {
                    Text("关闭")
                }
            }, dismissButton = {
                var isIgnoreExceptionSigning by remember { mutableStateOf(false) }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch {
                        isIgnoreExceptionSigning = true
                        val data = ignoreExceptionUserIndex!!
                        ignoreExceptionUserIndex = null
                        onRetrySignAction(
                            data.first,
                            data.second,
                            true
                        )
                        isIgnoreExceptionSigning = false
                    }
                }, enabled = isIgnoreExceptionSigning.not()) {
                    Text("强制重试签到")
                }
            })
        }

        @OnlyAppDevelopedMode if (isDevelopedMode && faceRecognitionData != null && inspectingFaceImagePhoneNumber != null) {
            val phoneNumber = inspectingFaceImagePhoneNumber!!
            SnackbarAlertDialog(onDismissRequest = {
                inspectingFaceImagePhoneNumber = null
            }, title = {
                Text("用户 $phoneNumber 的人脸识别照片")
            }, text = { localSnackbarHost ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        ChaoxingFaceHelper.storedFaceRecognitionImages.peekValue()
                            ?.get(phoneNumber).orEmpty()
                            .map { ChaoxingFaceHelper.getFaceImageFile(context, it.objectId) }
                    ) { image ->
                        AsyncImage(
                            model = image,
                            imageLoader = imageLoader,
                            contentDescription = "人脸识别照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(110.dp)
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(5.dp))
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    inspectedFaceImage = image
                                },
                            onError = {
                                it.result.throwable.snackbarReport(
                                    localSnackbarHost,
                                    coroutineScope,
                                    "人脸识别照片加载失败",
                                    hapticFeedback
                                )
                            }
                        )
                    }
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    inspectingFaceImagePhoneNumber = null
                }) { Text("关闭") }
            })
        }

        inspectedFaceImage?.let { image ->
            val objectId = when (image) {
                is File -> image.nameWithoutExtension
                is String -> image.substringBefore("/origin").substringAfterLast("/")
                else -> image.toString()
            }
            SnackbarAlertDialog(onDismissRequest = {
                inspectedFaceImage = null
            }, title = {
                Text("人脸照片详情")
            }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(
                        model = image,
                        imageLoader = imageLoader,
                        contentDescription = "人脸识别照片大图",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                    )
                    Text("图片ID: $objectId")
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    inspectedFaceImage = null
                }) { Text("关闭") }
            }, dismissButton = {
                OutlinedButton(
                    enabled = isSavingFaceImage.not(),
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        coroutineScope.launch {
                            isSavingFaceImage = true
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    val bitmap: Bitmap = when (image) {
                                        is File -> BitmapFactory.decodeFile(image.absolutePath)
                                            ?: throw IllegalStateException("读取人脸照片失败")

                                        is String -> {
                                            val result = imageLoader.execute(
                                                ImageRequest.Builder(context).data(image).build()
                                            )
                                            (result as? SuccessResult)?.image?.toBitmap()
                                                ?: ChaoxingHttpClient.instance!!.newCall(
                                                    Request.Builder().url(image).build()
                                                ).execute().use { response ->
                                                    response.body.byteStream().use { stream ->
                                                        BitmapFactory.decodeStream(stream)
                                                            ?: throw IllegalStateException("读取人脸照片失败")
                                                    }
                                                }
                                        }

                                        else -> throw IllegalStateException("读取人脸照片失败")
                                    }
                                    val values = ContentValues().apply {
                                        put(
                                            MediaStore.Images.Media.DISPLAY_NAME,
                                            "face_$objectId.jpg"
                                        )
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            put(
                                                MediaStore.Images.Media.RELATIVE_PATH,
                                                Environment.DIRECTORY_PICTURES
                                            )
                                            put(MediaStore.Images.Media.IS_PENDING, 1)
                                        }
                                    }
                                    val collection =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                            MediaStore.Images.Media.getContentUri(
                                                MediaStore.VOLUME_EXTERNAL_PRIMARY
                                            )
                                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    val uri = context.contentResolver.insert(collection, values)
                                        ?: throw IllegalStateException("创建图片文件失败")
                                    context.contentResolver.openOutputStream(uri)?.use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        values.clear()
                                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                                        context.contentResolver.update(uri, values, null, null)
                                    }
                                }
                            }.onSuccess {
                                snackbarHost.displaySnackbar("人脸照片已保存到相册", coroutineScope)
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "保存人脸照片失败",
                                    hapticFeedback
                                )
                            }
                            isSavingFaceImage = false
                        }
                    }
                ) { Text("保存") }
            })
        }

        if (faceRecognitionData != null) {
            inspectedFailedFaceImagePhoneNumber?.let { phoneNumber ->
                val info = faceRecognitionData.failedImageInfos[phoneNumber]
                val objectId = info?.first
                SnackbarAlertDialog(onDismissRequest = {
                    inspectedFailedFaceImagePhoneNumber = null
                }, title = {
                    Text("本次签到的人脸照片")
                }, text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = info?.second
                                ?: objectId?.let {
                                    ChaoxingFaceHelper.getFaceImageFile(
                                        context,
                                        it
                                    )
                                },
                            imageLoader = imageLoader,
                            contentDescription = "本次签到人脸照片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                        )
                        Text("图片ID: ${objectId ?: "无"}")
                        val record = objectId?.let { id ->
                            ChaoxingFaceHelper.storedFaceRecognitionImages.peekValue()
                                ?.get(phoneNumber)?.firstOrNull { it.objectId == id }
                        }
                        Text("此前是否人脸识别失败过: ${if (record?.isFailureBefore == true) "是" else "否"}")
                        Text("使用次数: ${record?.useCount ?: 0}")
                    }
                }, confirmButton = {
                    Button(onClick = {
                        inspectedFailedFaceImagePhoneNumber = null
                    }) { Text("关闭") }
                })
            }
        }

        fun updateTagClickState() {
            tagContainedUserIndexList?.forEachIndexed { tagIndex, userIndexList ->
                if (userIndexList.isNotEmpty()) {
                    val allChecked = userIndexList.all { userIndex ->
                        userSelections[userIndex + 1]
                    }
                    tagClickState[tagIndex].value = allChecked
                }
            }
        }

        var isDatastoreLoadReady by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val datastore = context.chaoxingDataStore.data.first()
                tagEntities = datastore.tagsLibraryList
                tagClickState.addAll(List(datastore.tagsLibraryList.size) {
                    mutableStateOf(
                        false
                    )
                })
                selfPhoneNumber = datastore.loginSession.phoneNumber
                tagContainedUserIndexList = datastore.tagsLibraryList.map { tagEntity ->
                    buildList {
                        datastore.otherUsersList.mapIndexed { index, otherUserSession ->
                            if (otherUserSession.tagsList.any { it == tagEntity.id })
                                add(index)
                        }
                    }
                }
                val data = datastore.otherUsersList.filter {
                    it.phoneNumber != datastore.loginSession.phoneNumber
                }
                if (faceRecognitionData?.imageIconList?.value?.isEmpty() == true) {
                    faceRecognitionData.imageIconList.value = buildList {
                        add(
                            if (datastore.faceRecognitionConfiguresMap[datastore.loginSession.phoneNumber]?.imagesList?.isNotEmpty() == true)
                                mutableStateOf(FaceRecognitionImageStatus.HaveImage)
                            else mutableStateOf(FaceRecognitionImageStatus.NoImage)
                        )
                        data.forEach {
                            add(
                                if (datastore.faceRecognitionConfiguresMap[it.phoneNumber]?.imagesList?.isNotEmpty() == true)
                                    mutableStateOf(FaceRecognitionImageStatus.HaveImage)
                                else mutableStateOf(FaceRecognitionImageStatus.NoImage)
                            )
                        }
                    }
                }
                signStatus.addAll(Array(data.size) {
                    ChaoxingSignStatus(hapticFeedback)
                })
                userSelections.addAll(List(data.size) { false })
                signUserList.addAll(data.let { sessions ->
                    if (isCloneSession) {
                        sessions.sortedBy { it.phoneNumber != ChaoxingHttpClient.cloneInstance!!.userEntity.phoneNumber }
                    } else {
                        sessions
                    }
                })
            }
            isDatastoreLoadReady = true
            success = isCurrentAlreadySigned
            userSelections[0] = isCurrentAlreadySigned != true
        }
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val gapPx = remember { with(density) { 80.dp.toPx() } }
        val showFab by remember {
            derivedStateOf {
                scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - gapPx
            }
        }

        fun performSign() {
            if (!userSelections.any { it }) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                snackbarHost.displaySnackbar("请选择要签到的用户", coroutineScope)
                return
            }
            if (signStatus.all { it.isSuccess.value == true }) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                snackbarHost.displaySnackbar("所有用户均已签到", coroutineScope)
                return
            }
            val indexList = mutableListOf<Int>()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            val isSelf = userSelections[0] && signStatus[0].isSuccess.value != true
            if (isSelf)
                indexList.add(0)
            val otherUserSessionList =
                signUserList.mapIndexed { index, chaoxingOtherUserSession ->
                    if (userSelections[index + 1] && signStatus[1 + index].isSuccess.value != true) {
                        indexList.add(index + 1)
                        chaoxingOtherUserSession
                    } else {
                        null
                    }
                }
            onSignAction(
                isSelf, otherUserSessionList, indexList
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp, 0.dp)
                    .verticalScroll(scrollState)
            ) {
                prefixTipsContent()

                AnimatedVisibility(
                    isDatastoreLoadReady && signUserList.size < 2,
                    enter = slideInVertically(tween(300), initialOffsetY = { -it }) + fadeIn(
                        tween(
                            300
                        )
                    ),
                    exit = slideOutVertically(tween(300), targetOffsetY = { it }) + fadeOut(
                        tween(
                            300
                        )
                    )
                ) {
                    Card(
                        onClick = {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.ContextClick
                            )
                            navToOtherUser()
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10AEC2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(10.dp, 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(10.5.dp))
                            Icon(
                                painterResource(R.drawable.ic_lightbulb),
                                contentDescription = "Help",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.5.dp))
                            Text(
                                "如果你想给其他用户签到但还没有添加其他用户，可以点击此跳转至添加用户向导。",
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.W500,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }

                suffixContent?.invoke()
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "选择要进行签到的用户：",
                    modifier = Modifier.padding(start = 3.dp),
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 4.dp, 8.dp, 16.dp)
                ) {
                    Row {
                        if (tagEntities != null && tagContainedUserIndexList != null)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                FilterChip(
                                    selected = allSelected,
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.ContextClick
                                        )
                                        val target = !allSelected
                                        if (!isCurrentAlreadySigned) {
                                            userSelections[0] = target
                                        }
                                        for (i in 1 until userSelections.size) {
                                            userSelections[i] = target
                                        }
                                        updateTagClickState()
                                    },
                                    label = {
                                        Text("全选")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_list_checks),
                                            null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    border = BorderStroke(1.5.dp, Color.Gray)
                                )
                                if (tagEntities!!.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AssistChip(onClick = {
                                            navToOtherUser()
                                        }, label = {
                                            Text("点击跳转添加标签...")
                                        }, leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_tag_plus_outline),
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.Gray
                                            )
                                        }, border = BorderStroke(1.5.dp, Color.Gray))
                                    }
                                } else {
                                    tagEntities!!.forEachIndexed { index, type ->
                                        FilterChip(
                                            selected = tagClickState[index].value,
                                            onClick = {
                                                tagClickState[index].value =
                                                    !tagClickState[index].value
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                tagContainedUserIndexList!![index].forEach { userIndex ->
                                                    if (userIndex + 1 < userSelections.size)
                                                        userSelections[userIndex + 1] =
                                                            tagClickState[index].value
                                                }
                                            },
                                            label = {
                                                Text(type.name)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painterResource(R.drawable.ic_tag),
                                                    null,
                                                    tint = if (type.color == TAG_COLOR_UNSPECIFIED) {
                                                        if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                                    } else {
                                                        Color(type.color)
                                                    }, modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            border = BorderStroke(1.5.dp, Color.Gray)
                                        )
                                    }
                                }
                            }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = userSelections[0] && signStatus[0].isSuccess.value != true,
                            onCheckedChange = { isChecked ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                userSelections[0] = isChecked
                            },
                            enabled = (success == true).not()
                        )
                        Row(modifier = Modifier.clickable((success == true).not()) {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.ContextClick
                            )
                            userSelections[0] = userSelections[0].not()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "给自己签到",
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = if (success != true) TextDecoration.None else TextDecoration.LineThrough
                                    )
                                    faceRecognitionData?.imageIconList?.value?.getOrNull(0)
                                        ?.let {
                                            Icon(
                                                painterResource(it.value.resId),
                                                null,
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .size(14.dp)
                                                    .then(
                                                        if (isDevelopedMode) Modifier.clickable {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.ContextClick
                                                            )
                                                            inspectingFaceImagePhoneNumber =
                                                                ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                                        } else Modifier
                                                    ),
                                                tint = it.value.color.takeOrElse { MaterialTheme.colorScheme.primary }
                                            )
                                        }
                                    if (faceRecognitionData != null) {
                                        val selfPhone =
                                            ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                        if (selfPhone in faceRecognitionData.failedPhoneNumbers) {
                                            Icon(
                                                painterResource(R.drawable.ic_user_square),
                                                contentDescription = "查看本次签到照片",
                                                tint = Color(0xFFF43E06),
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .size(14.dp)
                                                    .clickable {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.ContextClick
                                                        )
                                                        inspectedFailedFaceImagePhoneNumber =
                                                            selfPhone
                                                    }
                                            )
                                        }
                                    }
                                    if (signStatus[0].isCaptchaResolvedByModel.value) {
                                        Icon(
                                            painterResource(R.drawable.ic_brain_circuit),
                                            contentDescription = "验证码由模型自动识别",
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    "${ChaoxingHttpClient.instance?.userEntity?.name} ($selfPhoneNumber)",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                userContent?.invoke(0)
                                signStatus[0].ResultCard()
                            }
                        }
                    }
                    signUserList.forEachIndexed { index, session ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                        (1 + index).let { i ->
                            val successForOtherUser by signStatus[i].isSuccess
                            var isRetrying by remember { mutableStateOf(false) }
                                Checkbox(
                                    checked = userSelections[i] && signStatus[i].isSuccess.value != true,
                                    onCheckedChange = { isChecked ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        userSelections[i] = isChecked
                                        updateTagClickState()
                                    },
                                    enabled = (successForOtherUser == true).not()
                                )
                                Row(modifier = Modifier.clickable((successForOtherUser == true).not()) {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    userSelections[i] = userSelections[i].not()
                                    updateTagClickState()
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = session.name,
                                                color = if (session.isObsoleteSession || signStatus[i].isObsoleteSession.value) Color(
                                                    0xFFFCC307
                                                ) else Color.Unspecified,
                                                textDecoration = if (successForOtherUser != true) TextDecoration.None else TextDecoration.LineThrough
                                            )
                                            if (isCloneSession)
                                                Icon(
                                                    painterResource(R.drawable.ic_square_stack),
                                                    null,
                                                    tint = LocalContentColor.current,
                                                    modifier = Modifier
                                                        .padding(start = 4.dp)
                                                        .size(16.dp)
                                                )
                                            faceRecognitionData?.imageIconList?.value?.getOrNull(i)
                                                ?.let {
                                                    Icon(
                                                        painterResource(it.value.resId),
                                                        null,
                                                        modifier = Modifier
                                                            .padding(start = 4.dp)
                                                            .size(14.dp)
                                                            .then(
                                                                if (isDevelopedMode) Modifier.clickable {
                                                                    hapticFeedback.performHapticFeedback(
                                                                        HapticFeedbackType.ContextClick
                                                                    )
                                                                    inspectingFaceImagePhoneNumber =
                                                                        session.phoneNumber
                                                                } else Modifier
                                                            ),
                                                        tint = it.value.color.takeOrElse { MaterialTheme.colorScheme.primary }
                                                    )
                                                }
                                            if (signStatus[i].isCaptchaResolvedByModel.value) {
                                                Icon(
                                                    painterResource(R.drawable.ic_brain_circuit),
                                                    contentDescription = "验证码由模型自动识别",
                                                    modifier = Modifier
                                                        .padding(start = 4.dp)
                                                        .size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (faceRecognitionData != null && session.phoneNumber in faceRecognitionData.failedPhoneNumbers) {
                                            Icon(
                                                painterResource(R.drawable.ic_user_square),
                                                contentDescription = "查看本次签到照片",
                                                tint = Color(0xFFF43E06),
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .size(14.dp)
                                                    .clickable {
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.ContextClick
                                                        )
                                                        inspectedFailedFaceImagePhoneNumber =
                                                            session.phoneNumber
                                                    }
                                            )
                                        }
                                        Text(
                                            session.phoneNumber,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (session.isObsoleteSession || signStatus[i].isObsoleteSession.value)
                                            IconButton(onClick = {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                repairSessionIndex = index
                                            }) {
                                                Icon(
                                                    painterResource(R.drawable.ic_triangle_alert),
                                                    null,
                                                    tint = Color(0xFFFCC307),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                )
                                            }
                                        userContent?.invoke(1 + index)
                                        signStatus[i].ResultCard {
                                            if (signStatus[i].isBypassCheckingRequired) {
                                                ignoreExceptionUserIndex = index to session
                                            } else if (isRetrying.not()) {
                                                isRetrying = true
                                                coroutineScope.launch {
                                                    onRetrySignAction(index, session, false)
                                                    isRetrying = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { performSign() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isSigning.value.not()
                ) {
                    Text("签到")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isSigning.value.not())
                            performSign()
                    },
                    containerColor = if (isSigning.value) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        FloatingActionButtonDefaults.containerColor
                    },
                    contentColor = if (isSigning.value) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.alpha(if (isSigning.value) 0.6f else 1f)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_clipboard_pen_line),
                        contentDescription = "签到",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
