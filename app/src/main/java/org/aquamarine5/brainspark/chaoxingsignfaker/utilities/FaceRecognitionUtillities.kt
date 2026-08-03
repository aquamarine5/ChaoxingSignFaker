/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import androidx.annotation.DrawableRes
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession

typealias FaceRecognitionImageIconState = MutableState<List<MutableState<FaceRecognitionImageStatus>>>

enum class FaceRecognitionImageStatus(@DrawableRes val resId: Int, val color: Color) {
    HaveImage(R.drawable.ic_image_v, Color.Unspecified),
    NoImage(R.drawable.ic_image_slash, Color.Gray),
    ImageCheckSuccess(R.drawable.ic_image_check, Color(0xFF229453)),
    ImageCheckFailure(R.drawable.ic_image_times, Color(0xFFF43E06))
}

fun FaceRecognitionImageIconState.setStatus(
    status: FaceRecognitionImageStatus,
    phoneNumber: String,
    otherUserSessionList: List<ChaoxingOtherUserSession?>
) {
    val index = if (phoneNumber == ChaoxingHttpClient.instance!!.userEntity.phoneNumber) 0
        else otherUserSessionList.filterNotNull()
            .indexOfFirst { it.phoneNumber == phoneNumber }
            .let { if (it < 0) return else it + 1 }
    if (index in this.value.indices) this.value[index].value = status
}