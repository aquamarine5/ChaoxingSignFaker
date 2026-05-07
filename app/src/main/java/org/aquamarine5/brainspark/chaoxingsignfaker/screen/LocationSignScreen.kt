/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCloudDriveHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.checkIsLast
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.FaceRecognitionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NotReadyToSignNoticeComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifShouldDeselect
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume

@Serializable
data class GetLocationDestination(
    override val activeId: Long,
    override val classId: Int,
    override val courseId: Int,
    val extContent: String,
    val startTime: Long?,
    val endTime: Long?,
    val isLate: Boolean
) : SignDestination {
    companion object {
        fun parseFromSignActivityEntity(
            activityEntity: ChaoxingSignActivityEntity,
            isLate: Boolean
        ): GetLocationDestination {
            return GetLocationDestination(
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

@Composable
fun LocationSignScreen(
    destination: GetLocationDestination,
    navToCourseDetailDestination: () -> Unit,
    navToOtherSign: (SignDestination) -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    var signActivityStatus by remember { mutableStateOf<ChaoxingSignActivityStatus?>(null) }
    var isSignForOther by remember { mutableStateOf(false) }
    var signInfo by remember { mutableStateOf<ChaoxingLocationDetailEntity?>(null) }
    val signer = remember { ChaoxingLocationSigner(ChaoxingHttpClient.instance!!, destination) }
    var isSponsor by remember { mutableStateOf(false) }
    if (isSponsor) {
        SponsorPopupDialog()
    }
    var captchaValidateParams by remember {
        mutableStateOf<Pair<ChaoxingLocationSigner, suspend (Result<String>) -> Unit>?>(
            null
        )
    }
    if (captchaValidateParams != null) {
        CaptchaHandlerDialog(
            captchaValidateParams!!.first,
            captchaValidateParams!!.second,
            onDismiss = {
                captchaValidateParams = null
            })
    }
    var signoffData by remember { mutableStateOf<ChaoxingSignOutEntity?>(null) }
    val snackbarHost = LocalSnackbarHostState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    LaunchedEffect(Unit) {
        isFetchedFailure = runCatching {
            val data = signer.getLocationSignInfo()
            signInfo = data.first
            signoffData = data.second
            signActivityStatus = signer.preSign()
        }.onFailure {
            it.snackbarReport(
                snackbarHost,
                coroutineScope,
                "获取签到信息失败",
                hapticFeedback
            )
        }
    }
    Crossfade(isFetchedFailure) { f ->
        if (f == null) {
            CenterCircularProgressIndicator()
        } else if (f.isFailure) {
            NetworkExceptionComponent(f.exceptionOrNull()!!) {
                coroutineScope.launch {
                    isFetchedFailure = runCatching {
                        val data = signer.getLocationSignInfo()
                        signInfo = data.first
                        signoffData = data.second
                        signActivityStatus = signer.preSign()
                    }.onFailure {
                        it.snackbarReport(
                            snackbarHost,
                            coroutineScope,
                            "获取签到信息失败",
                            hapticFeedback
                        )
                    }
                }
                isFetchedFailure = null
            }
        } else {
            Crossfade(signActivityStatus) { c ->
                if (c != null && c != ChaoxingSignActivityStatus.READY_TO_SIGN) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        NotReadyToSignNoticeComponent(
                            onSignForOtherUser = {
                                signActivityStatus = ChaoxingSignActivityStatus.READY_TO_SIGN
                                isSignForOther = true
                            }, onDismiss = {
                                signActivityStatus = ChaoxingSignActivityStatus.READY_TO_SIGN
                            }, isExpiredSign = c == ChaoxingSignActivityStatus.EXPIRED
                        ) { navToCourseDetailDestination() }

                        if (destination.startTime != null)
                            SignPotentialWarningTips(
                                destination.startTime,
                                destination.endTime,
                                destination.isLate,
                                isPadding = true
                            )
                    }
                } else if (c == ChaoxingSignActivityStatus.READY_TO_SIGN) {
                    var isGetLocation by remember { mutableStateOf(false) }
                    val signStatus = remember { mutableListOf(ChaoxingSignStatus(hapticFeedback)) }
                    var isSelfForSign by remember { mutableStateOf(false) }
                    var isSigning by remember { mutableStateOf(false) }
                    var otherUserSessionForSignList by
                    remember { mutableStateOf<List<ChaoxingOtherUserSession?>>(emptyList()) }
                    val userSelections = remember { mutableStateListOf(isSignForOther.not()) }
                    var isCaptcha = remember { false }
                    var isFirstOtherUserForSign = remember { true }

                    // future will be edited.
                    var isFaceRequired by remember { mutableStateOf(false) }
                    var isFaceImageCaptured by remember { mutableStateOf(false) }

                    val faceImageBitmaps = remember { mutableMapOf<String, Bitmap>() }
                    val faceImageObjectIds = remember { mutableMapOf<String, String>() }

                    LaunchedEffect(Unit) {
                        isFaceRequired = signer.isFaceRequired()
                    }


                    Column(modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OtherUserSelectorComponent(
                            navToOtherUser = { navToOtherUserDestination() },
                            signStatus = signStatus,
                            isCurrentAlreadySigned = isSignForOther,
                            isSigning = isSigning,
                            userSelections = userSelections,
                            prefixTipsContent = {
                                if (signoffData != null)
                                    SignOutRedirectTips(
                                        signoffData!!
                                    ) {
                                        navToOtherSign(it)
                                    }
                                if (destination.startTime != null)
                                    SignPotentialWarningTips(
                                        destination.startTime,
                                        destination.endTime,
                                        destination.isLate
                                    )

                                if (isFaceRequired)
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF12AA9C)
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
                                                painterResource(R.drawable.ic_scan_face),
                                                contentDescription = "Help",
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(9.dp))
                                            Text(
                                                "已经临时破解人脸识别签到，以下人脸识别签到方法并不是最优解，仅供临时使用。\n为自己签到时，调用前置摄像头为自己拍摄一张正脸照片（睁眼），随后正常设置位置以签到。\n为其他人代签时，可以提前保存一张他的正脸照，随后在拍摄页面点击右下角按钮选择图片上传，随后正常设置位置以签到。",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.W500,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                            }
                        ) { isSelf, otherUserSessionList, _ ->
                            isSigning = true
                            isSelfForSign = isSelf
                            otherUserSessionForSignList = otherUserSessionList
                            coroutineScope.launch {
                                if (isFaceRequired)
                                    isFaceImageCaptured = true
                                else
                                    isGetLocation = true
                            }
                        }
                    }
                    AnimatedVisibility(
                        isFaceImageCaptured, enter = slideInHorizontally(
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
                        FaceRecognitionComponent(mutableListOf<Pair<String, String>>().apply {
                            if (isSelfForSign && !faceImageObjectIds.containsKey(
                                    ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                )
                            ) add(ChaoxingHttpClient.instance!!.userEntity.phoneNumber to ChaoxingHttpClient.instance!!.userEntity.name)
                            otherUserSessionForSignList.forEach {
                                if (it != null && !faceImageObjectIds.containsKey(
                                        it.phoneNumber
                                    )
                                ) add(it.phoneNumber to it.name)
                            }
                        }, onCancel = {
                            isSigning = false
                            isFaceImageCaptured = false
                        }) {
                            faceImageBitmaps.putAll(it)
                            isGetLocation = true
                            isFaceImageCaptured = false
                        }
                    }
                    AnimatedVisibility(
                        isGetLocation,
                        enter =
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            ) + fadeIn(
                                animationSpec = tween(300)
                            ),
                        exit =
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { it }) +
                                    fadeOut(animationSpec = tween(300)),
                        modifier = Modifier.zIndex(1f)
                    ) {
                        BackHandler(isGetLocation) {
                            isSigning = false
                            isGetLocation = false
                        }
                        GetLocationComponent(signInfo, confirmButtonText = {
                            Text("签到")
                        }) { result ->
                            isGetLocation = false
                            coroutineScope.launch {
                                runCatching {
                                    if (isSelfForSign) {
                                        signStatus[0].loading()
                                        val faceImageUploadedObjectId =
                                            if (isFaceRequired) {
                                                faceImageObjectIds.getOrPut(
                                                    ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                                ) {
                                                    ChaoxingCloudDriveHelper.uploadImage(
                                                        ChaoxingHttpClient.instance!!,
                                                        faceImageBitmaps.remove(
                                                            ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                                        )!!
                                                    )
                                                }
                                            } else null
                                        if (signer.sign(
                                                result,
                                                faceImageUploadedObjectId
                                            )
                                        ) {
                                            isCaptcha = true
                                            suspendCancellableCoroutine { continuation ->
                                                captchaValidateParams =
                                                    signer to { captchaValidate ->
                                                        if (captchaValidate.isSuccess) {
                                                            signer.signWithCaptcha(
                                                                result,
                                                                captchaValidate.getOrThrow(),
                                                                faceImageUploadedObjectId
                                                            )
                                                            userSelections[0] = false
                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                signStatus[0].successForLate()
                                                            else
                                                                signStatus[0].success()
                                                            if (otherUserSessionForSignList.all { it == null }) {
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
                                                            UMengHelper.onSignLocationEvent(
                                                                context,
                                                                result,
                                                                ChaoxingHttpClient.instance!!.userEntity.name
                                                            )
                                                        } else {
                                                            (captchaValidate.exceptionOrNull()
                                                                ?: ChaoxingSigner.CaptchaException()).apply {
                                                                signStatus[0].failed(this)
                                                                this.snackbarReport(
                                                                    snackbarHost,
                                                                    coroutineScope,
                                                                    "验证码校验失败",
                                                                    hapticFeedback
                                                                )
                                                            }
                                                        }
                                                        continuation.resume(Unit)
                                                    }
                                            }
                                        } else {
                                            userSelections[0] = false
                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                signStatus[0].successForLate()
                                            else
                                                signStatus[0].success()
                                            if (otherUserSessionForSignList.all { it == null }) {
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
                                            UMengHelper.onSignLocationEvent(
                                                context,
                                                result,
                                                ChaoxingHttpClient.instance!!.userEntity.name
                                            )
                                        }
                                    }
                                }.onFailure {
                                    signStatus[0].failed(it)
                                    it.ifShouldDeselect {
                                        userSelections[0] = false
                                    }
                                    if (otherUserSessionForSignList.all { it == null }) {
                                        isSigning = false
                                        if (userSelections.all { !it })
                                            coroutineScope.launch {
                                                delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                isSponsor = true
                                            }
                                    }
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                                        hapticFeedback
                                    )
                                }

                                otherUserSessionForSignList.forEachIndexed { index, userSession ->
                                    if (userSession == null) return@forEachIndexed
                                    runCatching {
                                        signStatus[index + 1].loading()
                                        if (!isCaptcha || (isSelfForSign && isFirstOtherUserForSign))
                                            delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                        isFirstOtherUserForSign = false
                                        ChaoxingHttpClient.loadFromOtherUserSession(
                                            userSession,
                                            context
                                        ).also { client ->
                                            ChaoxingLocationSigner(
                                                client,
                                                destination,
                                                signer.getSignInfo()
                                            ).apply {
                                                when (preSign()) {
                                                    ChaoxingSignActivityStatus.EXPIRED -> {
                                                        throw ChaoxingSigner.SignExpiredException()
                                                    }

                                                    ChaoxingSignActivityStatus.ALREADY_SIGNED -> {
                                                        throw ChaoxingSigner.AlreadySignedException()
                                                    }

                                                    ChaoxingSignActivityStatus.READY_TO_SIGN -> {
                                                        if (ChaoxingCourseHelper.checkClassValid(
                                                                client,
                                                                destination.classId
                                                            ) == false
                                                        )
                                                            throw ChaoxingSigner.SignActivityNoPermissionException()
                                                        val faceImageUploadedObjectId =
                                                            if (isFaceRequired) {
                                                                faceImageObjectIds.getOrPut(
                                                                    userSession.phoneNumber
                                                                ) {
                                                                    ChaoxingCloudDriveHelper.uploadImage(
                                                                        client,
                                                                        faceImageBitmaps.remove(
                                                                            userSession.phoneNumber
                                                                        )!!
                                                                    )
                                                                }
                                                            } else null
                                                        if (sign(
                                                                result,
                                                                faceImageUploadedObjectId
                                                            )
                                                        ) {
                                                            isCaptcha = true
                                                            suspendCancellableCoroutine { continuation ->
                                                                captchaValidateParams =
                                                                    this@apply to { captchaValidate ->
                                                                        if (captchaValidate.isSuccess) {
                                                                            this@apply.signWithCaptcha(
                                                                                result,
                                                                                captchaValidate.getOrThrow(),
                                                                                faceImageUploadedObjectId
                                                                            )
                                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                signStatus[1 + index].successForLate()
                                                                            else
                                                                                signStatus[1 + index].success()
                                                                            userSelections[index + 1] =
                                                                                false
                                                                            if (otherUserSessionForSignList.checkIsLast(
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
                                                                                delay(
                                                                                    ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED
                                                                                )
                                                                                isSponsor = true
                                                                            }
                                                                            UMengHelper.onSignLocationEvent(
                                                                                context,
                                                                                result,
                                                                                userSession.name,
                                                                                true
                                                                            )
                                                                        } else {
                                                                            (captchaValidate.exceptionOrNull()
                                                                                ?: ChaoxingSigner.CaptchaException()).apply {
                                                                                signStatus[index + 1].failed(
                                                                                    this
                                                                                )
                                                                                this.snackbarReport(
                                                                                    snackbarHost,
                                                                                    coroutineScope,
                                                                                    "为${userSession.name}签到时验证码校验失败",
                                                                                    hapticFeedback
                                                                                )
                                                                            }
                                                                        }
                                                                        continuation.resume(Unit)
                                                                    }
                                                            }
                                                        } else {
                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                signStatus[1 + index].successForLate()
                                                            else
                                                                signStatus[1 + index].success()
                                                            if (otherUserSessionForSignList.checkIsLast(
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
                                                            userSelections[index + 1] = false
                                                            UMengHelper.onSignLocationEvent(
                                                                context,
                                                                result,
                                                                userSession.name,
                                                                true
                                                            )
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
                                        err.ifShouldDeselect {
                                            userSelections[index + 1] = false
                                        }
                                        if (otherUserSessionForSignList.checkIsLast(index + 1)) {
                                            isSigning = false
                                            if (userSelections.all { !it })
                                                coroutineScope.launch {
                                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                    isSponsor =
                                                        true
                                                }
                                        }
                                        signStatus[index + 1].failed(
                                            err
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    CenterCircularProgressIndicator()
                }
            }
        }
    }
}