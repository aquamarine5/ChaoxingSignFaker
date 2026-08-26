/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.DrawableRes
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

typealias FaceRecognitionImageIconState = MutableState<List<MutableState<FaceRecognitionImageStatus>>>

private const val FACE_IMAGE_COMPRESS_QUALITY = 90

enum class FaceRecognitionImageStatus(@DrawableRes val resId: Int, val color: Color) {
    HaveImage(R.drawable.ic_image_v, Color.Unspecified),
    NoImage(R.drawable.ic_image_alt_slash, Color(0xFF888888)),
    UseProfileImage(R.drawable.ic_image_download, Color(0xFFFFA500)),
    ImageCheckSuccess(R.drawable.ic_image_check, Color(0xFF229453)),
    ImageCheckFailure(R.drawable.ic_image_times, Color(0xFFF43E06)),
    NewImageAdded(R.drawable.ic_image_plus, Color(0xFF00BCD4))
}

fun FaceRecognitionImageIconState.setStatus(
    status: FaceRecognitionImageStatus,
    phoneNumber: String,
    otherUserSessionList: List<ChaoxingOtherUserSession?>
) {
    val index = if (phoneNumber == ChaoxingHttpClient.instance!!.userEntity.phoneNumber) 0
    else otherUserSessionList.indexOfFirst { it?.phoneNumber == phoneNumber }
        .let { if (it < 0) return else it + 1 }
    if (index in this.value.indices) setStatus(status, index)
}

fun FaceRecognitionImageIconState.setStatus(
    status: FaceRecognitionImageStatus,
    index: Int
) {
    if (index in this.value.indices) this.value[index].value = status
}

suspend fun randomizeStylizeFaceImage(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
    val cropRatio = 0.90f + Random.nextFloat() * 0.09f
    val cropWidth = (bitmap.width * cropRatio).toInt().coerceAtLeast(1)
    val cropHeight = (bitmap.height * cropRatio).toInt().coerceAtLeast(1)
    val cropped = Bitmap.createBitmap(
        bitmap,
        Random.nextInt(bitmap.width - cropWidth + 1),
        Random.nextInt(bitmap.height - cropHeight + 1),
        cropWidth,
        cropHeight
    )
    val angle = Random.nextFloat() * 10f - 5f
    val radians = Math.toRadians(abs(angle.toDouble()))
    val sinValue = sin(radians)
    val cosValue = cos(radians)
    val heightRatio = cropHeight.toDouble() / cropWidth.toDouble()
    val safeWidth = minOf(
        cropWidth / (cosValue + sinValue * heightRatio),
        cropHeight / (sinValue + cosValue * heightRatio)
    ).toInt().coerceIn(1, cropWidth)
    val safeHeight = (safeWidth * heightRatio).toInt().coerceIn(1, cropHeight)
    val rotated = Bitmap.createBitmap(
        cropped, 0, 0, cropped.width, cropped.height,
        Matrix().apply { postRotate(angle) }, true
    )
    val result = Bitmap.createBitmap(
        rotated,
        (rotated.width - safeWidth) / 2,
        (rotated.height - safeHeight) / 2,
        safeWidth,
        safeHeight
    )
    if (rotated !== cropped && rotated !== result) rotated.recycle()
    if (cropped !== result) cropped.recycle()
    ByteArrayOutputStream().use { out ->
        result.compress(Bitmap.CompressFormat.JPEG, FACE_IMAGE_COMPRESS_QUALITY, out)
        val compressed =
            BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        if (compressed != null) {
            if (compressed !== result) result.recycle()
            compressed
        } else {
            result
        }
    }
}