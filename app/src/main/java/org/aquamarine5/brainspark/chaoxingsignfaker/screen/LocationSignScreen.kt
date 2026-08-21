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
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCloudDriveHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
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
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSignHandler
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingFaceSignException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageIconState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.setStatus
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
    val faceRecognitionImageIconList: FaceRecognitionImageIconState =
        remember { mutableStateOf(emptyList()) }
    var isFaceRequired by remember { mutableStateOf(false) }
    val isDisplayFaceRecognitionImageNewFeatureTips = remember { mutableStateOf(false) }
    val httpClientStorage = remember { mutableMapOf<String, ChaoxingHttpClient>() }
    LaunchedEffect(Unit) {
        context.chaoxingDataStore.data.first().let {
            isDisplayFaceRecognitionImageNewFeatureTips.value =
                !it.learntTooltips.saveFaceRecognitionImagesToLocal
        }
        isFetchedFailure = runCatching {
            val data = if (destination.isCloneSession) {
                ChaoxingHttpClient.cloneInstance!!.let { client ->
                    httpClientStorage.putIfAbsent(client.userEntity.phoneNumber, client)
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

                    // future will be edited.

                    var isFaceImageCaptured by remember { mutableStateOf(false) }

                    val faceImageObjectIds = remember { mutableMapOf<String, String>() }
                    val storedFaceImageObjectIds = remember { mutableMapOf<String, String>() }
                    val faceImageBitmaps = remember { mutableMapOf<String, Bitmap>() }
                    val newFaceImagePhones = remember { mutableStateSetOf<String>() }
                    var showFaceSaveDialog by remember { mutableStateOf(false) }
                    var sponsorPendingAfterFaceSave by remember { mutableStateOf(false) }
                    if (showFaceSaveDialog) {
                        SaveFaceImagesDialog(newFaceImagePhones.size, onSave = {
                            coroutineScope.launch {
                                newFaceImagePhones.toList().forEach { phoneNumber ->
                                    faceImageBitmaps[phoneNumber]?.let { bitmap ->
                                        runCatching {
                                            val client =
                                                if (phoneNumber == ChaoxingHttpClient.instance!!.userEntity.phoneNumber) ChaoxingHttpClient.instance!! else httpClientStorage[phoneNumber]!!
                                            ChaoxingFaceHelper.saveFaceImage(
                                                client,
                                                context,
                                                bitmap,
                                                phoneNumber
                                            )
                                            faceRecognitionImageIconList.setStatus(
                                                FaceRecognitionImageStatus.HaveImage,
                                                phoneNumber,
                                                otherUserSessionForSignList
                                            )
                                            faceImageBitmaps.remove(phoneNumber)
                                        }.onFailure {
                                            it.snackbarReport(
                                                snackbarHost,
                                                coroutineScope,
                                                "保存人脸照片失败",
                                                hapticFeedback
                                            )
                                        }
                                    }
                                }
                                newFaceImagePhones.clear()
                                if (sponsorPendingAfterFaceSave) {
                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                    isSponsor = true
                                    sponsorPendingAfterFaceSave = false
                                }
                                showFaceSaveDialog = false
                            }
                        }, onDismiss = {
                            newFaceImagePhones.clear()
                            if (sponsorPendingAfterFaceSave) {
                                sponsorPendingAfterFaceSave = false
                                coroutineScope.launch {
                                    delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                    isSponsor = true
                                }
                            }
                            showFaceSaveDialog = false
                        })
                    }
                    val signHandler = remember {
                        ChaoxingSignHandler<ChaoxingLocationSignEntity>(
                            context = context, userSelections = userSelections,
                            signStatus = signStatus,
                            onSelfSigning = { value ->
                                runCatching {
                                    val faceImageUploadedObjectId =
                                        if (isFaceRequired) {
                                            faceImageObjectIds.getOrPut(
                                                ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                            ) {
                                                ChaoxingCloudDriveHelper.uploadImage(
                                                    ChaoxingHttpClient.instance!!,
                                                    faceImageBitmaps[
                                                        ChaoxingHttpClient.instance!!.userEntity.phoneNumber
                                                    ]!!
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
                                }.onFailure { exception ->
                                    if (exception is ChaoxingFaceSignException) {
                                        faceImageObjectIds.remove(ChaoxingHttpClient.instance!!.userEntity.phoneNumber)
                                        faceRecognitionImageIconList.setStatus(
                                            FaceRecognitionImageStatus.ImageCheckFailure,
                                            0
                                        )
                                    }
                                    storedFaceImageObjectIds[ChaoxingHttpClient.instance!!.userEntity.phoneNumber]
                                        ?.let { objectId ->
                                            ChaoxingFaceHelper.afterUsingFaceImage(
                                                context,
                                                ChaoxingHttpClient.instance!!.userEntity.phoneNumber,
                                                objectId,
                                                exception is ChaoxingFaceSignException,
                                            )
                                        }
                                }.onSuccess {
                                    faceRecognitionImageIconList.setStatus(
                                        FaceRecognitionImageStatus.ImageCheckSuccess,
                                        0
                                    )
                                }
                            },
                            onOtherUserSigning = { value, session, bypassChecking, index ->
                                runCatching {
                                    httpClientStorage.getOrPut(session.phoneNumber) {
                                        ChaoxingHttpClient.loadFromOtherUserSession(
                                            session,
                                            context
                                        )
                                    }.let { client ->
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
                                                    faceImageObjectIds.getOrPut(
                                                        session.phoneNumber
                                                    ) {
                                                        ChaoxingCloudDriveHelper.uploadImage(
                                                            client,
                                                            faceImageBitmaps[
                                                                session.phoneNumber
                                                            ]!!
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
                                }.onFailure { exception ->
                                    if (exception is ChaoxingFaceSignException) {
                                        faceImageObjectIds.remove(session.phoneNumber)
                                        faceRecognitionImageIconList.setStatus(
                                            FaceRecognitionImageStatus.ImageCheckFailure,
                                            index + 1
                                        )
                                    }
                                    storedFaceImageObjectIds[session.phoneNumber]?.let { objectId ->
                                        ChaoxingFaceHelper.afterUsingFaceImage(
                                            context,
                                            session.phoneNumber,
                                            objectId,
                                            exception is ChaoxingFaceSignException,
                                        )
                                    }
                                }.onSuccess {
                                    faceRecognitionImageIconList.setStatus(
                                        FaceRecognitionImageStatus.ImageCheckSuccess,
                                        index + 1
                                    )
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
                            },
                            onAllSigningFinished = { isSuccessful ->
                                isSigning.value = false
                                if (isSuccessful) {
                                    if (newFaceImagePhones.isNotEmpty()) {
                                        sponsorPendingAfterFaceSave = true
                                        showFaceSaveDialog = true
                                    } else {
                                        coroutineScope.launch {
                                            delay(ChaoxingSignHelper.TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED)
                                            isSponsor = true
                                        }
                                    }
                                }
                            }
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

                                if (isFaceRequired) {
                                    FaceRecognitionNewFeatureTips(
                                        isDisplayFaceRecognitionImageNewFeatureTips
                                    )
                                }
                            },
                            faceRecognitionImageIconStatus = faceRecognitionImageIconList,
                            isCloneSession = destination.isCloneSession,
                            onIgnoreExceptionSignAction = { index, session ->
                                signHandler.ignoreExceptionOtherUserSigning(session, index)
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
                                        storedImages[phoneNumber].orEmpty().randomOrNull()
                                            ?.let { image ->
                                                faceImageObjectIds[phoneNumber] = image.objectId
                                                storedFaceImageObjectIds[phoneNumber] =
                                                    image.objectId
                                            }
                                    }
                                }
                                if (isFaceRequired && (
                                            (isSelf && ChaoxingHttpClient.instance!!.userEntity.phoneNumber !in faceImageObjectIds.keys) ||
                                                    otherUserSessionList.any { it != null && it.phoneNumber !in faceImageObjectIds.keys }
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
                                isSigning.value = false
                                isFaceImageCaptured = false
                            }) {
                            it.forEach { (string, bitmap) ->
                                faceRecognitionImageIconList.setStatus(
                                    FaceRecognitionImageStatus.NewImageAdded,
                                    string,
                                    otherUserSessionForSignList
                                )
                                faceImageBitmaps[string] = bitmap
                                newFaceImagePhones.add(string)
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