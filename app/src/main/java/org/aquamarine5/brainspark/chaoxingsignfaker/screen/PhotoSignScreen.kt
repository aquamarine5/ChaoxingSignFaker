/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.checkIsLast
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CameraComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias ChaoxingPhotoActivityEntity = PhotoSignDestination

@Serializable
data class PhotoSignDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String,
    val startTime: Long,
    val endTime: Long?,
    val isLate: Boolean
) {
    companion object {
        fun parseFromSignActivityEntity(
            activityEntity: ChaoxingSignActivityEntity,
            isLate: Boolean
        ): PhotoSignDestination {
            return PhotoSignDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext,
                activityEntity.startTime,
                activityEntity.endTime,
                isLate
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoSignScreen(
    destination: PhotoSignDestination,
    navBack: () -> Unit,
    navToOtherSign: (Any) -> Unit,
    navToOtherUserDestination: () -> Unit
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
    var signoffEntity by remember { mutableStateOf<ChaoxingSignOutEntity?>(null) }
    if (isSponsor) {
        SponsorPopupDialog()
    }
    var captchaValidateParams by remember {
        mutableStateOf<Pair<ChaoxingPhotoSigner, suspend (Result<String>) -> Unit>?>(null)
    }
    if (captchaValidateParams != null) {
        CaptchaHandlerDialog(
            captchaValidateParams!!.first,
            captchaValidateParams!!.second,
            onDismiss = {
                captchaValidateParams = null
            })
    }
    val hapticFeedback = LocalHapticFeedback.current
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    LaunchedEffect(Unit) {
        isFetchedFailure = runCatching {
            val data = signer.ifPhotoRequiredLogin()
            isImage = data.first
            signoffEntity = data.second
            isAlreadySigned = signer.preSign()
        }.onFailure {
            it.snackbarReport(
                snackbarHost,
                coroutineScope,
                "获取签到信息失败", hapticFeedback
            )
        }
    }
    Crossfade(isFetchedFailure) { v ->
        if (v == null) {
            CenterCircularProgressIndicator()
        } else if (v.isFailure) {
            NetworkExceptionComponent(v.exceptionOrNull()!!) {
                coroutineScope.launch {
                    isFetchedFailure = runCatching {
                        val data = signer.ifPhotoRequiredLogin()
                        isImage = data.first
                        signoffEntity = data.second
                        isAlreadySigned = signer.preSign()
                    }.onFailure {
                        it.snackbarReport(
                            snackbarHost,
                            coroutineScope,
                            "获取签到信息失败", hapticFeedback
                        )
                    }
                }
            }
        } else {
            if (isAlreadySigned == false) {
                if (isImage == false) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        var isCaptcha = remember { false }
                        var isFirstOtherUserForSign = remember { true }
                        var isSigning by remember { mutableStateOf(false) }
                        val signStatus =
                            remember { mutableListOf(ChaoxingSignStatus(hapticFeedback)) }
                        val userSelections = remember { mutableStateListOf(isForSelf.not()) }
                        OtherUserSelectorComponent(
                            navToOtherUser = {
                                navToOtherUserDestination()
                            },
                            signStatus,
                            isForSelf,
                            userSelections,
                            isSigning,
                            prefixTipsContent = {
                                Card(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW, "cxstudy://".toUri()
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.DarkGray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(2.dp, 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp, 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            painterResource(R.drawable.ic_info),
                                            contentDescription = "Info",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            buildAnnotatedString {
                                                append("这是一个普通的点击签到，不会收集任何其他的信息，推荐对于这种签到使用学习通APP而不是随地大小签。")
                                                withStyle(
                                                    SpanStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = TextDecoration.Underline
                                                    )
                                                ) {
                                                    append("\n点击跳转到学习通。")
                                                }
                                            },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.W500
                                        )
                                    }
                                }
                                if (signoffEntity != null)
                                    SignOutRedirectTips(
                                        signoffEntity!!
                                    ) {
                                        navToOtherSign(it)
                                    }
                                SignPotentialWarningTips(
                                    destination.startTime,
                                    destination.endTime,
                                    destination.isLate
                                )
                            }
                        ) { isSelf, otherUserSessionList, _ ->
                            isSigning = true
                            coroutineScope.launch {
                                if (isSelf) runCatching {
                                    signStatus[0].loading()
                                    if (signer.signByClick()) {
                                        isCaptcha = true
                                        suspendCoroutine { continuation ->
                                            captchaValidateParams =
                                                signer to { validateValue ->
                                                    validateValue.onSuccess {
                                                        signer.signByClickWithCaptcha(
                                                            validateValue.getOrThrow()
                                                        )
                                                        UMengHelper.onSignClickEvent(
                                                            context,
                                                            ChaoxingHttpClient.instance!!.userEntity.name
                                                        )
                                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                            signStatus[0].successForLate()
                                                        else
                                                            signStatus[0].success()
                                                        userSelections[0] = false
                                                        if (otherUserSessionList.all { it == null }) {
                                                            isSigning = false
                                                            coroutineScope.launch {
                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                    context,
                                                                    destination.classId,
                                                                    destination.courseId,
                                                                    ChaoxingHttpClient.instance!!
                                                                )
                                                            }
                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                            isSponsor = true
                                                        }
                                                    }.onFailure {
                                                        it.snackbarReport(
                                                            snackbarHost,
                                                            coroutineScope,
                                                            "验证码校验失败", hapticFeedback
                                                        )
                                                        signStatus[0].failed(it)
                                                    }
                                                    continuation.resume(Unit)
                                                }
                                        }
                                    } else {
                                        UMengHelper.onSignClickEvent(
                                            context,
                                            ChaoxingHttpClient.instance!!.userEntity.name
                                        )
                                        userSelections[0] = false
                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                            signStatus[0].successForLate()
                                        else
                                            signStatus[0].success()
                                        if (otherUserSessionList.all { it == null }) {
                                            isSigning = false
                                            coroutineScope.launch {
                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                    context,
                                                    destination.classId,
                                                    destination.courseId,
                                                    ChaoxingHttpClient.instance!!
                                                )
                                            }
                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                            isSponsor = true
                                        }
                                    }
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                                        hapticFeedback
                                    )
                                    it.ifAlreadySigned {
                                        userSelections[0] = false
                                        if (otherUserSessionList.all { it == null } && userSelections.all { !it }) {
                                            isSigning = false
                                            coroutineScope.launch {
                                                delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                isSponsor = true
                                            }
                                        }
                                    }
                                    signStatus[0].failed(it)
                                }

                                otherUserSessionList.forEachIndexed { index, userSession ->
                                    if (userSession == null) return@forEachIndexed
                                    runCatching {
                                        signStatus[1 + index].loading()
                                        if (!isCaptcha || (isSelf && isFirstOtherUserForSign))
                                            delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                        isFirstOtherUserForSign = false
                                        ChaoxingHttpClient.loadFromOtherUserSession(
                                            userSession, context
                                        ).also { client ->
                                            ChaoxingPhotoSigner(
                                                client, destination
                                            ).apply {
                                                if (preSign()) {
                                                    throw ChaoxingSigner.AlreadySignedException()
                                                } else if (ifPhotoRequiredLogin().first) {
                                                    throw ChaoxingPhotoSigner.ChaoxingIncorrectSignTypeException()
                                                } else {
                                                    if (ChaoxingCourseHelper.checkClassValid(
                                                            client,
                                                            destination.classId
                                                        ) == false
                                                    )
                                                        throw ChaoxingSigner.SignActivityNoPermissionException()
                                                    if (signByClick()) {
                                                        isCaptcha = true
                                                        suspendCoroutine { continuation ->
                                                            captchaValidateParams =
                                                                this@apply to { validateValue ->
                                                                    validateValue.onSuccess {
                                                                        this@apply.signByClickWithCaptcha(
                                                                            validateValue.getOrThrow()
                                                                        )
                                                                        UMengHelper.onSignClickEvent(
                                                                            context,
                                                                            userSession.name,
                                                                            isOtherUser = true
                                                                        )
                                                                        userSelections[1 + index] =
                                                                            false
                                                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                            signStatus[1 + index].successForLate()
                                                                        else
                                                                            signStatus[1 + index].success()
                                                                        if (otherUserSessionList.checkIsLast(
                                                                                index + 1
                                                                            )
                                                                        ) {
                                                                            isSigning = false
                                                                            coroutineScope.launch {
                                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                    context,
                                                                                    destination.classId,
                                                                                    destination.courseId,
                                                                                    client
                                                                                )
                                                                            }
                                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                            isSponsor = true
                                                                        }
                                                                    }.onFailure { err ->
                                                                        err.snackbarReport(
                                                                            snackbarHost,
                                                                            coroutineScope,
                                                                            "验证码校验失败",
                                                                            hapticFeedback
                                                                        )
                                                                        signStatus[1 + index].failed(
                                                                            err
                                                                        )
                                                                    }
                                                                    continuation.resume(
                                                                        Unit
                                                                    )
                                                                }
                                                        }
                                                    } else {
                                                        UMengHelper.onSignClickEvent(
                                                            context,
                                                            userSession.name,
                                                            isOtherUser = true
                                                        )
                                                        userSelections[1 + index] = false
                                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                            signStatus[1 + index].successForLate()
                                                        else
                                                            signStatus[1 + index].success()
                                                        if (otherUserSessionList.checkIsLast(index + 1)) {
                                                            isSigning = false
                                                            coroutineScope.launch {
                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                    context,
                                                                    destination.classId,
                                                                    destination.courseId,
                                                                    client
                                                                )
                                                            }
                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                            isSponsor = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }.onFailure { err ->
                                        err.snackbarReport(
                                            snackbarHost,
                                            coroutineScope,
                                            "为${userSession.name}签到失败",
                                            hapticFeedback
                                        )
                                        err.ifAlreadySigned {
                                            userSelections[index + 1] = false
                                            if (otherUserSessionList.checkIsLast(index + 1) && userSelections.all { !it }) {
                                                isSigning = false
                                                coroutineScope.launch {
                                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                    isSponsor =
                                                        true
                                                }
                                            }
                                        }
                                        signStatus[1 + index].failed(err)
                                    }
                                }
                                isSigning = false
                            }
                        }
                    }
                } else if (isImage == true) {
                    var isCaptcha = remember { false }
                    var isFirstOtherUserForSign = remember { true }
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
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("这是一个图片签到")
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            isSignForOther = false
                                        },
                                        enabled = isForSelf.not(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("为自己签到（从图库读取图片）") }
                                    Button(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            isSignForOther = true
                                        }, modifier = Modifier.fillMaxWidth()
                                    ) { Text("为多人签到（拍摄或从图库读取多张图片）") }
                                }
                            }

                            true -> {
                                val userSelections =
                                    remember { mutableStateListOf(isForSelf.not()) }
                                val signStatus =
                                    remember { mutableListOf(ChaoxingSignStatus(hapticFeedback)) }
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
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        OtherUserSelectorComponent(
                                            navToOtherUser = {
                                                navToOtherUserDestination()
                                            },
                                            signStatus,
                                            isForSelf,
                                            userSelections,
                                            isSigning,
                                            prefixTipsContent = {
                                                if (signoffEntity != null)
                                                    SignOutRedirectTips(
                                                        signoffEntity!!
                                                    ) {
                                                        navToOtherSign(it)
                                                    }
                                                SignPotentialWarningTips(
                                                    destination.startTime,
                                                    destination.endTime,
                                                    destination.isLate
                                                )
                                            },
                                            userContent = { index ->
                                                var isShowDialog by remember {
                                                    mutableStateOf(
                                                        false
                                                    )
                                                }
                                                bitmapIndexList.indexOf(index).let {
                                                    if (it != -1 && bitmapList.size > it) {
                                                        IconButton(onClick = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.ContextClick
                                                            )
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
                                            ), exit = slideOutHorizontally(
                                                targetOffsetX = { -it },
                                                animationSpec = tween(300)
                                            ) + fadeOut(
                                                animationSpec = tween(300)
                                            )
                                        ) {
                                            BackHandler(isCamera) {
                                                isSigning = false
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
                                            var imageIndex by remember { mutableIntStateOf(0) }
                                            Column(
                                                modifier = Modifier
                                                    .zIndex(1f)
                                                    .fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                CameraComponent(
                                                    pictureCount =
                                                        combinedUserList.size,
                                                    onNextPhoto = {
                                                        imageIndex++
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
                                                            Text("拍摄给 ${combinedUserList[imageIndex]} 签到的图片")
                                                        }
                                                    }) { imageList ->
                                                    coroutineScope.launch {
                                                        isCamera = false
                                                        bitmapList = imageList
                                                        if (isSelfForSign) {
                                                            runCatching {
                                                                signStatus[0].loading()
                                                                signer.getCloudToken()
                                                                    .let { token ->
                                                                        signer.uploadImage(
                                                                            imageList[0],
                                                                            token
                                                                        ).let { objectId ->
                                                                            if (signer.signByImage(
                                                                                    objectId
                                                                                )
                                                                            ) {
                                                                                isCaptcha = true
                                                                                suspendCoroutine { continuation ->
                                                                                    captchaValidateParams =
                                                                                        signer to { validateValue ->
                                                                                            validateValue.onSuccess {
                                                                                                signer.signByImageWithCaptcha(
                                                                                                    objectId,
                                                                                                    validateValue.getOrThrow()
                                                                                                )
                                                                                                UMengHelper.onSignPhotoEvent(
                                                                                                    context,
                                                                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                                                                )
                                                                                                userSelections[0] =
                                                                                                    false
                                                                                                if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                                    signStatus[0].successForLate()
                                                                                                else
                                                                                                    signStatus[0].success()
                                                                                                if (otherUserSessionForSignList.all { it == null }) {
                                                                                                    isSigning =
                                                                                                        false
                                                                                                    coroutineScope.launch {
                                                                                                        ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                                            context,
                                                                                                            destination.classId,
                                                                                                            destination.courseId,
                                                                                                            ChaoxingHttpClient.instance!!
                                                                                                        )
                                                                                                    }
                                                                                                    delay(
                                                                                                        ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED
                                                                                                    )
                                                                                                    isSponsor =
                                                                                                        true
                                                                                                }
                                                                                            }
                                                                                                .onFailure {
                                                                                                    it.snackbarReport(
                                                                                                        snackbarHost,
                                                                                                        coroutineScope,
                                                                                                        "验证码校验失败",
                                                                                                        hapticFeedback
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
                                                                            } else {
                                                                                UMengHelper.onSignPhotoEvent(
                                                                                    context,
                                                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                                                )
                                                                                userSelections[0] =
                                                                                    false
                                                                                if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                    signStatus[0].successForLate()
                                                                                else
                                                                                    signStatus[0].success()
                                                                                if (otherUserSessionForSignList.all { it == null }) {
                                                                                    isSigning =
                                                                                        false
                                                                                    coroutineScope.launch {
                                                                                        ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                            context,
                                                                                            destination.classId,
                                                                                            destination.courseId,
                                                                                            ChaoxingHttpClient.instance!!
                                                                                        )
                                                                                    }
                                                                                    delay(
                                                                                        ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED
                                                                                    )
                                                                                    isSponsor = true
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                            }.onFailure {
                                                                it.snackbarReport(
                                                                    snackbarHost,
                                                                    coroutineScope,
                                                                    "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                                                                    hapticFeedback
                                                                )
                                                                it.ifAlreadySigned {
                                                                    userSelections[0] = false
                                                                    if (otherUserSessionForSignList.all { it == null } && userSelections.all { !it }) {
                                                                        isSigning = false
                                                                        coroutineScope.launch {
                                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                            isSponsor = true
                                                                        }
                                                                    }
                                                                }
                                                                signStatus[0].failed(it)
                                                            }
                                                        }

                                                        otherUserSessionForSignList.forEachIndexed { index, chaoxingOtherUserSession ->
                                                            if (chaoxingOtherUserSession == null) return@forEachIndexed
                                                            runCatching {
                                                                signStatus[1 + index].loading()
                                                                if (!isCaptcha || (isSelfForSign && isFirstOtherUserForSign))
                                                                    delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                                                isFirstOtherUserForSign = false
                                                                ChaoxingHttpClient.loadFromOtherUserSession(
                                                                    chaoxingOtherUserSession,
                                                                    context
                                                                ).also { client ->
                                                                    ChaoxingPhotoSigner(
                                                                        client,
                                                                        destination
                                                                    ).apply {
                                                                        if (preSign()) {
                                                                            throw ChaoxingSigner.AlreadySignedException()
                                                                        } else {
                                                                            if (ChaoxingCourseHelper.checkClassValid(
                                                                                    client,
                                                                                    destination.classId
                                                                                ) == false
                                                                            )
                                                                                throw ChaoxingSigner.SignActivityNoPermissionException()
                                                                            val objectId =
                                                                                uploadImage(
                                                                                    imageList[bitmapIndexList.indexOf(
                                                                                        index + 1
                                                                                    )],
                                                                                    getCloudToken()
                                                                                )
                                                                            if (signByImage(
                                                                                    objectId
                                                                                )
                                                                            ) {
                                                                                isCaptcha = true
                                                                                suspendCoroutine { continuation ->
                                                                                    captchaValidateParams =
                                                                                        this to { validateValue ->
                                                                                            validateValue.onSuccess {
                                                                                                this@apply.signByImageWithCaptcha(
                                                                                                    objectId,
                                                                                                    validateValue.getOrThrow()
                                                                                                )
                                                                                                UMengHelper.onSignPhotoEvent(
                                                                                                    context,
                                                                                                    ChaoxingHttpClient.instance!!.userEntity.name,
                                                                                                    true
                                                                                                )
                                                                                                userSelections[1 + index] =
                                                                                                    false
                                                                                                if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                                    signStatus[1 + index].successForLate()
                                                                                                else
                                                                                                    signStatus[1 + index].success()
                                                                                                if (otherUserSessionForSignList.checkIsLast(
                                                                                                        index + 1
                                                                                                    )
                                                                                                ) {
                                                                                                    isSigning =
                                                                                                        false
                                                                                                    coroutineScope.launch {
                                                                                                        ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                                            context,
                                                                                                            destination.classId,
                                                                                                            destination.courseId,
                                                                                                            client
                                                                                                        )
                                                                                                    }
                                                                                                    delay(
                                                                                                        ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED
                                                                                                    )
                                                                                                    isSponsor =
                                                                                                        true
                                                                                                }
                                                                                            }
                                                                                                .onFailure {
                                                                                                    it.snackbarReport(
                                                                                                        snackbarHost,
                                                                                                        coroutineScope,
                                                                                                        "验证码校验失败",
                                                                                                        hapticFeedback
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
                                                                            } else {
                                                                                UMengHelper.onSignPhotoEvent(
                                                                                    context,
                                                                                    chaoxingOtherUserSession.name,
                                                                                    true
                                                                                )
                                                                                userSelections[1 + index] =
                                                                                    false
                                                                                if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                    signStatus[1 + index].successForLate()
                                                                                else
                                                                                    signStatus[1 + index].success()
                                                                                if (otherUserSessionForSignList.checkIsLast(
                                                                                        index + 1
                                                                                    )
                                                                                ) {
                                                                                    isSigning =
                                                                                        false
                                                                                    coroutineScope.launch {
                                                                                        ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                            context,
                                                                                            destination.classId,
                                                                                            destination.courseId,
                                                                                            client
                                                                                        )
                                                                                    }
                                                                                    delay(
                                                                                        ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED
                                                                                    )
                                                                                    isSponsor =
                                                                                        true
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }.onFailure {
                                                                it.snackbarReport(
                                                                    snackbarHost,
                                                                    coroutineScope,
                                                                    "为${chaoxingOtherUserSession.name}签到失败",
                                                                    hapticFeedback
                                                                )
                                                                it.ifAlreadySigned {
                                                                    userSelections[index + 1] =
                                                                        false
                                                                    if (otherUserSessionForSignList.checkIsLast(
                                                                            index + 1
                                                                        ) && userSelections.all { !it }
                                                                    ) {
                                                                        isSigning = false
                                                                        coroutineScope.launch {
                                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                            isSponsor =
                                                                                true
                                                                        }
                                                                    }
                                                                }
                                                                signStatus[1 + index].failed(
                                                                    it
                                                                )
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
                                                            if (signer.signByImage(objectId)) {
                                                                captchaValidateParams =
                                                                    signer to { validateValue ->
                                                                        validateValue.onSuccess {
                                                                            signer.signByImageWithCaptcha(
                                                                                objectId,
                                                                                validateValue.getOrThrow()
                                                                            )
                                                                            coroutineScope.launch {
                                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                                    context,
                                                                                    destination.classId,
                                                                                    destination.courseId,
                                                                                    ChaoxingHttpClient.instance!!
                                                                                )
                                                                            }
                                                                            isSignSuccess =
                                                                                true
                                                                            hapticFeedback.performHapticFeedback(
                                                                                HapticFeedbackType.Confirm
                                                                            )
                                                                        }.onFailure {
                                                                            it.snackbarReport(
                                                                                snackbarHost,
                                                                                coroutineScope,
                                                                                "验证码校验错误",
                                                                                hapticFeedback
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
                                                it.snackbarReport(
                                                    snackbarHost,
                                                    coroutineScope,
                                                    "签到失败", hapticFeedback
                                                )
                                            }
                                            isShowPhotoPicker = false
                                        }
                                    }
                                    Crossfade(isSignSuccess) { v ->
                                        if (v) {
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
                                                if (signoffEntity != null)
                                                    SignOutRedirectTips(
                                                        signoffEntity!!
                                                    ) {
                                                        navToOtherSign(it)
                                                    }
                                                Button(onClick = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.ContextClick
                                                    )
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
                                            onClick = {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                photoPermissionsState.launchMultiplePermissionRequest()
                                            },
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
            } else if (isAlreadySigned == true) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    AlreadySignedNotice({
                        isAlreadySigned = false
                        isForSelf = true
                    }, onDismiss = {
                        isAlreadySigned = false
                    }) {
                        navBack()
                    }
                    SignPotentialWarningTips(
                        destination.startTime,
                        destination.endTime,
                        destination.isLate,
                        isPadding = true
                    )
                }
            }
        }
    }
}
