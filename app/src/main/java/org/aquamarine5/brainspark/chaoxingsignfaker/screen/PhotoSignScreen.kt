/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CameraComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.handleReport
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class PhotoSignDestination(
    val activeId: Long, val classId: Int, val courseId: Int, val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): PhotoSignDestination {
            return PhotoSignDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoSignScreen(
    destination: PhotoSignDestination, navBack: () -> Unit, navToOtherUserDestination: () -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp, 0.dp)
    ) {
        val snackbarHost = LocalSnackbarHostState.current
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val signer = ChaoxingPhotoSigner(ChaoxingHttpClient.instance!!, destination)
        var isImage by remember { mutableStateOf<Boolean?>(null) }
        var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
        var isSignSuccess by remember { mutableStateOf(false) }
        var isShowPhotoPicker by remember { mutableStateOf(false) }
        var isForSelf by remember { mutableStateOf(false) }
        var isSponsor by remember { mutableStateOf(false) }
        if (isSponsor) {
            SponsorPopupDialog()
        }
        var captchaValidateParams by remember {
            mutableStateOf<Pair<ChaoxingPhotoSigner, (Result<String>) -> Unit>?>(null)
        }
        if (captchaValidateParams != null) {
            CaptchaHandlerDialog(captchaValidateParams!!.first,
                captchaValidateParams!!.second,
                onDismiss = {
                    captchaValidateParams = null
                })
        }
        LaunchedEffect(Unit) {
            runCatching {
                isImage = signer.ifPhotoRequiredLogin()
                isAlreadySigned = signer.preSign()
            }.onFailure {
                if ((it is ChaoxingPredictableException).not()) {
                    Sentry.captureException(it)
                }
                Toast.makeText(context, "获取签到事件详情失败", Toast.LENGTH_SHORT).show()
                navBack()
            }
        }
        Crossfade(isAlreadySigned) { v ->
            when (v) {
                false -> {
                    if (isImage == false) {
                        Column {
                            Column(modifier = Modifier.padding(16.dp, 0.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, 10.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(18.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        painterResource(R.drawable.ic_info),
                                        contentDescription = "Info",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "这是一个普通的点击签到，不会收集任何其他的信息，所以我们推荐对于这种签到使用学习通APP而不是随地大小签。",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.W500
                                    )
                                }
                                OutlinedButton(onClick = {
                                    context.startActivity(Intent(
                                        Intent.ACTION_VIEW, Uri.parse("cxstudy://")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("跳转到学习通")
                                }
                            }
                            var isSigning by remember { mutableStateOf(false) }
                            val signStatus = remember { mutableListOf(ChaoxingSignStatus()) }
                            OtherUserSelectorComponent(
                                navToOtherUser = {
                                    navToOtherUserDestination()
                                },
                                signStatus,
                                isForSelf,
                                isSigning
                            ) { isSelf, otherUserSessionList, _ ->
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (isSelf) runCatching {
                                            signStatus[0].loading()
                                            suspendCoroutine { continuation ->
                                                runBlocking {
                                                    signer.signByClick {
                                                        captchaValidateParams =
                                                            signer to { validateValue ->
                                                                runBlocking {
                                                                    validateValue.onSuccess {
                                                                        runCatching {
                                                                            signer.signByClickWithCaptcha(
                                                                                validateValue.getOrThrow()
                                                                            )
                                                                        }.onSuccess {
                                                                            UMengHelper.onSignClickEvent(
                                                                                context,
                                                                                ChaoxingHttpClient.instance!!.userEntity.name
                                                                            )
                                                                            signStatus[0].success()
                                                                            if (otherUserSessionList.isEmpty()) {
                                                                                isSponsor = true
                                                                            }
                                                                        }.onFailure {
                                                                            it.snackbarReport(
                                                                                snackbarHost,
                                                                                coroutineScope,
                                                                                "签到失败"
                                                                            )
                                                                            signStatus[0].failed(it)
                                                                        }
                                                                        continuation.resume(Unit)
                                                                    }.onFailure {
                                                                        it.snackbarReport(
                                                                            snackbarHost,
                                                                            coroutineScope,
                                                                            "验证码校验失败"
                                                                        )
                                                                        signStatus[0].failed(it)
                                                                        continuation.resume(Unit)
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        }.onFailure {
                                            it.snackbarReport(
                                                snackbarHost,
                                                coroutineScope,
                                                "签到失败"
                                            )
                                            signStatus[0].failed(it)
                                        }.onSuccess {
                                            if (captchaValidateParams != null) return@onSuccess
                                            UMengHelper.onSignClickEvent(
                                                context,
                                                ChaoxingHttpClient.instance!!.userEntity.name
                                            )
                                            signStatus[0].success()
                                            if (otherUserSessionList.isEmpty()) {
                                                isSponsor = true
                                            }
                                        }
                                        otherUserSessionList.forEachIndexed { index, userSession ->
                                            if (userSession == null) return@forEachIndexed
                                            runCatching {
                                                signStatus[1 + index].loading()
                                                ChaoxingHttpClient.loadFromOtherUserSession(
                                                    userSession, context
                                                ).also { client ->
                                                    ChaoxingPhotoSigner(
                                                        client, destination
                                                    ).apply {
                                                        if (preSign()) {
                                                            signStatus[index + 1].failed(
                                                                ChaoxingSigner.AlreadySignedException()
                                                            )
                                                        } else if (ifPhotoRequiredLogin()) {
                                                            signStatus[index + 1].failed(
                                                                ChaoxingPhotoSigner.ChaoxingIncorrectSignTypeException()
                                                            )
                                                        } else {
                                                            suspendCoroutine { continuation ->
                                                                runBlocking {
                                                                    signByClick {
                                                                        captchaValidateParams =
                                                                            this@apply to { validateValue ->
                                                                                validateValue.onSuccess {
                                                                                    runBlocking {
                                                                                        runCatching {
                                                                                            this@apply.signByClickWithCaptcha(
                                                                                                validateValue.getOrThrow()
                                                                                            )
                                                                                        }.onSuccess {
                                                                                            UMengHelper.onSignClickEvent(
                                                                                                context,
                                                                                                userSession.name,
                                                                                                isOtherUser = true
                                                                                            )
                                                                                            signStatus[1 + index].success()
                                                                                            if (index == otherUserSessionList.size - 1) {
                                                                                                isSponsor =
                                                                                                    true
                                                                                            }
                                                                                        }
                                                                                            .onFailure {
                                                                                                it.snackbarReport(
                                                                                                    snackbarHost,
                                                                                                    coroutineScope,
                                                                                                    "签到失败"
                                                                                                )
                                                                                                signStatus[1 + index].failed(
                                                                                                    it
                                                                                                )
                                                                                            }
                                                                                        continuation.resume(
                                                                                            Unit
                                                                                        )
                                                                                    }
                                                                                }.onFailure {
                                                                                    it.snackbarReport(
                                                                                        snackbarHost,
                                                                                        coroutineScope,
                                                                                        "验证码校验失败"
                                                                                    )
                                                                                    signStatus[1 + index].failed(
                                                                                        it
                                                                                    )
                                                                                    continuation.resume(
                                                                                        Unit
                                                                                    )
                                                                                }
                                                                            }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }.onFailure {
                                                it.snackbarReport(
                                                    snackbarHost,
                                                    coroutineScope,
                                                    "签到失败"
                                                )
                                                signStatus[1 + index].failed(it)
                                            }.onSuccess {
                                                if (captchaValidateParams != null) return@onSuccess
                                                UMengHelper.onSignClickEvent(
                                                    context, userSession.name, isOtherUser = true
                                                )
                                                signStatus[1 + index].success()
                                                if (index == otherUserSessionList.size - 1) {
                                                    isSponsor = true
                                                }
                                            }
                                            if (index != otherUserSessionList.size - 1) {
                                                signStatus[2+index].loading()
                                                delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isImage == true) {
                        var isSignForOther by remember { mutableStateOf<Boolean?>(null) }
                        Crossfade(isSignForOther) { value ->
                            when (value) {
                                null -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(painterResource(R.drawable.ic_image_up), null)
                                        Text("这是一个图片签到")
                                        Button(
                                            onClick = {
                                                isSignForOther = false
                                            },
                                            enabled = isForSelf.not(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("为自己签到（从图库读取图片）") }
                                        Button(
                                            onClick = {
                                                isSignForOther = true
                                            }, modifier = Modifier.fillMaxWidth()
                                        ) { Text("为他人代签（自己拍摄多张图片上传）") }
                                    }
                                }

                                true -> {
                                    val signStatus =
                                        remember { mutableListOf(ChaoxingSignStatus()) }
                                    var isCamera by remember { mutableStateOf(false) }
                                    var isSigning by remember { mutableStateOf(false) }
                                    var isSelfForSign by remember { mutableStateOf(false) }
                                    var bitmapList by remember {
                                        mutableStateOf<List<Bitmap>>(
                                            emptyList()
                                        )
                                    }
                                    val otherUserSessionForSignList =
                                        remember {
                                            mutableStateListOf<ChaoxingOtherUserSession?>()
                                        }
                                    var bitmapIndexList by remember {
                                        mutableStateOf<List<Int>>(
                                            emptyList()
                                        )
                                    }
                                    BackHandler(isSignForOther == true && !isCamera) {
                                        isSignForOther = null
                                    }
                                    Box(
                                        modifier = Modifier
                                            .zIndex(0f)
                                            .fillMaxSize()
                                    ) {
                                        Column {
                                            OtherUserSelectorComponent(
                                                navToOtherUser = {
                                                    navToOtherUserDestination()
                                                },
                                                signStatus,
                                                isForSelf,
                                                isSigning,
                                                userContent = { index ->
                                                    var isShowDialog by remember {
                                                        mutableStateOf(
                                                            false
                                                        )
                                                    }
                                                    bitmapIndexList.indexOf(index).let {
                                                        if (it != -1 && bitmapList.size > it) {
                                                            IconButton(onClick = {
                                                                isShowDialog = true
                                                            }) {
                                                                Icon(
                                                                    painterResource(R.drawable.ic_image),
                                                                    null
                                                                )
                                                            }
                                                            if (isShowDialog) AlertDialog(
                                                                onDismissRequest = {
                                                                    isShowDialog = false
                                                                },
                                                                confirmButton = {
                                                                    Button(onClick = {
                                                                        isShowDialog = false
                                                                    }) {
                                                                        Text("关闭")
                                                                    }
                                                                },
                                                                text = {
                                                                    Image(
                                                                        bitmapList[it].asImageBitmap(),
                                                                        null,
                                                                        modifier = Modifier
                                                                            .fillMaxHeight(
                                                                                0.5f
                                                                            )
                                                                            .padding(4.dp, 0.dp)
                                                                    )
                                                                })
                                                        }
                                                    }
                                                }) { isSelf, otherUserSessionList, indexList ->
                                                isSelfForSign = isSelf
                                                isSigning = true
                                                otherUserSessionForSignList.clear()
                                                otherUserSessionForSignList.addAll(
                                                    otherUserSessionList
                                                )
                                                bitmapIndexList = indexList
                                                isCamera = true
                                            }
                                        }
                                        Column(
                                            modifier = Modifier
                                                .zIndex(1f)
                                                .fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            AnimatedVisibility(
                                                isCamera, enter = slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = tween(300)
                                                ) + fadeIn(
                                                    animationSpec = tween(300)
                                                ), exit = scaleOut(
                                                    targetScale = 0.8f, animationSpec = tween(300)
                                                ) + fadeOut(
                                                    animationSpec = tween(300)
                                                )
                                            ) {
                                                BackHandler(isCamera) {
                                                    isCamera = false
                                                }
                                                val combinedUserList = if (isSelfForSign) {
                                                    listOf(
                                                        ChaoxingHttpClient.instance!!.userEntity.name,
                                                    ) + otherUserSessionForSignList.filterNotNull()
                                                        .map { it.name }
                                                } else {
                                                    otherUserSessionForSignList.filterNotNull()
                                                        .map { it.name }
                                                }
                                                var index by remember { mutableIntStateOf(0) }
                                                Column(
                                                    modifier = Modifier
                                                        .zIndex(1f)
                                                        .fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    CameraComponent(pictureCount =
                                                    otherUserSessionForSignList.size,
                                                        onNextPhoto = {
                                                            index++
                                                        },
                                                        content = {
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
                                                                Text("拍摄给 ${combinedUserList[index]} 签到的图片")
                                                            }
                                                        }) {
                                                        coroutineScope.launch {
                                                            isCamera = false
                                                            bitmapList = it
                                                            if (isSelfForSign) {
                                                                runCatching {
                                                                    signStatus[0].loading()
                                                                    signer.getCloudToken()
                                                                        .let { token ->
                                                                            signer.uploadImage(
                                                                                context,
                                                                                it[0],
                                                                                token
                                                                            ).let { objectId ->
                                                                                suspendCoroutine { continuation ->
                                                                                    runBlocking {
                                                                                        signer.signByImage(
                                                                                            objectId
                                                                                        ) {
                                                                                            captchaValidateParams =
                                                                                                signer to { validateValue ->
                                                                                                    validateValue.onSuccess {
                                                                                                        runBlocking {
                                                                                                            runCatching {
                                                                                                                signer.signByImageWithCaptcha(
                                                                                                                    objectId,
                                                                                                                    validateValue.getOrThrow()
                                                                                                                )
                                                                                                            }.onSuccess {
                                                                                                                UMengHelper.onSignPhotoEvent(
                                                                                                                    context,
                                                                                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                                                                                )
                                                                                                                signStatus[0].success()
                                                                                                                if (otherUserSessionForSignList.isEmpty()) {
                                                                                                                    isSponsor =
                                                                                                                        true
                                                                                                                }
                                                                                                            }
                                                                                                                .onFailure {
                                                                                                                    it.handleReport(
                                                                                                                        context,
                                                                                                                        "签到失败"
                                                                                                                    )
                                                                                                                    signStatus[0].failed(
                                                                                                                        it
                                                                                                                    )
                                                                                                                }
                                                                                                            continuation.resume(
                                                                                                                Unit
                                                                                                            )
                                                                                                        }
                                                                                                    }
                                                                                                        .onFailure {
                                                                                                            it.handleReport(
                                                                                                                context,
                                                                                                                "签到失败"
                                                                                                            )
                                                                                                            signStatus[0].failed(
                                                                                                                it
                                                                                                            )
                                                                                                            continuation.resume(
                                                                                                                Unit
                                                                                                            )
                                                                                                        }
                                                                                                }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                }.onFailure {
                                                                    if ((it is ChaoxingPredictableException).not()) {
                                                                        Sentry.captureException(it)
                                                                    }
                                                                    it.printStackTrace()
                                                                    signStatus[0].failed(it)
                                                                }.onSuccess {
                                                                    if (captchaValidateParams != null) return@onSuccess
                                                                    UMengHelper.onSignPhotoEvent(
                                                                        context,
                                                                        ChaoxingHttpClient.instance!!.userEntity.name
                                                                    )
                                                                    signStatus[0].success()
                                                                    if (otherUserSessionForSignList.isEmpty()) {
                                                                        isSponsor = true
                                                                    }
                                                                }
                                                            }
                                                            otherUserSessionForSignList.toList()
                                                                .forEachIndexed { index, chaoxingOtherUserSession ->
                                                                    if (chaoxingOtherUserSession == null) return@forEachIndexed
                                                                    runCatching {
                                                                        signStatus[1 + index].loading()
                                                                        suspendCoroutine { continuation ->
                                                                            runBlocking {
                                                                                ChaoxingHttpClient.loadFromOtherUserSession(
                                                                                    chaoxingOtherUserSession,
                                                                                    context
                                                                                ).also { client ->
                                                                                    ChaoxingPhotoSigner(
                                                                                        client,
                                                                                        destination
                                                                                    ).apply {
                                                                                        if (preSign()) {
                                                                                            signStatus[1 + index].failed(
                                                                                                ChaoxingSigner.AlreadySignedException()
                                                                                            )
                                                                                        } else {
                                                                                            val objectId =
                                                                                                uploadImage(
                                                                                                    context,
                                                                                                    it[bitmapIndexList.indexOf(
                                                                                                        index + 1
                                                                                                    )],
                                                                                                    getCloudToken()
                                                                                                )
                                                                                            signByImage(
                                                                                                objectId
                                                                                            ) {
                                                                                                captchaValidateParams =
                                                                                                    this to { validateValue ->
                                                                                                        validateValue.onSuccess {
                                                                                                            runBlocking {
                                                                                                                runCatching {
                                                                                                                    this@apply.signByImageWithCaptcha(
                                                                                                                        objectId,
                                                                                                                        validateValue.getOrThrow()
                                                                                                                    )
                                                                                                                }.onSuccess {
                                                                                                                    UMengHelper.onSignPhotoEvent(
                                                                                                                        context,
                                                                                                                        ChaoxingHttpClient.instance!!.userEntity.name,
                                                                                                                        true
                                                                                                                    )
                                                                                                                    signStatus[index + 1].success()
                                                                                                                    otherUserSessionForSignList.remove(
                                                                                                                        chaoxingOtherUserSession
                                                                                                                    )
                                                                                                                    if (otherUserSessionForSignList.isEmpty()) {
                                                                                                                        isSponsor =
                                                                                                                            true
                                                                                                                    }
                                                                                                                }
                                                                                                                    .onFailure {
                                                                                                                        it.snackbarReport(
                                                                                                                            snackbarHost,
                                                                                                                            coroutineScope,
                                                                                                                            "签到失败"
                                                                                                                        )
                                                                                                                        signStatus[index + 1].failed(
                                                                                                                            it
                                                                                                                        )
                                                                                                                    }
                                                                                                                continuation.resume(
                                                                                                                    Unit
                                                                                                                )
                                                                                                            }
                                                                                                        }
                                                                                                            .onFailure {
                                                                                                                it.handleReport(
                                                                                                                    context,
                                                                                                                    "验证码校验失败"
                                                                                                                )
                                                                                                                signStatus[index + 1].failed(
                                                                                                                    it
                                                                                                                )
                                                                                                                continuation.resume(
                                                                                                                    Unit
                                                                                                                )
                                                                                                            }
                                                                                                    }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }.onFailure {
                                                                        it.snackbarReport(
                                                                            snackbarHost,
                                                                            coroutineScope,
                                                                            "签到失败"
                                                                        )
                                                                        signStatus[1 + index].failed(
                                                                            it
                                                                        )
                                                                    }.onSuccess {
                                                                        if (captchaValidateParams != null) return@onSuccess
                                                                        UMengHelper.onSignPhotoEvent(
                                                                            context,
                                                                            chaoxingOtherUserSession.name,
                                                                            true
                                                                        )
                                                                        signStatus[1 + index].success()
                                                                        otherUserSessionForSignList.remove(
                                                                            chaoxingOtherUserSession
                                                                        )
                                                                        if (otherUserSessionForSignList.isEmpty()) {
                                                                            isSponsor = true
                                                                        }
                                                                    }
                                                                    if (index != otherUserSessionForSignList.size - 1) {
                                                                        signStatus[2 + index].loading()
                                                                        delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                                                    }
                                                                }
                                                            isSigning = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                false -> {
                                    BackHandler(isSignForOther == false) {
                                        isSignForOther = null
                                    }
                                    val photoPermissionsState = rememberMultiplePermissionsState(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) listOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES,
                                            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                        ) else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) listOf(
                                            android.Manifest.permission.READ_MEDIA_IMAGES
                                        ) else listOf(
                                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                                        )
                                    )
                                    if (photoPermissionsState.allPermissionsGranted) {
                                        if (isShowPhotoPicker) signer.GetPhotoFromMediaStore { uri ->
                                            if (uri == null) {
                                                return@GetPhotoFromMediaStore
                                            }
                                            coroutineScope.launch {
                                                runCatching {
                                                    signer.getCloudToken().let { token ->
                                                        signer.uploadImage(context, uri, token)
                                                            .let { objectId ->
                                                                signer.signByImage(objectId) {
                                                                    captchaValidateParams =
                                                                        signer to { validateValue ->
                                                                            validateValue.onSuccess {
                                                                                coroutineScope.launch {
                                                                                    runCatching {
                                                                                        signer.signByImageWithCaptcha(
                                                                                            objectId,
                                                                                            validateValue.getOrThrow()
                                                                                        )
                                                                                    }.onSuccess {
                                                                                        isSignSuccess =
                                                                                            true
                                                                                    }.onFailure {
                                                                                        if ((it is ChaoxingPredictableException).not()) {
                                                                                            Sentry.captureException(
                                                                                                it
                                                                                            )
                                                                                        }
                                                                                        Toast.makeText(
                                                                                            context,
                                                                                            "签到失败",
                                                                                            Toast.LENGTH_SHORT
                                                                                        ).show()
                                                                                    }
                                                                                }
                                                                            }.onFailure {
                                                                                it.snackbarReport(
                                                                                    snackbarHost,
                                                                                    coroutineScope,
                                                                                    "签到失败"
                                                                                )
                                                                            }
                                                                        }
                                                                }
                                                                UMengHelper.onSignPhotoEvent(
                                                                    context,
                                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                                )
                                                            }
                                                    }
                                                }.onFailure {
                                                    if ((it is ChaoxingPredictableException).not()) {
                                                        Sentry.captureException(it)
                                                    }
                                                    it.snackbarReport(
                                                        snackbarHost,
                                                        coroutineScope,
                                                        "签到失败"
                                                    )
                                                }.onSuccess {
                                                    if (captchaValidateParams != null) return@onSuccess
                                                    isSignSuccess = true
                                                }
                                                isShowPhotoPicker = false
                                            }
                                        }
                                        Crossfade(isSignSuccess) {
                                            if (it) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.ic_check_px80),
                                                        ""
                                                    )
                                                    Text("签到成功")
                                                    Button(onClick = {
                                                        navBack()
                                                    }) { Text("返回") }
                                                }
                                            } else {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.Center,
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Button(onClick = {
                                                        isShowPhotoPicker = true
                                                    }) {
                                                        Text("选择图片")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text("请授予应用读取图片权限")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { photoPermissionsState.launchMultiplePermissionRequest() },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text("授予")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                true -> {
                    AlreadySignedNotice({
                        isAlreadySigned = false
                        isForSelf = true
                    }, onDismiss = {
                        isAlreadySigned = false
                    }) {
                        navBack()
                    }
                }

                null -> {
                    CenterCircularProgressIndicator()
                }
            }
        }
    }
}