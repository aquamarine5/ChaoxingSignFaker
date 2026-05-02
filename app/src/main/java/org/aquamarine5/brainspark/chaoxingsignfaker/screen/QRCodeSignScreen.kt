/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingQRCodeSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume

@Serializable
data class QRCodeSignDestination(
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
        ): QRCodeSignDestination {
            return QRCodeSignDestination(
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
fun QRCodeSignScreen(
    destination: QRCodeSignDestination,
    navToOtherUser: () -> Unit,
    navToOtherSign: (SignDestination) -> Unit,
    navBack: () -> Unit
) {
    var signActivityStatus by remember { mutableStateOf<ChaoxingSignActivityStatus?>(null) }
    var isCurrentAlreadySigned by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val signer = remember { ChaoxingQRCodeSigner(ChaoxingHttpClient.instance!!, destination) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHost = LocalSnackbarHostState.current
    var isMapRequired by remember { mutableStateOf(false) }
    var signoffData by remember { mutableStateOf<ChaoxingSignOutEntity?>(null) }
    var captchaValidateParams by remember {
        mutableStateOf<Pair<ChaoxingQRCodeSigner, suspend (Result<String>) -> Unit>?>(
            null
        )
    }
    val hapticFeedback = LocalHapticFeedback.current
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    if (captchaValidateParams != null) {
        CaptchaHandlerDialog(
            captchaValidateParams!!.first,
            captchaValidateParams!!.second,
            onDismiss = {
                captchaValidateParams = null
            })
    }
    LaunchedEffect(Unit) {
        isFetchedFailure = runCatching {
            signActivityStatus = signer.preSign()
            val data = signer.getQRCodeSignInfo()
            isMapRequired = data.first.isPositionRequired
            signoffData = data.second
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
                        signActivityStatus = signer.preSign()
                        val data = signer.getQRCodeSignInfo()
                        isMapRequired = data.first.isPositionRequired
                        signoffData = data.second
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
            Crossfade(signActivityStatus) { c ->
                if (c != null && c != ChaoxingSignActivityStatus.READY_TO_SIGN) {
                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        NotReadyToSignNoticeComponent(onSignForOtherUser = {
                            signActivityStatus = ChaoxingSignActivityStatus.READY_TO_SIGN
                            isCurrentAlreadySigned = true
                        }, onDismiss = {
                            signActivityStatus = ChaoxingSignActivityStatus.READY_TO_SIGN
                        }) { navBack() }

                        if (destination.startTime != null)
                            SignPotentialWarningTips(
                                destination.startTime,
                                destination.endTime,
                                destination.isLate
                            )
                    }
                } else if (c == ChaoxingSignActivityStatus.READY_TO_SIGN) {
                    var isSelfForSign by remember { mutableStateOf(false) }
                    var isQRCodeScanning by remember { mutableStateOf(false) }
                    val isQRCodeScanPause = remember { mutableStateOf(false) }
                    val isQRCodeParsing = remember { mutableStateOf(false) }
                    var isQRCodeIllegal by remember { mutableStateOf(false) }
                    var isMapGetting by remember { mutableStateOf(false) }
                    var qrcodeIllegalText by remember { mutableStateOf("二维码不合法") }
                    var signUserList by remember {
                        mutableStateOf<List<ChaoxingOtherUserSession?>>(
                            emptyList()
                        )
                    }
                    var isCaptcha = remember { false }
                    var isFirstOtherUserForSign = remember { true }
                    var locationData by remember { mutableStateOf<ChaoxingLocationSignEntity?>(null) }
                    var job by remember { mutableStateOf<Job?>(null) }
                    val userSelections = remember { mutableStateListOf(true) }
                    val signStatus =
                        remember { mutableStateListOf(ChaoxingSignStatus(hapticFeedback)) }
                    var isSigning by remember { mutableStateOf(false) }
                    var isSponsor by remember { mutableStateOf(false) }
                    if (isSponsor) {
                        SponsorPopupDialog()
                    }

                    // TODO: 人脸识别
                    var isFaceRequired by remember { mutableStateOf(false) }
                    var isFaceImageCaptured by remember { mutableStateOf(false) }

                    val faceImageBitmaps = remember { mutableMapOf<String, Bitmap>() }
                    val faceImageObjectIds = remember { mutableMapOf<String, String>() }
                    LaunchedEffect(Unit) {
                        isFaceRequired = signer.isFaceRequired()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp, 8.dp, 8.dp, 0.dp)
                        ) {
                            OtherUserSelectorComponent(
                                navToOtherUser = {
                                    navToOtherUser()
                                },
                                signStatus = signStatus,
                                isCurrentAlreadySigned = isCurrentAlreadySigned,
                                userSelections = userSelections,
                                isSigning = isSigning,
                                prefixTipsContent = {
                                    if (signoffData != null)
                                        SignOutRedirectTips(
                                            signoffData!!
                                        ) {
                                            navToOtherSign(it)
                                        }
                                    Card(
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
                                                "自己不在课堂现场时，必须需要另一名在场的用户为你代签，随地大小签不支持破解二维码签到。\n为很多人进行二维码代签时，可能会出现部分用户因为二维码超时失效导致的签到失败，请尝试多次扫码以完成签到。",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.W500
                                            )
                                        }
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
                                signUserList = otherUserSessionList
                                if (isFaceRequired)
                                    isFaceImageCaptured = true
                                else if (isMapRequired)
                                    isMapGetting = true
                                else {
                                    isQRCodeScanPause.value = false
                                    isQRCodeParsing.value = false
                                    isQRCodeScanning = true
                                }
                            }
                        }
                        AnimatedVisibility(
                            isFaceImageCaptured,
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
                            FaceRecognitionComponent(mutableListOf<Pair<String, String>>().apply {
                                if (isSelfForSign && !faceImageObjectIds.containsKey(
                                        ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                    )
                                ) add(ChaoxingHttpClient.instance!!.userEntity.phoneNumber to ChaoxingHttpClient.instance!!.userEntity.name)
                                signUserList.forEach {
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
                                isFaceImageCaptured = false
                                if (isMapRequired)
                                    isMapGetting = true
                                else {
                                    isQRCodeScanPause.value = false
                                    isQRCodeParsing.value = false
                                    isQRCodeScanning = true
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)
                        ) {
                            AnimatedVisibility(
                                isMapGetting,
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
                                GetLocationComponent(confirmButtonText = {
                                    Text("设置")
                                }) {
                                    isMapGetting = false
                                    isQRCodeScanPause.value = false
                                    isQRCodeParsing.value = false
                                    isQRCodeScanning = true
                                    locationData = it
                                }
                                BackHandler(isMapGetting) {
                                    isSigning = false
                                    isMapGetting = false
                                }
                            }
                            AnimatedVisibility(
                                isQRCodeScanning, enter =
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
                                            fadeOut(animationSpec = tween(300))
                            ) {
                                BackHandler(isQRCodeScanning) {
                                    isSigning = false
                                    isQRCodeScanning = false
                                    isQRCodeParsing.value = false
                                    isQRCodeScanPause.value = false
                                    isQRCodeIllegal = false
                                }
                                QRCodeScanComponent(isQRCodeScanPause, isQRCodeParsing, onClose = {
                                    isSigning = false
                                    isQRCodeParsing.value = false
                                    isQRCodeScanPause.value = false
                                    isQRCodeIllegal = false
                                    isQRCodeScanning = false
                                }, onScanResult = { result ->
                                    isSigning = true
                                    isQRCodeScanPause.value = true
                                    isQRCodeParsing.value = true
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            runCatching {
                                                return@runCatching signer.parseQRCode(result)
                                            }.onSuccess { enc ->
                                                isQRCodeScanning = false
                                                if (isSelfForSign) {
                                                    runCatching {
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
                                                                enc,
                                                                locationData,
                                                                faceImageUploadedObjectId
                                                            )
                                                        ) {
                                                            isCaptcha = true
                                                            suspendCancellableCoroutine { continuation ->
                                                                captchaValidateParams =
                                                                    signer to { validateValue ->
                                                                        validateValue.onSuccess {
                                                                            signer.signWithCaptcha(
                                                                                enc,
                                                                                locationData,
                                                                                validateValue.getOrThrow(),
                                                                                faceImageUploadedObjectId
                                                                            )
                                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                signStatus[0].successForLate()
                                                                            else
                                                                                signStatus[0].success()
                                                                            userSelections[0] =
                                                                                false
                                                                            UMengHelper.onSignQRCodeEvent(
                                                                                context,
                                                                                ChaoxingHttpClient.instance!!.userEntity.name
                                                                            )
                                                                            if (signUserList.all { it == null }) {
                                                                                isSigning = false
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
                                                                        }.onFailure {
                                                                            it.snackbarReport(
                                                                                snackbarHost,
                                                                                coroutineScope,
                                                                                "验证码校验失败",
                                                                                hapticFeedback
                                                                            )
                                                                            signStatus[0].failed(it)
                                                                        }
                                                                        continuation.resume(Unit)
                                                                    }
                                                            }
                                                        } else {
                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                signStatus[0].successForLate()
                                                            else
                                                                signStatus[0].success()
                                                            userSelections[0] = false
                                                            UMengHelper.onSignQRCodeEvent(
                                                                context,
                                                                ChaoxingHttpClient.instance!!.userEntity.name
                                                            )
                                                            if (signUserList.isEmpty()) {
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
                                                    }.onFailure { err ->
                                                        err.snackbarReport(
                                                            snackbarHost,
                                                            coroutineScope,
                                                            "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                                                            hapticFeedback
                                                        )
                                                        err.ifAlreadySigned {
                                                            userSelections[0] = false
                                                        }
                                                        if (signUserList.all { it == null }) {
                                                            isSigning = false
                                                            coroutineScope.launch {
                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                    context,
                                                                    destination.classId,
                                                                    destination.courseId,
                                                                    ChaoxingHttpClient.instance!!
                                                                )
                                                            }
                                                            if (userSelections.all { !it })
                                                                coroutineScope.launch {
                                                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                    isSponsor = true
                                                                }
                                                        }
                                                        signStatus[0].failed(err)
                                                    }
                                                }

                                                signUserList.forEachIndexed { index, session ->
                                                    if (session == null) return@forEachIndexed
                                                    runCatching {
                                                        signStatus[1 + index].loading()
                                                        if (!isCaptcha || (isSelfForSign && isFirstOtherUserForSign))
                                                            delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                                        isFirstOtherUserForSign = false
                                                        ChaoxingHttpClient.loadFromOtherUserSession(
                                                            session, context
                                                        ).also { client ->
                                                            ChaoxingQRCodeSigner(
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
                                                                                    session.phoneNumber
                                                                                ) {
                                                                                    ChaoxingCloudDriveHelper.uploadImage(
                                                                                        client,
                                                                                        faceImageBitmaps.remove(
                                                                                            session.phoneNumber
                                                                                        )!!
                                                                                    )
                                                                                }
                                                                            } else null
                                                                        if (sign(
                                                                                enc,
                                                                                locationData,
                                                                                faceImageUploadedObjectId
                                                                            )
                                                                        ) {
                                                                            isCaptcha = true
                                                                            suspendCancellableCoroutine { continuation ->
                                                                                captchaValidateParams =
                                                                                    this@apply to { validateValue ->
                                                                                        validateValue.onSuccess {
                                                                                            signWithCaptcha(
                                                                                                enc,
                                                                                                locationData,
                                                                                                validateValue.getOrThrow(),
                                                                                                faceImageUploadedObjectId
                                                                                            )
                                                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                                signStatus[1 + index].successForLate()
                                                                                            else
                                                                                                signStatus[1 + index].success()
                                                                                            UMengHelper.onSignQRCodeEvent(
                                                                                                context,
                                                                                                session.name,
                                                                                                true
                                                                                            )
                                                                                            userSelections[1 + index] =
                                                                                                false
                                                                                            if (signUserList.checkIsLast(
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
                                                                                                signStatus[1 + index].failed(
                                                                                                    it
                                                                                                )
                                                                                            }
                                                                                        continuation.resume(
                                                                                            Unit
                                                                                        )
                                                                                    }
                                                                            }
                                                                        } else {
                                                                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                                signStatus[1 + index].successForLate()
                                                                            else
                                                                                signStatus[1 + index].success()
                                                                            UMengHelper.onSignQRCodeEvent(
                                                                                context,
                                                                                session.name,
                                                                                true
                                                                            )
                                                                            userSelections[1 + index] =
                                                                                false
                                                                            if (signUserList.checkIsLast(
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
                                                                                isSponsor =
                                                                                    true
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }.onFailure { err ->
                                                        err.snackbarReport(
                                                            snackbarHost,
                                                            coroutineScope,
                                                            "为${session.name}签到失败",
                                                            hapticFeedback
                                                        )
                                                        err.ifAlreadySigned {
                                                            userSelections[1 + index] = false
                                                        }
                                                        if (index == signUserList.size - 1) {
                                                            isSigning = false
                                                            if (userSelections.all { !it })
                                                                coroutineScope.launch {
                                                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                    isSponsor =
                                                                        true
                                                                }
                                                        }
                                                        signStatus[1 + index].failed(err)
                                                    }
                                                }
                                                isSigning = false
                                            }.onFailure {
                                                it.printStackTrace()
                                                (it as? ChaoxingQRCodeSigner.QRCodeParseException).let { exception ->
                                                    if (exception == null)
                                                        Sentry.captureException(it)
                                                    else {
                                                        if (isDevelopedMode)
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    exception.rawValue,
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        Log.w(
                                                            "ChaoxingQRCodeSigner",
                                                            exception.rawValue
                                                        )
                                                    }
                                                }
                                                isQRCodeIllegal = true
                                                isQRCodeScanPause.value = true
                                                qrcodeIllegalText =
                                                    it.message ?: "二维码解析失败，不是正确码。"
                                                job?.cancel()
                                                job = coroutineScope.launch {
                                                    delay(1000)
                                                    isQRCodeScanPause.value = false
                                                    delay(1000)
                                                    isQRCodeIllegal = false
                                                }
                                            }
                                        }
                                    }
                                }) {
                                    Column(
                                        modifier = Modifier
                                            .offset(y = Dp(resources.displayMetrics.run {
                                                0.75f * heightPixels / density
                                            }) - 48.dp)
                                            .zIndex(2f)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Crossfade(isQRCodeIllegal) {
                                            when (it) {
                                                true -> {
                                                    Row(
                                                        modifier = Modifier
                                                            .background(
                                                                Color(0x72F1441D),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .border(
                                                                BorderStroke(
                                                                    2.dp,
                                                                    Color(0xFFF1441D)
                                                                ),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_octagon_alert),
                                                            contentDescription = "Illegal QR Code"
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text(qrcodeIllegalText)
                                                    }
                                                }

                                                false -> {
                                                    Row(
                                                        modifier = Modifier
                                                            .background(
                                                                Color(0x88888888),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .border(
                                                                BorderStroke(
                                                                    2.dp,
                                                                    Color(0xFF444444)
                                                                ),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_scan_qr_code),
                                                            contentDescription = "Scan QR Code"
                                                        )
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Text("扫描签到二维码")
                                                    }
                                                }
                                            }
                                        }
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