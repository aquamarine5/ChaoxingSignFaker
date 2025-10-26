/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingQRCodeSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class QRCodeSignDestination(
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
    navToOtherSign: (Any) -> Unit,
    navBack: () -> Unit
) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isCurrentAlreadySigned by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val signer = ChaoxingQRCodeSigner(ChaoxingHttpClient.instance!!, destination)
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
    if (captchaValidateParams != null) {
        CaptchaHandlerDialog(
            captchaValidateParams!!.first,
            captchaValidateParams!!.second,
            onDismiss = {
                captchaValidateParams = null
            })
    }
    LaunchedEffect(Unit) {
        runCatching {
            isAlreadySigned = signer.preSign()
            val data = signer.getQRCodeSignInfo()
            isMapRequired = data.first.isPositionRequired
            signoffData = data.second
        }.onFailure {
            it.snackbarReport(
                snackbarHost,
                coroutineScope,
                "获取签到信息失败", hapticFeedback
            )
            navBack()
        }
    }
    Crossfade(isAlreadySigned) { v ->
        when (v) {
            true -> {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    SignPotentialWarningTips(
                        destination.startTime,
                        destination.endTime,
                        destination.isLate
                    )

                    AlreadySignedNotice(onSignForOtherUser = {
                        isAlreadySigned = false
                        isCurrentAlreadySigned = true
                    }, onDismiss = {
                        isAlreadySigned = false
                    }) { navBack() }
                }
            }

            false -> {
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
                val signStatus = remember { mutableStateListOf(ChaoxingSignStatus(hapticFeedback)) }
                var isSigning by remember { mutableStateOf(false) }
                var isSponsor by remember { mutableStateOf(false) }
                if (isSponsor) {
                    SponsorPopupDialog()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
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
                                            buildAnnotatedString {
                                                append("通常情况下，")
                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                    append("随地大小签")
                                                }
                                                append(" 的二维码签到功能是用于给其他用户签到的，而不是用于仅给自己签到。")
                                            },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.W500
                                        )
                                    }
                                }
                                SignPotentialWarningTips(
                                    destination.startTime,
                                    destination.endTime,
                                    destination.isLate
                                )
                            }
                        ) { isSelf, otherUserSessionList, indexList ->
                            isSigning = true
                            isSelfForSign = isSelf
                            signUserList = otherUserSessionList
                            if (isMapRequired)
                                isMapGetting = true
                            else isQRCodeScanning = true
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
                                isQRCodeScanning = true
                                isQRCodeScanPause.value = false
                                isQRCodeParsing.value = false
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
                                ), exit =
                                scaleOut(targetScale = 0.8f, animationSpec = tween(300)) + fadeOut(
                                    animationSpec = tween(300)
                                )
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
                            }, onScanResult = {
                                isSigning = true
                                isQRCodeScanPause.value = true
                                isQRCodeParsing.value = true
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            return@runCatching signer.parseQRCode(it)
                                        }.onSuccess { enc ->
                                            isQRCodeScanning = false
                                            if (isSelfForSign) {
                                                runCatching {
                                                    signStatus[0].loading()
                                                    if (signer.sign(enc, locationData)) {
                                                        isCaptcha = true
                                                        suspendCoroutine { continuation ->
                                                            captchaValidateParams =
                                                                signer to { validateValue ->
                                                                    validateValue.onSuccess {
                                                                        signer.signWithCaptcha(
                                                                            enc,
                                                                            locationData,
                                                                            validateValue.getOrThrow()
                                                                        )
                                                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                            signStatus[0].successForLate()
                                                                        else
                                                                            signStatus[0].success()
                                                                        userSelections[0] = false
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
                                                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
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
                                                        "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败", hapticFeedback
                                                    )
                                                    err.ifAlreadySigned {
                                                        userSelections[0] = false
                                                        if (signUserList.all { it == null } && userSelections.all { !it }) {
                                                            isSigning = false
                                                            coroutineScope.launch {
                                                                ChaoxingRecommendHelper.recordRecommendEvent(
                                                                    context,
                                                                    destination.classId,
                                                                    destination.courseId,
                                                                    ChaoxingHttpClient.instance!!
                                                                )
                                                            }
                                                            coroutineScope.launch {
                                                                delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                isSponsor = true
                                                            }
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
                                                            client, destination
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
                                                                if (sign(enc, locationData)) {
                                                                    isCaptcha = true
                                                                    suspendCoroutine { continuation ->
                                                                        captchaValidateParams =
                                                                            this@apply to { validateValue ->
                                                                                validateValue.onSuccess {
                                                                                    signWithCaptcha(
                                                                                        enc,
                                                                                        locationData,
                                                                                        validateValue.getOrThrow()
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
                                                                                }.onFailure {
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
                                                                        delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                                                        isSponsor =
                                                                            true
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }.onFailure { err ->
                                                    err.snackbarReport(
                                                        snackbarHost,
                                                        coroutineScope,
                                                        "为${session.name}签到失败", hapticFeedback
                                                    )
                                                    err.ifAlreadySigned {
                                                        userSelections[1 + index] = false
                                                        if (index == signUserList.size - 1 && userSelections.all { !it }) {
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
                                        }.onFailure {
                                            it.printStackTrace()
                                            isQRCodeIllegal = true
                                            isQRCodeScanPause.value = true
                                            qrcodeIllegalText =
                                                it.message ?: "二维码解析失败，不是正确码。"
                                            job?.cancel()
                                            job = coroutineScope.launch {
                                                delay(3000)
                                                isQRCodeScanPause.value = false
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
                                        }))
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
                                                            BorderStroke(2.dp, Color(0xFFF1441D)),
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
                                                            BorderStroke(2.dp, Color(0xFF444444)),
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
            }

            null -> {
                CenterCircularProgressIndicator()
            }
        }
    }
}