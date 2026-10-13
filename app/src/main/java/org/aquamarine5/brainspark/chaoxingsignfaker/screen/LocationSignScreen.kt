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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.baidu.mapapi.model.CoordUtil
import com.baidu.mapapi.model.LatLng
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
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpRequesterPool
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CaptchaHandlerParams
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.DONT_SAVE_NEARBY_DISTANCE
import org.aquamarine5.brainspark.chaoxingsignfaker.components.FaceRecognitionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.FaceRecognitionNewFeatureTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NotReadyToSignNoticeComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SaveFaceImagesDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SaveFavoriteLocationDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignOutRedirectTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SignPotentialWarningTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorPopupDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.cloneSessionGuard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.toChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.components.toChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignResult
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSignHandler
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.FaceRecognitionImageStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.rememberFaceRecognitionData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.saveSelectedLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@Immutable
@Serializable
data class GetLocationDestination(
    override val activeId: Long,
    override val classId: Int,
    override val courseId: Long,
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
    if (!cloneSessionGuard(
            destination.isCloneSession,
            onCloneInvalid = navToCourseDetailDestination
        )
    ) return
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
    var selectedLocation by remember { mutableStateOf<ChaoxingLocationSignEntity?>(null) }
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    val faceRecognitionData = rememberFaceRecognitionData()
    var isFaceRequired by remember { mutableStateOf(false) }
    val isDisplayFaceRecognitionImageNewFeatureTips = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        context.chaoxingDataStore.data.first().let {
            isDisplayFaceRecognitionImageNewFeatureTips.value =
                !it.learntTooltips.saveFaceRecognitionImagesToLocal
            if (it.containsClassSelectedLocation(destination.classId)) {
                selectedLocation =
                    it.getClassSelectedLocationOrThrow(destination.classId)
                        .toChaoxingLocationSignEntity()
            }
        }
        isFetchedFailure = runCatching {
            val data = if (destination.isCloneSession) {
                ChaoxingHttpClient.cloneInstance!!.let { client ->
                    ChaoxingLocationSigner(
                        client,
                        destination
                    ).let {
                        isFaceRequired = it.isFaceRequired()
                        signActivityStatus = it.preSign(context)
                        it.getLocationSignInfo()
                    }
                }
            } else {
                isFaceRequired = signer.isFaceRequired()
                signActivityStatus = signer.preSign(context)
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
                        val data = if (destination.isCloneSession) {
                            ChaoxingHttpClient.cloneInstance!!.let { client ->
                                ChaoxingLocationSigner(
                                    client,
                                    destination
                                ).let {
                                    isFaceRequired = it.isFaceRequired()
                                    signActivityStatus = it.preSign(context)
                                    it.getLocationSignInfo()
                                }
                            }
                        } else {
                            isFaceRequired = signer.isFaceRequired()
                            signActivityStatus = signer.preSign(context)
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
                isFetchedFailure = null
            }
        } else {
            Crossfade(signActivityStatus, animationSpec = tween(700)) { c ->
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
                    var signedLocation by remember {
                        mutableStateOf<ChaoxingLocationSignEntity?>(
                            null
                        )
                    }
                    var isShowSaveFavoriteDialog by remember { mutableStateOf(false) }
                    if (isShowSaveFavoriteDialog) {
                        signedLocation?.let { signed ->
                            SaveFavoriteLocationDialog(
                                signed,
                                onDismiss = {
                                    isShowSaveFavoriteDialog = false
                                }
                            )
                        }
                    }
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
                                    ChaoxingHttpClient.instance!!.phoneNumber
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
                                            faceImageUploadedObjectId,
                                            context
                                        )
                                    ) {
                                        val resolution =
                                            suspendCancellableCoroutine { continuation ->
                                                captchaValidateParams =
                                                    signer to { captchaResult ->
                                                        if (continuation.isActive)
                                                            continuation.resumeWith(captchaResult)
                                                    }
                                            }
                                        signer.sign(
                                            value,
                                            faceImageUploadedObjectId,
                                            context,
                                            resolution.validate
                                        )
                                        return@runCatching ChaoxingSignResult(
                                            isCaptchaSigning = true,
                                            isCaptchaResolvedByModel = resolution.resolvedByModel
                                        )
                                    } else return@runCatching ChaoxingSignResult(
                                        isCaptchaSigning = false,
                                        isCaptchaResolvedByModel = false
                                    )
                                }.onFailure {
                                    if (it is ChaoxingSigner.WrongPositionException) selectedLocation =
                                        null
                                }
                            },
                            onOtherUserSigning = { value, session, bypassChecking, _ ->
                                runCatching {
                                    (if (isFaceRequired) ChaoxingHttpRequesterPool.getClient(
                                        context,
                                        session.phoneNumber
                                    ) else ChaoxingHttpRequesterPool.getRequester(
                                        context,
                                        session.phoneNumber
                                    ))
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
                                                if (!(isAlwaysForceSign || bypassChecking)) checkSignStatusThrowException(
                                                    context
                                                )
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
                                                if (sign(
                                                        value,
                                                        faceImageUploadedObjectId,
                                                        context
                                                    )
                                                ) {
                                                    val resolution =
                                                        suspendCancellableCoroutine { continuation ->
                                                            captchaValidateParams =
                                                                this to { captchaResult ->
                                                                    if (continuation.isActive) {
                                                                        continuation.resumeWith(
                                                                            captchaResult
                                                                        )
                                                                    }
                                                                }
                                                        }
                                                    sign(
                                                        value,
                                                        faceImageUploadedObjectId,
                                                        context,
                                                        resolution.validate
                                                    )
                                                    return@runCatching ChaoxingSignResult(
                                                        isCaptchaSigning = true,
                                                        isCaptchaResolvedByModel = resolution.resolvedByModel
                                                    )
                                                } else return@runCatching ChaoxingSignResult(
                                                    isCaptchaSigning = false,
                                                    isCaptchaResolvedByModel = false
                                                )
                                            }
                                        }
                                }.onFailure {
                                    if (it is ChaoxingSigner.WrongPositionException) selectedLocation =
                                        null
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
                                value.saveSelectedLocation(
                                    context, destination.classId, coroutineScope
                                ) {
                                    selectedLocation = it
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().apply {
                                            val signedLocationProto =
                                                value.toChaoxingLocation()
                                            setPreferences(
                                                it.preferences.toBuilder()
                                                    .setLastSignedLocation(signedLocationProto)
                                                    .build()
                                            )
                                            putClassRememberedLocation(
                                                destination.classId,
                                                signedLocationProto
                                            )
                                        }.build()
                                    }
                                }
                            },
                            onAllSigningFinished = { isSuccessful ->
                                isSigning.value = false
                                if (isSuccessful) {
                                    signedLocation?.let { target ->
                                        coroutineScope.launch {
                                            val isAlreadySaved =
                                                context.chaoxingDataStore.data.first().let { data ->
                                                    data.locationsList.any {
                                                        it.latitude == target.latitude && it.longitude == target.longitude
                                                    } || data.dontSaveNearbyPositionList.any {
                                                        CoordUtil.getDistance(
                                                            CoordUtil.ll2point(
                                                                LatLng(
                                                                    target.latitude,
                                                                    target.longitude
                                                                )
                                                            ),
                                                            CoordUtil.ll2point(
                                                                LatLng(it.latitude, it.longitude)
                                                            )
                                                        ) < DONT_SAVE_NEARBY_DISTANCE
                                                    }
                                                }
                                            if (isAlreadySaved.not()) {
                                                isShowSaveFavoriteDialog = true
                                            }
                                        }
                                    }
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
                    val startSigningWithSelectedLocation = {
                        val savedLocation = selectedLocation
                        if (savedLocation == null) isGetLocation = true
                        else signHandler.startSigning(
                            savedLocation,
                            isSelfForSign,
                            otherUserSessionForSignList,
                            hapticFeedback,
                            coroutineScope,
                            snackbarHost
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
                                if (selectedLocation != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "选定的签到位置：",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(selectedLocation!!.address)
                                        }
                                        Button(onClick = {
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.ContextClick
                                            )
                                            isGetLocation = true
                                        }) {
                                            Text("重新获取位置")
                                        }
                                    }
                                }
                            },
                            faceRecognitionData = faceRecognitionData.takeIf { isFaceRequired },
                            isCloneSession = destination.isCloneSession,
                            onRetrySignAction = { index, session, bypassChecking ->
                                signHandler.retryOtherUserSigning(
                                    session,
                                    index,
                                    bypassChecking,
                                    hapticFeedback,
                                    coroutineScope,
                                    snackbarHost
                                ).onFailure {
                                    if (it is ChaoxingSigner.WrongPositionException) selectedLocation =
                                        null
                                }
                            }
                        ) { isSelf, otherUserSessionList, _ ->
                            isSigning.value = true
                            isSelfForSign = isSelf
                            otherUserSessionForSignList = otherUserSessionList
                            coroutineScope.launch {
                                if (isFaceRequired) {
                                    val selectedPhoneNumbers = buildList {
                                        if (isSelf) add(ChaoxingHttpClient.instance!!.phoneNumber)
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
                                            (isSelf && ChaoxingHttpClient.instance!!.phoneNumber !in faceRecognitionData.faceImageObjectIds.keys) ||
                                                    otherUserSessionList.any { it != null && it.phoneNumber !in faceRecognitionData.faceImageObjectIds.keys }
                                            )
                                )
                                    isFaceImageCaptured = true
                                else startSigningWithSelectedLocation()
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
                                        ChaoxingHttpClient.instance!!.phoneNumber
                                    )
                                ) add(ChaoxingHttpClient.instance!!.phoneNumber to ChaoxingHttpClient.instance!!.name)
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
                            startSigningWithSelectedLocation()
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
                        GetLocationComponent(
                            signInfo,
                            classId = destination.classId,
                            confirmButtonText = {
                                Text("签到")
                            }) { result ->
                            isGetLocation = false
                            signedLocation = result
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
