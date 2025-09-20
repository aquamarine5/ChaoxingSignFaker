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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
data class GetLocationDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String,
    val startTime:Long,
    val endTime:Long?,
    val isLate: Boolean
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity,isLate: Boolean): GetLocationDestination {
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
    navToOtherSign: (Any) -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isSignForOther by remember { mutableStateOf(false) }
    var signInfo by remember { mutableStateOf<ChaoxingLocationDetailEntity?>(null) }
    val signer = ChaoxingLocationSigner(ChaoxingHttpClient.instance!!, destination)
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
    LaunchedEffect(Unit) {
        runCatching {
            val data = signer.getLocationSignInfo()
            signInfo = data.first
            signoffData = data.second
            isAlreadySigned = signer.preSign()
        }.onFailure {
            it.snackbarReport(
                snackbarHost,
                coroutineScope,
                "获取签到信息失败",
                hapticFeedback
            )
            navToCourseDetailDestination()
        }
    }

    Crossfade(isAlreadySigned) { v ->
        when (v) {
            true -> {
                Column(modifier = Modifier.padding(8.dp, 0.dp)) {
                    SignPotentialWarningTips(destination.startTime, destination.endTime,destination.isLate)
                    AlreadySignedNotice(onSignForOtherUser = {
                        isAlreadySigned = false
                        isSignForOther = true
                    }, onDismiss = {
                        isAlreadySigned = false
                    }) { navToCourseDetailDestination() }
                }
            }

            false -> {
                var isGetLocation by remember { mutableStateOf(false) }
                val signStatus = remember { mutableListOf(ChaoxingSignStatus(hapticFeedback)) }
                var isSelfForSign by remember { mutableStateOf(false) }
                var isSigning by remember { mutableStateOf(false) }
                var otherUserSessionForSignList by
                remember { mutableStateOf<List<ChaoxingOtherUserSession?>>(emptyList()) }
                val userSelections = remember { mutableStateListOf(isSignForOther.not()) }
                Column(modifier = Modifier.padding(8.dp)) {
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
                            SignPotentialWarningTips(destination.startTime, destination.endTime,destination.isLate)
                        }
                    ) { isSelf, otherUserSessionList, _ ->
                        isSigning = true
                        isSelfForSign = isSelf
                        otherUserSessionForSignList = otherUserSessionList
                        isGetLocation = true
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
                                    if (signer.sign(result)) {
                                        suspendCoroutine { continuation ->
                                            captchaValidateParams =
                                                signer to { captchaValidate ->
                                                    if (captchaValidate.isSuccess) {
                                                        signer.signWithCaptcha(
                                                            result,
                                                            captchaValidate.getOrThrow()
                                                        )
                                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                            signStatus[0].successForLate()
                                                        else
                                                            signStatus[0].success()
                                                        if (otherUserSessionForSignList.isEmpty()) {
                                                            isSigning = false
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
                                        if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                            signStatus[0].successForLate()
                                        else
                                            signStatus[0].success()
                                        if (otherUserSessionForSignList.isEmpty()) {
                                            isSigning = false
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
                                it.ifAlreadySigned {
                                    userSelections[0] = false
                                }
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "签到失败",
                                    hapticFeedback
                                )
                            }
                            otherUserSessionForSignList.forEachIndexed { index, userSession ->
                                if (userSession == null) return@forEachIndexed
                                runCatching {
                                    signStatus[index + 1].loading()
                                    delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                    ChaoxingHttpClient.loadFromOtherUserSession(
                                        userSession,
                                        context
                                    ).also { client ->
                                        ChaoxingLocationSigner(client, destination).apply {
                                            if (preSign()) {
                                                throw ChaoxingSigner.AlreadySignedException()
                                            } else {
                                                if(ChaoxingCourseHelper.checkClassValid(client,destination.classId)==false)
                                                    throw ChaoxingSigner.SignActivityNoPermissionException()
                                                if (sign(result)) {
                                                    suspendCoroutine { continuation ->
                                                        captchaValidateParams =
                                                            this@apply to { captchaValidate ->
                                                                if (captchaValidate.isSuccess) {
                                                                    this@apply.signWithCaptcha(
                                                                        result,
                                                                        captchaValidate.getOrThrow()
                                                                    )
                                                                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                        signStatus[1 + index].successForLate()
                                                                    else
                                                                        signStatus[1 + index].success()
                                                                    userSelections[index + 1] =
                                                                        false
                                                                    if (index == otherUserSessionForSignList.size - 1) {
                                                                        isSigning = false
                                                                        delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
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
                                                    if (index == otherUserSessionForSignList.size - 1) {
                                                        isSigning = false
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
                                }.onFailure { err ->
                                    err.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "为${userSession.name}签到失败",
                                        hapticFeedback
                                    )
                                    err.ifAlreadySigned {
                                        userSelections[index + 1] = false
                                    }
                                    signStatus[index + 1].failed(
                                        err
                                    )
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