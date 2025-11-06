/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifAlreadySigned
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPasswordSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@Serializable
data class PasswordSignDestination(
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
        ): PasswordSignDestination {
            return PasswordSignDestination(
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

enum class PasswordCodeStatus {
    ENTERED, INPUTTING, PENDING, INCORRECT, CORRECT
}

@Composable
fun PasswordSignScreen(
    destination: PasswordSignDestination,
    navToCourseDetailDestination: () -> Unit,
    navToOtherSign: (Any) -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isSignForOther by remember { mutableStateOf(false) }
    val signer = remember { ChaoxingPasswordSigner(ChaoxingHttpClient.instance!!, destination) }
    var isSponsor by remember { mutableStateOf(false) }
    var numberCount by remember { mutableIntStateOf(-1) }
    if (isSponsor) {
        SponsorPopupDialog()
    }
    var captchaValidateParams by remember {
        mutableStateOf<Pair<ChaoxingPasswordSigner, suspend (Result<String>) -> Unit>?>(
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
    var isCheckingSuccess by remember { mutableStateOf<Boolean?>(null) }
    var signoffData by remember { mutableStateOf<ChaoxingSignOutEntity?>(null) }
    val snackbarHost = LocalSnackbarHostState.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        runCatching {
            signer.getPasswordInfo().apply {
                numberCount = first
                signoffData = second
            }
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
                var text by remember { mutableStateOf("") }
                val focusManager = LocalFocusManager.current
                var isCaptcha = remember { false }
                var isFirstOtherUserForSign = remember { true }
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
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
                            var isCheckingStatus by remember { mutableStateOf<Boolean?>(null) }
                            LaunchedEffect(isCheckingStatus) {
                                delay(1000L)
                                if (isCheckingStatus == false)
                                    isCheckingStatus = null
                            }
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
                                    BasicTextField(
                                        value = text,
                                        singleLine = true,
                                        onValueChange = { newText ->
                                            if (newText.length <= numberCount && newText.all { it.isDigit() }) {
                                                text = newText
                                                if (newText.length == numberCount) {
                                                    coroutineScope.launch {
                                                        signer.checkSignCode(text.toInt()).let {
                                                            isCheckingSuccess = it
                                                            if (it) {
                                                                isCheckingStatus = true
                                                                hapticFeedback.performHapticFeedback(
                                                                    HapticFeedbackType.Confirm
                                                                )
                                                                focusManager.clearFocus()
                                                            } else {
                                                                isCheckingStatus = false
                                                                hapticFeedback.performHapticFeedback(
                                                                    HapticFeedbackType.Reject
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .onFocusChanged {
                                                if (it.isFocused)
                                                    keyboardController?.show()
                                            }
                                            .wrapContentHeight(),
                                        readOnly = isCheckingSuccess == true,
                                        decorationBox = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.Transparent),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                for (i in 0 until numberCount) {
                                                    key(i) {
                                                        val codeState by remember {
                                                            derivedStateOf {
                                                                when {
                                                                    isCheckingStatus == true -> PasswordCodeStatus.CORRECT
                                                                    isCheckingStatus == false -> PasswordCodeStatus.INCORRECT
                                                                    i < text.length -> PasswordCodeStatus.ENTERED
                                                                    i == text.length -> PasswordCodeStatus.INPUTTING
                                                                    else -> PasswordCodeStatus.PENDING
                                                                }
                                                            }
                                                        }
                                                        val animatedContainerColor by animateColorAsState(
                                                            when (codeState) {
                                                                PasswordCodeStatus.ENTERED -> Color(
                                                                    0xFF2196F3
                                                                )

                                                                PasswordCodeStatus.CORRECT -> Color(
                                                                    0xFF43B244
                                                                )

                                                                PasswordCodeStatus.INCORRECT -> Color(
                                                                    0xFFF43E06
                                                                )

                                                                PasswordCodeStatus.INPUTTING -> Color.White
                                                                PasswordCodeStatus.PENDING -> Color(
                                                                    0xFF9E9E9E
                                                                )
                                                            }
                                                        )
                                                        val animatedElevation by animateDpAsState(
                                                            when (codeState) {
                                                                PasswordCodeStatus.INPUTTING -> 15.dp
                                                                PasswordCodeStatus.PENDING -> 0.dp
                                                                else -> 7.dp
                                                            }
                                                        )
                                                        val animatedTextColor by animateColorAsState(
                                                            when (codeState) {
                                                                PasswordCodeStatus.ENTERED, PasswordCodeStatus.CORRECT, PasswordCodeStatus.INCORRECT -> Color.White
                                                                else -> Color.Gray
                                                            }
                                                        )
                                                        val cardElevation =
                                                            CardDefaults.cardElevation(
                                                                defaultElevation = animatedElevation
                                                            )
                                                        val cardColors = CardDefaults.cardColors(
                                                            containerColor = animatedContainerColor
                                                        )
                                                        Card(
                                                            modifier = Modifier.size((276 / numberCount).dp),
                                                            colors = cardColors,
                                                            elevation = cardElevation
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (codeState != PasswordCodeStatus.PENDING) {
                                                                    Text(
                                                                        text.getOrElse(i) { '_' }
                                                                            .toString(),
                                                                        style = TextStyle(
                                                                            fontSize = (144 / numberCount).sp,
                                                                            color = animatedTextColor,
                                                                            textAlign = TextAlign.Center
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { isSelf, otherUserSessionList, _ ->
                        if (isCheckingSuccess != true) {
                            coroutineScope.launch {
                                snackbarHost.currentSnackbarData?.dismiss()
                                snackbarHost.showSnackbar(
                                    "请先输入正确的数字签到码",
                                    withDismissAction = true
                                )
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
                                        UMengHelper.onSignCodeEvent(
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
                                        ChaoxingPasswordSigner(client, destination).apply {
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
                                                                    UMengHelper.onSignCodeEvent(
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
                                                    UMengHelper.onSignCodeEvent(
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