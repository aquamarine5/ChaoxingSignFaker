/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingFaceHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession

class FaceRecognitionData {
    val imageIconList: FaceRecognitionImageIconState = mutableStateOf(emptyList())
    val capturedBitmaps: SnapshotStateMap<String, Bitmap> = mutableStateMapOf()
    val newImagePhones: SnapshotStateSet<String> = mutableStateSetOf()
    val faceImageObjectIds: SnapshotStateMap<String, String> = mutableStateMapOf()
    val storedFaceImageObjectIds: SnapshotStateMap<String, String> = mutableStateMapOf()
    val failedPhoneNumbers: SnapshotStateList<String> = mutableStateListOf()
    val signUsedFaceBitmaps: SnapshotStateMap<String, Bitmap> = mutableStateMapOf()
    val failedImageInfos: SnapshotStateMap<String, Pair<String?, Bitmap?>> = mutableStateMapOf()

    fun resolveIndex(
        phoneNumber: String,
        otherUserSessionList: List<ChaoxingOtherUserSession?>
    ): Int {
        return if (phoneNumber == ChaoxingHttpClient.instance!!.userEntity.phoneNumber) 0
        else otherUserSessionList.indexOfFirst { it?.phoneNumber == phoneNumber }
            .let { if (it < 0) -1 else it + 1 }
    }

    fun setStatus(
        status: FaceRecognitionImageStatus,
        phoneNumber: String,
        otherUserSessionList: List<ChaoxingOtherUserSession?>
    ) {
        val index = resolveIndex(phoneNumber, otherUserSessionList)
        if (index in imageIconList.value.indices) imageIconList.value[index].value = status
    }

    fun currentStatus(
        phoneNumber: String,
        otherUserSessionList: List<ChaoxingOtherUserSession?>
    ): FaceRecognitionImageStatus? {
        val index = resolveIndex(phoneNumber, otherUserSessionList)
        return imageIconList.value.getOrNull(index)?.value
    }

    fun markFailure(
        phoneNumber: String,
        otherUserSessionList: List<ChaoxingOtherUserSession?>
    ) {
        failedImageInfos[phoneNumber] =
            (faceImageObjectIds[phoneNumber]
                ?: storedFaceImageObjectIds[phoneNumber]) to signUsedFaceBitmaps[phoneNumber]
        faceImageObjectIds.remove(phoneNumber)
        failedPhoneNumbers.add(phoneNumber)
        setStatus(FaceRecognitionImageStatus.ImageCheckFailure, phoneNumber, otherUserSessionList)
    }

    fun markSuccess(
        phoneNumber: String,
        otherUserSessionList: List<ChaoxingOtherUserSession?>
    ) {
        if (currentStatus(phoneNumber, otherUserSessionList) !=
            FaceRecognitionImageStatus.UseProfileImage
        ) {
            setStatus(
                FaceRecognitionImageStatus.ImageCheckSuccess,
                phoneNumber,
                otherUserSessionList
            )
        }
        failedPhoneNumbers.remove(phoneNumber)
        failedImageInfos.remove(phoneNumber)
    }

    suspend fun reportUsage(context: Context, phoneNumber: String, isFailure: Boolean) {
        storedFaceImageObjectIds[phoneNumber]?.let { objectId ->
            runCatching {
                ChaoxingFaceHelper.afterUsingFaceImage(
                    context,
                    phoneNumber,
                    objectId,
                    isFailure
                )
            }
        }
    }
}

@Composable
fun rememberFaceRecognitionData(): FaceRecognitionData = remember { FaceRecognitionData() }
