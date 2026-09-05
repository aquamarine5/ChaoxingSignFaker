/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCloudDriveHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClientPool
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerParams
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.FaceRecognitionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.FaceRecognitionNewFeatureTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NotReadyToSignNoticeComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SaveFaceImagesDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.toChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSignHandler
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.rememberFaceRecognitionData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@Immutable
@Serializable
data class GetLocationDestination(
    override val activeId: Long,
    override val classId: Int,
    override val courseId: Int,
    val extContent: String,
    val startTime: Long?,
    override val endTime: Long?,
    val isLate: Boolean,
    override val isCloneSession: Boolean
) : SignDestination {
    companion object {
        fun parseFromSignActivityEntity(
            activityEntity: ChaoxingSignActivityEntity,
            isLate: Boolean,
            isCloneSession: Boolean
        ): GetLocationDestination {
            return GetLocationDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext,
                activityEntity.startTime,
                activityEntity.endTime,
                isLate,
                isCloneSession
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
    val signer = remember {
        ChaoxingLocationSigner(
            ChaoxingHttpClient.instance!!,
            destination
        )
    }
    var isSponsor by remember { mutableStateOf(false) }
    if (isSponsor) {
        SponsorPopupDialog()
    }
    var captchaValidateParams by remember {
        mutableStateOf<CaptchaHandlerParams<ChaoxingLocationSigner>>(
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
    val faceRecognitionData = rememberFaceRecognitionData()
    var isFaceRequired by remember { mutableStateOf(false) }
    val isDisplayFaceRecognitionImageNewFeatureTips = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        context.chaoxingDataStore.data.first().let {
            isDisplayFaceRecognitionImageNewFeatureTips.value =
                !it.learntTooltips.saveFaceRecognitionImagesToLocal
        }
        isFetchedFailure = runCatching {
            val data = if (destination.isCloneSession) {
                ChaoxingHttpClient.cloneInstance!!.let { client ->
                    ChaoxingLocationSigner(
                        client,
                        destination
                    ).let {
                        isFaceRequired = it.isFaceRequired()
                        signActivityStatus = it.preSign()
                        it.getLocationSignInfo()
                    }
                }
            } else {
                isFaceRequired = signer.isFaceRequired()
                signActivityStatus = signer.preSign()
                signer.getLocationSignInfo()
            }
            signInfo = data.first
            signoffData = data.second
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
                    Box(modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 8.dp)) {
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
                    val isSigning = remember { mutableStateOf(false) }
                    var otherUserSessionForSignList by remember {
                        mutableStateOf<List<ChaoxingOtherUserSession?>>(
                            emptyList()
                        )
                    }
                    val userSelections = remember { mutableStateListOf(isSignForOther.not()) }
                    var isFaceImageCaptured by remember { mutableStateOf(false) }
                    var showFaceSaveDialog by remember { mutableStateOf(false) }
                    var sponsorPendingAfterFaceSave by remember { mutableStateOf(false) }
                    if (showFaceSaveDialog) {
                        SaveFaceImagesDialog(
                            faceRecognitionData,
                            otherUserSessionForSignList
                        ) {
                            if (sponsorPendingAfterFaceSave) {
                                coroutineScope.launch {
                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                    isSponsor = true
                                    sponsorPendingAfterFaceSave = false
                                }
                            }
                            showFaceSaveDialog = false
                        }
                    }
                    val signHandler = remember(isFaceRequired) {
                        ChaoxingSignHandler<ChaoxingLocationSignEntity>(
                            context = context, userSelections = userSelections,
                            signStatus = signStatus,
                            onSelfSigning = { value ->
                                val selfPhoneNumber =
                                    ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                runCatching {
                                    val faceImageUploadedObjectId =
                                        if (isFaceRequired) {
                                            faceRecognitionData.faceImageObjectIds.getOrPut(
                                                selfPhoneNumber
                                            ) {
                                                val bitmap =
                                                    faceRecognitionData.capturedBitmaps.remove(
                                                        selfPhoneNumber
                                                    )
                                                        ?: throw ChaoxingPredictableException(
                                                            "未拍摄人脸照片，无法上传"
                                                        )
                                                faceRecognitionData.signUsedFaceBitmaps[selfPhoneNumber] =
                                                    bitmap
                                                ChaoxingCloudDriveHelper.uploadImage(
                                                    ChaoxingHttpClient.instance!!,
                                                    bitmap
                                                )
                                            }
                                        } else null
                                    if (signer.sign(
                                            value,
                                            faceImageUploadedObjectId
                                        )
                                    ) {
                                        suspendCancellableCoroutine { continuation ->
                                            captchaValidateParams =
                                                signer to { captchaValidate ->
                                                    if (continuation.isActive)
                                                        continuation.resumeWith(captchaValidate.onSuccess {
                                                            signer.signWithCaptcha(
                                                                value,
                                                                it,
                                                                faceImageUploadedObjectId
                                                            )
                                                        })
                                                }
                                        }
                                        return@runCatching true
                                    } else return@runCatching false
                                }
                            },
                            onOtherUserSigning = { value, session, bypassChecking, _ ->
                                runCatching {
                                    ChaoxingHttpClientPool.get(context, session.phoneNumber)
                                        .let { client ->
                                            ChaoxingLocationSigner(
                                                client,
                                                if (isAlwaysForceSign || bypassChecking) destination.copy(
                                                    classId = ChaoxingCourseHelper.getClassIdFromCourseId(
                                                        client,
                                                        destination.courseId
                                                    ).getOrNull() ?: destination.classId
                                                ) else destination,
                                                signer.getSignInfo()
                                            ).run {
                                                if (!(isAlwaysForceSign || bypassChecking)) checkSignStatusThrowException()
                                                val faceImageUploadedObjectId =
                                                    if (isFaceRequired) {
                                                        faceRecognitionData.faceImageObjectIds.getOrPut(
                                                            session.phoneNumber
                                                        ) {
                                                            val bitmap =
                                                                faceRecognitionData.capturedBitmaps.remove(
                                                                    session.phoneNumber
                                                                )
                                                                    ?: throw ChaoxingPredictableException(
                                                                        "未拍摄${session.name}的人脸照片，无法上传"
                                                                    )
                                                            faceRecognitionData.signUsedFaceBitmaps[session.phoneNumber] =
                                                                bitmap
                                                            ChaoxingCloudDriveHelper.uploadImage(
                                                                client,
                                                                bitmap
                                                            )
                                                        }
                                                    } else null
                                                if (sign(value, faceImageUploadedObjectId)) {
                                                    suspendCancellableCoroutine { continuation ->
                                                        captchaValidateParams =
                                                            this to { captchaValidate ->
                                                                if (continuation.isActive) {
                                                                    continuation.resumeWith(
                                                                        runCatching {
                                                                            captchaValidate.onSuccess {
                                                                                signWithCaptcha(
                                                                                    value,
                                                                                    it,
                                                                                    faceImageUploadedObjectId
                                                                                )
                                                                            }.getOrThrow()
                                                                        })
                                                                }
                                                            }
                                                    }
                                                    return@runCatching true
                                                } else return@runCatching false
                                            }
                                        }
                                }
                            },
                            destination = destination,
                            onSigningFinished = { value, name, isOtherUser ->
                                coroutineScope.launch {
                                    UMengHelper.onSignLocationEvent(
                                        context,
                                        value,
                                        name,
                                        isOtherUser
                                    )
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setPreferences(
                                            it.preferences.toBuilder()
                                                .setLastSignedLocation(value.toChaoxingLocation())
                                                .build()
                                        ).build()
                                    }
                                }
                            },
                            onAllSigningFinished = { isSuccessful ->
                                isSigning.value = false
                                if (isSuccessful) {
                                    if (faceRecognitionData.newImagePhones.isNotEmpty()) {
                                        sponsorPendingAfterFaceSave = true
                                        showFaceSaveDialog = true
                                    } else {
                                        coroutineScope.launch {
                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                            isSponsor = true
                                        }
                                    }
                                }
                            },
                            faceRecognitionData = faceRecognitionData.takeIf { isFaceRequired }
                        )
                    }

                    Column(modifier = Modifier.padding(8.dp, 4.dp, 8.dp, 0.dp)) {
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
                                ;
                                if (isFaceRequired) {
                                    FaceRecognitionNewFeatureTips(
                                        isDisplayFaceRecognitionImageNewFeatureTips
                                    )
                                }
                            },
                            faceRecognitionData = faceRecognitionData.takeIf { isFaceRequired },
                            isCloneSession = destination.isCloneSession,
                            onRetrySignAction = { index, session, bypassChecking ->
                                signHandler.retryOtherUserSigning(session, index, bypassChecking)
                            }
                        ) { isSelf, otherUserSessionList, _ ->
                            isSigning.value = true
                            isSelfForSign = isSelf
                            otherUserSessionForSignList = otherUserSessionList
                            coroutineScope.launch {
                                if (isFaceRequired) {
                                    val selectedPhoneNumbers = buildList {
                                        if (isSelf) add(ChaoxingHttpClient.instance!!.userEntity.phoneNumber)
                                        addAll(
                                            otherUserSessionList.filterNotNull()
                                                .map { it.phoneNumber })
                                    }
                                    val storedImages =
                                        ChaoxingFaceHelper.storedFaceRecognitionImages.getValue(
                                            context
                                        )
                                    selectedPhoneNumbers.forEach { phoneNumber ->
                                        val images = storedImages[phoneNumber].orEmpty()
                                        val candidate =
                                            if (phoneNumber in faceRecognitionData.failedPhoneNumbers)
                                                images.filter {
                                                    !it.isFailureBefore && it.useCount > 0
                                                }.randomOrNull()
                                            else images.randomOrNull()
                                        candidate?.let { image ->
                                            faceRecognitionData.faceImageObjectIds[phoneNumber] =
                                                image.objectId
                                            faceRecognitionData.storedFaceImageObjectIds[phoneNumber] =
                                                image.objectId
                                        }
                                    }
                                }
                                if (isFaceRequired && (
                                            (isSelf && ChaoxingHttpClient.instance!!.userEntity.phoneNumber !in faceRecognitionData.faceImageObjectIds.keys) ||
                                                    otherUserSessionList.any { it != null && it.phoneNumber !in faceRecognitionData.faceImageObjectIds.keys }
                                            )
                                )
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
                        ),
                        exit = slideOutHorizontally(
                            animationSpec = tween(400),
                            targetOffsetX = { (it * 1.5).toInt() }
                        )
                    ) {
                        FaceRecognitionComponent(
                            mutableListOf<Pair<String, String>>().apply {
                                if (isSelfForSign && !faceRecognitionData.faceImageObjectIds.containsKey(
                                        ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                    )
                                ) add(ChaoxingHttpClient.instance!!.userEntity.phoneNumber to ChaoxingHttpClient.instance!!.userEntity.name)
                                otherUserSessionForSignList.forEach {
                                    if (it != null && !faceRecognitionData.faceImageObjectIds.containsKey(
                                            it.phoneNumber
                                        )
                                    ) add(it.phoneNumber to it.name)
                                }
                            }, onCancel = {
                                isSigning.value = false
                                isFaceImageCaptured = false
                            }) { bitmaps, isUseProfileImage ->
                            bitmaps.forEach { (string, bitmap) ->
                                faceRecognitionData.setStatus(
                                    if (isUseProfileImage) FaceRecognitionImageStatus.UseProfileImage
                                    else FaceRecognitionImageStatus.NewImageAdded,
                                    string,
                                    otherUserSessionForSignList
                                )
                                faceRecognitionData.capturedBitmaps[string] = bitmap
                                if (!isUseProfileImage) {
                                    faceRecognitionData.newImagePhones.add(string)
                                }
                            }
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
                            isSigning.value = false
                            isGetLocation = false
                        }
                        GetLocationComponent(signInfo, confirmButtonText = {
                            Text("签到")
                        }) { result ->
                            isGetLocation = false
                            signHandler.startSigning(
                                result,
                                isSelfForSign,
                                otherUserSessionForSignList,
                                hapticFeedback,
                                coroutineScope,
                                snackbarHost
                            )
                        }
                    }
                } else {
                    CenterCircularProgressIndicator()
                }
            }
        }
    }
}
