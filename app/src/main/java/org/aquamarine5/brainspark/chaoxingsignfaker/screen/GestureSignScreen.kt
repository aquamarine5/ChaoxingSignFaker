/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.ihsg.patternlocker.OnPatternChangeListener
import com.github.ihsg.patternlocker.PatternLockerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
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
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingGestureSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Serializable
data class GestureSignDestination(
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
        ): GestureSignDestination {
            return GestureSignDestination(
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
fun GestureSignScreen(
    destination: GestureSignDestination,
    navToCourseDetailDestination: () -> Unit,
    navToOtherSign: (Any) -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isSignForOther by remember { mutableStateOf(false) }
    val signer = remember { ChaoxingGestureSigner(ChaoxingHttpClient.instance!!, destination) }
    var isSponsor by remember { mutableStateOf(false) }
    if (isSponsor) {
        SponsorPopupDialog()
    }
    var captchaValidateParams by remember {
        mutableStateOf<Pair<ChaoxingGestureSigner, suspend (Result<String>) -> Unit>?>(
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
            signoffData = signer.getGestureSignInfo()
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
                    SignPotentialWarningTips(
                        destination.startTime,
                        destination.endTime,
                        destination.isLate,
                        isPadding = true
                    )
                    AlreadySignedNotice(onSignForOtherUser = {
                        isAlreadySigned = false
                        isSignForOther = true
                    }, onDismiss = {
                        isAlreadySigned = false
                    }) { navToCourseDetailDestination() }
                }
            }

            false -> {
                var isCheckingStatus by remember { mutableStateOf(false) }
                var text by remember { mutableStateOf("") }
                var isCaptcha = remember { false }
                var isFirstOtherUserForSign = remember { true }
                val signStatus = remember { mutableListOf(ChaoxingSignStatus(hapticFeedback)) }
                val userSelections = remember { mutableStateListOf(isSignForOther.not()) }
                var isSigning by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(8.dp)) {
                    OtherUserSelectorComponent(
                        navToOtherUser = { navToOtherUserDestination() },
                        signStatus = signStatus,
                        isCurrentAlreadySigned = isSignForOther,
                        userSelections = userSelections,
                        isSigning = isSigning,
                        prefixTipsContent = {
                            if (signoffData != null)
                                SignOutRedirectTips(
                                    signoffData!!
                                ) {
                                    navToOtherSign(it)
                                }
                            SignPotentialWarningTips(
                                destination.startTime,
                                destination.endTime,
                                destination.isLate
                            )
                        },
                        suffixContent = {
                            Column {
                                Text(
                                    "请输入数字签到码：",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(0.dp, 6.dp)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val patternLockerView = remember {
                                        PatternLockerView(context).apply {
                                            setOnPatternChangedListener(object :
                                                OnPatternChangeListener {
                                                override fun onChange(
                                                    view: PatternLockerView,
                                                    hitIndexList: List<Int>
                                                ) {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.SegmentFrequentTick
                                                    )
                                                }

                                                override fun onClear(view: PatternLockerView) {
                                                    view.updateStatus(false)
                                                }

                                                override fun onComplete(
                                                    view: PatternLockerView,
                                                    hitIndexList: List<Int>
                                                ) {
                                                    val currentGestureOrderCode =
                                                        hitIndexList.joinToString("") { (it + 1).toString() }
                                                    coroutineScope.launch {
                                                        if (signer.checkSignGesture(
                                                                currentGestureOrderCode
                                                            )
                                                        ) {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.Confirm
                                                            )
                                                            isCheckingStatus = true
                                                            text = currentGestureOrderCode
                                                        } else {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.Reject
                                                            )
                                                            snackbarHost.displaySnackbar(
                                                                "签到码错误，请重新输入",
                                                                coroutineScope
                                                            )
                                                            view.updateStatus(true)
                                                            coroutineScope.launch {
                                                                delay(600L)
                                                                view.clearHitState()
                                                            }
                                                        }
                                                    }
                                                }

                                                override fun onStart(view: PatternLockerView) {
                                                    view.updateStatus(false)
                                                }
                                            })
                                        }
                                    }
                                    AndroidView({
                                        patternLockerView
                                    }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    ) { isSelf, otherUserSessionList, _ ->
                        if (!isCheckingStatus) {
                            coroutineScope.launch {
                                snackbarHost.currentSnackbarData?.dismiss()
                                snackbarHost.showSnackbar("请先输入正确的图案签到码")
                            }
                            return@OtherUserSelectorComponent
                        }
                        val code = text.toInt()
                        isSigning = true
                        coroutineScope.launch {
                            runCatching {
                                if (isSelf) {
                                    signStatus[0].loading()
                                    if (signer.sign(code)) {
                                        isCaptcha = true
                                        suspendCoroutine { continuation ->
                                            captchaValidateParams =
                                                signer to { captchaValue ->
                                                    captchaValue.onSuccess {
                                                        signer.signWithCaptcha(code, it)
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
                                                        UMengHelper.onSignCodeEvent(
                                                            context,
                                                            ChaoxingHttpClient.instance!!.userEntity.name
                                                        )
                                                    }.onFailure {
                                                        signStatus[0].failed(it)
                                                        it.snackbarReport(
                                                            snackbarHost,
                                                            coroutineScope,
                                                            "验证码校验失败",
                                                            hapticFeedback
                                                        )
                                                    }
                                                    continuation.resume(Unit)
                                                }
                                        }
                                    } else {
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
                                        UMengHelper.onSignGestureEvent(
                                            context,
                                            ChaoxingHttpClient.instance!!.userEntity.name
                                        )
                                    }
                                }
                            }.onFailure {
                                signStatus[0].failed(it)
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
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                                    hapticFeedback
                                )
                            }

                            otherUserSessionList.forEachIndexed { index, session ->
                                if (session == null) return@forEachIndexed
                                runCatching {
                                    signStatus[index + 1].loading()
                                    if (!isCaptcha || (isSelf && isFirstOtherUserForSign))
                                        delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                                    isFirstOtherUserForSign = false
                                    ChaoxingHttpClient.loadFromOtherUserSession(
                                        session,
                                        context
                                    ).also { client ->
                                        ChaoxingGestureSigner(client, destination).apply {
                                            if (preSign()) {
                                                throw ChaoxingSigner.AlreadySignedException()
                                            } else {
                                                if (ChaoxingCourseHelper.checkClassValid(
                                                        client,
                                                        destination.classId
                                                    ) == false
                                                )
                                                    throw ChaoxingSigner.SignActivityNoPermissionException()
                                                if (sign(code)) {
                                                    suspendCoroutine { continuation ->
                                                        captchaValidateParams =
                                                            this@apply to { captchaValidate ->
                                                                if (captchaValidate.isSuccess) {
                                                                    this@apply.signWithCaptcha(
                                                                        code,
                                                                        captchaValidate.getOrThrow()
                                                                    )
                                                                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime)
                                                                        signStatus[1 + index].successForLate()
                                                                    else
                                                                        signStatus[1 + index].success()
                                                                    userSelections[index + 1] =
                                                                        false
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
                                                                    UMengHelper.onSignGestureEvent(
                                                                        context,
                                                                        session.name,
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
                                                                            "为${session.name}签到时验证码校验失败",
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
                                                    if (otherUserSessionList.checkIsLast(1 + index)) {
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
                                                    UMengHelper.onSignGestureEvent(
                                                        context,
                                                        session.name,
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
                                        "为${session.name}签到失败",
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
                                    signStatus[index + 1].failed(err)
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