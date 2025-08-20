/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

@Serializable
data class GetLocationDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): GetLocationDestination {
            return GetLocationDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@Composable
fun LocationSignScreen(
    destination: GetLocationDestination,
    navToCourseDetailDestination: () -> Unit,
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
        mutableStateOf<Pair<ChaoxingLocationSigner, (Result<String>) -> Unit>?>(
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        runCatching {
            signInfo = signer.getLocationSignInfo()
            isAlreadySigned = signer.preSign()
        }.onFailure {
            if ((it is ChaoxingPredictableException).not()) {
                Sentry.captureException(it)
            }
            Toast.makeText(context, "获取签到事件详情失败", Toast.LENGTH_SHORT).show()
            navToCourseDetailDestination()
        }
    }
    Crossfade(isAlreadySigned) { v ->
        when (v) {
            true -> {
                AlreadySignedNotice(onSignForOtherUser = {
                    isAlreadySigned = false
                    isSignForOther = true
                }, onDismiss = {
                    isAlreadySigned = false
                }) { navToCourseDetailDestination() }
            }

            false -> {
                var isGetLocation by remember { mutableStateOf(false) }
                val signStatus = remember { mutableListOf(ChaoxingSignStatus()) }
                var isSelfForSign by remember { mutableStateOf(false) }
                val otherUserSessionForSignList =
                    remember { mutableListOf<ChaoxingOtherUserSession>() }

                OtherUserSelectorComponent(
                    navToOtherUser = { navToOtherUserDestination() },
                    signStatus = signStatus,
                    isCurrentAlreadySigned = isSignForOther,
                ) { isSelf, otherUserSessionList, _ ->
                    isSelfForSign = isSelf
                    otherUserSessionForSignList.addAll(otherUserSessionList)
                    isGetLocation = true
                }
                AnimatedVisibility(
                    isGetLocation, enter =
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(
                        animationSpec = tween(300)
                    ), exit =
                    slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) +
                            fadeOut(animationSpec = tween(300)), modifier = Modifier.zIndex(1f)
                ) {
                    BackHandler(isGetLocation) {
                        isGetLocation = false
                    }
                    GetLocationComponent(signInfo, confirmButtonText = {
                        Text("签到")
                    }) { result ->
                        isGetLocation = false
                        coroutineScope.launch {
                            if (isSelfForSign)
                                runCatching {
                                    signStatus[0].loading()
                                    signer.sign(result) {
                                        captchaValidateParams = signer to { captchaValidate ->
                                            if (captchaValidate.isSuccess) {
                                                coroutineScope.launch {
                                                    runCatching {
                                                        signer.signWithCaptcha(
                                                            result,
                                                            captchaValidate.getOrThrow()
                                                        )
                                                    }.onSuccess {
                                                        signStatus[0].success()
                                                        if (otherUserSessionForSignList.isEmpty()) {
                                                            isSponsor = true
                                                        }
                                                        UMengHelper.onSignLocationEvent(
                                                            context,
                                                            result,
                                                            ChaoxingHttpClient.instance!!.userEntity.name
                                                        )
                                                    }.onFailure {
                                                        signStatus[0].failed(it)
                                                        it.printStackTrace()
                                                        Toast.makeText(
                                                            context,
                                                            it.message,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        if ((it is ChaoxingPredictableException).not()) {
                                                            Sentry.captureException(it)
                                                        }
                                                    }
                                                }
                                            } else {
                                                (captchaValidate.exceptionOrNull()
                                                    ?: ChaoxingSigner.CaptchaException()).apply {
                                                    signStatus[0].failed(this)
                                                    Toast.makeText(
                                                        context,
                                                        this.message,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    this.printStackTrace()
                                                    if ((this is ChaoxingPredictableException).not()) {
                                                        Sentry.captureException(this)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }.onSuccess {
                                    if (captchaValidateParams != null)
                                        return@onSuccess
                                    signStatus[0].success()
                                    UMengHelper.onSignLocationEvent(
                                        context,
                                        result,
                                        ChaoxingHttpClient.instance!!.userEntity.name
                                    )
                                    if (otherUserSessionForSignList.isEmpty()) {
                                        isSponsor = true
                                    }
                                }.onFailure {
                                    signStatus[0].failed(it)
                                    it.printStackTrace()
                                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                    if ((it is ChaoxingPredictableException).not()) {
                                        Sentry.captureException(it)
                                    }
                                }


                            otherUserSessionForSignList.forEachIndexed { index, userSession ->
                                runCatching {
                                    signStatus[index + 1].loading()
                                    delay(1500)
                                    ChaoxingHttpClient.loadFromOtherUserSession(
                                        userSession,
                                        context
                                    ).also { client ->
                                        ChaoxingLocationSigner(client, destination).apply {
                                            if (preSign()) {
                                                signStatus[index + 1].failed(ChaoxingSigner.AlreadySignedException())
                                            } else {
                                                sign(result) {
                                                    captchaValidateParams =
                                                        this to { captchaValidate ->
                                                            if (captchaValidate.isSuccess) {
                                                                coroutineScope.launch {
                                                                    runCatching {
                                                                        signer.signWithCaptcha(
                                                                            result,
                                                                            captchaValidate.getOrThrow()
                                                                        )
                                                                    }.onSuccess {
                                                                        signStatus[index + 1].success()
                                                                        if (index == otherUserSessionForSignList.size - 1) {
                                                                            isSponsor = true
                                                                        }
                                                                        UMengHelper.onSignLocationEvent(
                                                                            context,
                                                                            result,
                                                                            userSession.name,
                                                                            true
                                                                        )
                                                                    }.onFailure {
                                                                        it.printStackTrace()
                                                                        if ((it is ChaoxingPredictableException).not()) {
                                                                            Sentry.captureException(
                                                                                it
                                                                            )
                                                                        }
                                                                        signStatus[index + 1].failed(
                                                                            it
                                                                        )
                                                                    }
                                                                }
                                                            } else {
                                                                (captchaValidate.exceptionOrNull()
                                                                    ?: ChaoxingSigner.CaptchaException()).apply {
                                                                    signStatus[index + 1].failed(
                                                                        this
                                                                    )
                                                                    Toast.makeText(
                                                                        context,
                                                                        this.message,
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    this.printStackTrace()
                                                                    if ((this is ChaoxingPredictableException).not()) {
                                                                        Sentry.captureException(this)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                                }.onSuccess {
                                    if (captchaValidateParams != null) return@onSuccess
                                    signStatus[index + 1].success()
                                    if (index == otherUserSessionForSignList.size - 1) {
                                        isSponsor = true
                                    }
                                    UMengHelper.onSignLocationEvent(
                                        context,
                                        result,
                                        userSession.name,
                                        true
                                    )
                                }.onFailure {
                                    it.printStackTrace()
                                    if ((it is ChaoxingPredictableException).not()) {
                                        Sentry.captureException(it)
                                    }
                                    signStatus[index + 1].failed(it)
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