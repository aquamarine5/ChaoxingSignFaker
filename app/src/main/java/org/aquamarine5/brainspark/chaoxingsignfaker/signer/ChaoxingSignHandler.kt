/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignResult
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingQRCodeSigner.QRCodeExpiredException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingCaptchaCancelledException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingFaceSignException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkIsLast
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ifShouldDeselect
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Immutable
class ChaoxingSignHandler<in T>(
    private val onSelfSigning: suspend (value: T) -> Result<ChaoxingSignResult>,
    private val onOtherUserSigning: suspend (value: T, session: ChaoxingOtherUserSession, bypassChecking: Boolean, index: Int) -> Result<ChaoxingSignResult>,
    private val destination: SignDestination,
    private val onSigningFinished: suspend (value: T, name: String, isOtherUser: Boolean) -> Unit,
    private val onAllSigningFinished: suspend (isSuccessful: Boolean) -> Unit,
    private val userSelections: SnapshotStateList<Boolean>,
    private val signStatus: MutableList<ChaoxingSignStatus>,
    private val context: Context,
    private val faceRecognitionData: FaceRecognitionData? = null,
    private val getSignRealtimeParameter: (suspend () -> T)? = null,
    private val signTimeSpan: Duration = LONG_SIGN_TIME_SPAN
) {
    companion object {
        val SHORT_SIGN_TIME_SPAN = 50.milliseconds
        val LONG_SIGN_TIME_SPAN = 200.milliseconds
    }

    class ChaoxingShouldSignOnceBeforeException :
        ChaoxingPredictableException("请先发起一次签到后再重试")

    private var storedValue: T? = null

    val hasSignRealtimeParameter: Boolean
        get() = getSignRealtimeParameter != null

    suspend fun retryOtherUserSigning(
        session: ChaoxingOtherUserSession,
        index: Int,
        bypassChecking: Boolean,
        hapticFeedback: HapticFeedback,
        coroutineScope: CoroutineScope,
        snackbarHost: SnackbarHostState
    ): Result<ChaoxingSignResult> {
        val realtimeParameterProvider = getSignRealtimeParameter
        val fallbackValue = storedValue
        return runCatching {
            val value = realtimeParameterProvider?.invoke() ?: fallbackValue
            ?: throw ChaoxingShouldSignOnceBeforeException()
            onOtherUserSigning(value, session, bypassChecking, index).getOrThrow()
                .also { signResult ->
                    if (signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel)
                        signStatus[1 + index].markCaptchaResolvedByModel()
                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                        signStatus[1 + index].successForLate()
                    else
                        signStatus[1 + index].success()
                    userSelections[index + 1] = false
                    onSigningFinished(value, session.name, true)
                }
        }.onFailure {
            if (it is CancellationException) throw it
            if (it is ChaoxingShouldSignOnceBeforeException) {
                signStatus[1 + index].isLoading.value = false
            } else {
                (it as? ChaoxingHttpClient.ChaoxingGetUserInfoException)?.let { exception ->
                    if (exception.isOtherUser)
                        ChaoxingOtherUserHelper.markSessionObsoleted(session, context)
                }
                signStatus[1 + index].failed(it)
                it.snackbarReport(
                    snackbarHost,
                    coroutineScope,
                    "重试签到失败",
                    hapticFeedback
                )
            }
        }
    }

    fun startContinuousSigning(
        isSelf: Boolean,
        otherUserSessionList: List<ChaoxingOtherUserSession?>,
        hapticFeedback: HapticFeedback,
        coroutineScope: CoroutineScope,
        snackbarHost: SnackbarHostState,
        getFreshEnc: suspend () -> T,
        onCurrentTargetChanged: (Int?) -> Unit
    ): Job {
        return coroutineScope.launch {
            var isCaptchaSigning = false
            var isCaptchaResolvedByModel = false
            val selfPhoneNumber = ChaoxingHttpClient.instance!!.phoneNumber
            val queue = buildList {
                if (isSelf) add(-1)
                otherUserSessionList.forEachIndexed { index, session ->
                    if (session != null) add(index)
                }
            }
            for (target in queue) {
                if (target == -1 && signStatus[0].isSuccess.value == true) continue
                if (target >= 0 && signStatus[target + 1].isSuccess.value == true) continue
                onCurrentTargetChanged(target)
                if (target == -1) {
                    signStatus[0].loading()
                    while (true) {
                        val value = getFreshEnc()
                        storedValue = value
                        val result = onSelfSigning(value)
                        val failure = result.exceptionOrNull()
                        if (failure is CancellationException) throw failure
                        if (result.isSuccess) {
                            val signResult = result.getOrNull()!!
                            isCaptchaSigning = signResult.isCaptchaSigning
                            isCaptchaResolvedByModel =
                                signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel
                            if (signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel)
                                signStatus[0].markCaptchaResolvedByModel()
                            userSelections[0] = false
                            faceRecognitionData?.markSuccess(selfPhoneNumber, otherUserSessionList)
                            faceRecognitionData?.reportUsage(context, selfPhoneNumber, false)
                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                                signStatus[0].successForLate()
                            else
                                signStatus[0].success()
                            onSigningFinished(
                                value,
                                ChaoxingHttpClient.instance!!.name,
                                false
                            )
                            break
                        } else {
                            val throwable = failure!!
                            if (throwable is QRCodeExpiredException) {
                                delay(500.milliseconds)
                                continue
                            }
                            if (throwable is ChaoxingFaceSignException)
                                faceRecognitionData?.markFailure(
                                    selfPhoneNumber,
                                    otherUserSessionList
                                )
                            faceRecognitionData?.reportUsage(
                                context,
                                selfPhoneNumber,
                                throwable is ChaoxingFaceSignException
                            )
                            signStatus[0].failed(throwable)
                            throwable.ifShouldDeselect {
                                userSelections[0] = false
                            }
                            if (throwable is ChaoxingSigner.WrongPositionException &&
                                throwable.isAlreadyDisabledRandomizedLocation
                            ) {
                                for (i in otherUserSessionList.indices) {
                                    if (otherUserSessionList[i] != null)
                                        signStatus[i + 1].failed(throwable)
                                }
                                snackbarHost.displaySnackbar(
                                    "签到位置超出范围，请重新选择位置",
                                    coroutineScope
                                )
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            } else if (throwable !is ChaoxingCaptchaCancelledException) {
                                throwable.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "为${ChaoxingHttpClient.instance!!.name}签到失败",
                                    hapticFeedback
                                )
                            }
                            onCurrentTargetChanged(null)
                            onAllSigningFinished(false)
                            return@launch
                        }
                    }
                } else {
                    val session = otherUserSessionList[target]!!
                    signStatus[target + 1].loading()
                    if (!isCaptchaSigning) delay(signTimeSpan)
                    else if (isCaptchaResolvedByModel) delay(SHORT_SIGN_TIME_SPAN)
                    while (true) {
                        val value = getFreshEnc()
                        storedValue = value
                        val result = onOtherUserSigning(value, session, false, target)
                        val failure = result.exceptionOrNull()
                        if (failure is CancellationException) throw failure
                        if (result.isSuccess) {
                            val signResult = result.getOrNull()!!
                            isCaptchaSigning = signResult.isCaptchaSigning
                            isCaptchaResolvedByModel =
                                signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel
                            if (signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel)
                                signStatus[1 + target].markCaptchaResolvedByModel()
                            if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                                signStatus[1 + target].successForLate()
                            else
                                signStatus[1 + target].success()
                            userSelections[target + 1] = false
                            faceRecognitionData?.markSuccess(
                                session.phoneNumber,
                                otherUserSessionList
                            )
                            faceRecognitionData?.reportUsage(context, session.phoneNumber, false)
                            onSigningFinished(value, session.name, true)
                            break
                        } else {
                            val throwable = failure!!
                            if (throwable is QRCodeExpiredException) {
                                delay(500.milliseconds)
                                continue
                            }
                            (throwable as? ChaoxingHttpClient.ChaoxingGetUserInfoException)?.let { exception ->
                                if (exception.isOtherUser) {
                                    signStatus[target + 1].markSessionObsoleted()
                                    ChaoxingOtherUserHelper.markSessionObsoleted(session, context)
                                }
                            }
                            if (throwable is ChaoxingFaceSignException)
                                faceRecognitionData?.markFailure(
                                    session.phoneNumber,
                                    otherUserSessionList
                                )
                            faceRecognitionData?.reportUsage(
                                context,
                                session.phoneNumber,
                                throwable is ChaoxingFaceSignException
                            )
                            signStatus[target + 1].failed(throwable)
                            throwable.ifShouldDeselect {
                                userSelections[target + 1] = false
                            }
                            if (throwable is ChaoxingSigner.WrongPositionException &&
                                throwable.isAlreadyDisabledRandomizedLocation
                            ) {
                                for (i in (target + 1)..<otherUserSessionList.size) {
                                    if (otherUserSessionList[i] != null)
                                        signStatus[i + 1].failed(throwable)
                                }
                                snackbarHost.displaySnackbar(
                                    "签到位置超出范围，请重新选择位置",
                                    coroutineScope
                                )
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            } else if (throwable !is ChaoxingCaptchaCancelledException) {
                                throwable.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "为${session.name}签到失败",
                                    hapticFeedback
                                )
                            }
                            onCurrentTargetChanged(null)
                            onAllSigningFinished(false)
                            return@launch
                        }
                    }
                }
            }
            onCurrentTargetChanged(null)
            onAllSigningFinished(true)
        }
    }

    fun startSigning(
        value: T,
        isSelf: Boolean,
        otherUserSessionList: List<ChaoxingOtherUserSession?>,
        hapticFeedback: HapticFeedback,
        coroutineScope: CoroutineScope,
        snackbarHost: SnackbarHostState
    ) {
        var isCaptchaSigning = false
        var isCaptchaResolvedByModel = false
        storedValue = value
        val selfPhoneNumber = ChaoxingHttpClient.instance!!.phoneNumber
        coroutineScope.launch {
            if (isSelf) {
                signStatus[0].loading()
                onSelfSigning(value).onSuccess { signResult ->
                    isCaptchaSigning = signResult.isCaptchaSigning
                    isCaptchaResolvedByModel =
                        signResult.isCaptchaSigning && signResult.isCaptchaResolvedByModel
                    if (isCaptchaResolvedByModel)
                        signStatus[0].markCaptchaResolvedByModel()
                    userSelections[0] = false
                    faceRecognitionData?.markSuccess(selfPhoneNumber, otherUserSessionList)
                    faceRecognitionData?.reportUsage(context, selfPhoneNumber, false)
                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                        signStatus[0].successForLate()
                    else
                        signStatus[0].success()
                    if (otherUserSessionList.all { it == null }) {
                        onAllSigningFinished(true)
                    }
                    onSigningFinished(value, ChaoxingHttpClient.instance!!.name, false)
                }.onFailure { throwable ->
                    if (throwable is ChaoxingFaceSignException)
                        faceRecognitionData?.markFailure(selfPhoneNumber, otherUserSessionList)
                    faceRecognitionData?.reportUsage(
                        context,
                        selfPhoneNumber,
                        throwable is ChaoxingFaceSignException
                    )
                    signStatus[0].failed(throwable)
                    throwable.ifShouldDeselect {
                        userSelections[0] = false
                    }
                    if (throwable is QRCodeExpiredException) {
                        for (i in otherUserSessionList.indices) {
                            if (otherUserSessionList[i] != null)
                                signStatus[i + 1].failed(throwable)
                        }
                        snackbarHost.displaySnackbar(
                            "签到二维码已过期，请重新扫码",
                            coroutineScope
                        )
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                        onAllSigningFinished(false)
                        return@launch
                    } else if (throwable is ChaoxingSigner.WrongPositionException &&
                        throwable.isAlreadyDisabledRandomizedLocation
                    ) {
                        for (i in otherUserSessionList.indices) {
                            if (otherUserSessionList[i] != null)
                                signStatus[i + 1].failed(throwable)
                        }
                        snackbarHost.displaySnackbar(
                            "签到位置超出范围，请重新选择位置",
                            coroutineScope
                        )
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                        onAllSigningFinished(false)
                        return@launch
                    } else {
                        if (throwable !is ChaoxingCaptchaCancelledException) {
                            throwable.snackbarReport(
                                snackbarHost,
                                coroutineScope,
                                "为${ChaoxingHttpClient.instance!!.name}签到失败",
                                hapticFeedback
                            )
                        }
                        if (otherUserSessionList.all { it == null }) {
                            onAllSigningFinished(userSelections.all { !it })
                        }
                    }
                }
            }
            var isFirstOtherUserForSign = true
            for ((index, session) in otherUserSessionList.withIndex()) {
                if (session == null) continue
                signStatus[index + 1].loading()
                if (!isCaptchaSigning) delay(signTimeSpan)
                else if (isCaptchaResolvedByModel) delay(SHORT_SIGN_TIME_SPAN)
                else if (isSelf && isFirstOtherUserForSign) delay(signTimeSpan)
                isFirstOtherUserForSign = false
                onOtherUserSigning(value, session, false, index).onSuccess {
                    isCaptchaSigning = it.isCaptchaSigning
                    isCaptchaResolvedByModel = it.isCaptchaSigning && it.isCaptchaResolvedByModel
                    if (isCaptchaResolvedByModel)
                        signStatus[1 + index].markCaptchaResolvedByModel()
                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                        signStatus[1 + index].successForLate()
                    else
                        signStatus[1 + index].success()
                    userSelections[index + 1] = false
                    faceRecognitionData?.markSuccess(session.phoneNumber, otherUserSessionList)
                    faceRecognitionData?.reportUsage(context, session.phoneNumber, false)
                    if (otherUserSessionList.checkIsLast(
                            index + 1
                        )
                    ) {
                        onAllSigningFinished(true)
                    }
                    onSigningFinished(value, session.name, true)
                }.onFailure { it ->
                    (it as? ChaoxingHttpClient.ChaoxingGetUserInfoException)?.let { exception ->
                        if (exception.isOtherUser) {
                            signStatus[index + 1].markSessionObsoleted()
                            ChaoxingOtherUserHelper.markSessionObsoleted(session, context)
                        }
                    }
                    if (it is ChaoxingFaceSignException)
                        faceRecognitionData?.markFailure(session.phoneNumber, otherUserSessionList)
                    faceRecognitionData?.reportUsage(
                        context,
                        session.phoneNumber,
                        it is ChaoxingFaceSignException
                    )
                    if (it !is ChaoxingCaptchaCancelledException) {
                        it.snackbarReport(
                            snackbarHost,
                            coroutineScope,
                            "为${session.name}签到失败",
                            hapticFeedback
                        )
                    }
                    it.ifShouldDeselect {
                        userSelections[index + 1] = false
                    }
                    signStatus[index + 1].failed(it)
                    if (it is QRCodeExpiredException) {
                        for (i in (index + 1)..<otherUserSessionList.size) {
                            if (otherUserSessionList[i] != null)
                                signStatus[i + 1].failed(it)
                        }
                        snackbarHost.displaySnackbar(
                            "签到二维码已过期，请重新扫码",
                            coroutineScope
                        )
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                        onAllSigningFinished(false)
                        return@launch
                    } else if (it is ChaoxingSigner.WrongPositionException &&
                        it.isAlreadyDisabledRandomizedLocation
                    ) {
                        for (i in (index + 1)..<otherUserSessionList.size) {
                            if (otherUserSessionList[i] != null)
                                signStatus[i + 1].failed(it)
                        }
                        snackbarHost.displaySnackbar(
                            "签到位置超出范围，请重新选择位置",
                            coroutineScope
                        )
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                        onAllSigningFinished(false)
                        return@launch
                    } else {
                        if (otherUserSessionList.checkIsLast(index + 1)) {
                            onAllSigningFinished(userSelections.all { !it })
                        }
                    }
                }
            }
        }
    }
}