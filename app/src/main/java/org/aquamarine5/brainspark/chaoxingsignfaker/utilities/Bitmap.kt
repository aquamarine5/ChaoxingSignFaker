/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.graphics.scale
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> { error("ImageLoader not provided") }

const val MAX_DECODE_DIMENSION = 3072

suspend fun randomizeStylizeImage(bitmap: Bitmap): Bitmap = withContext(Dispatchers.IO) {
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
        result.compress(Bitmap.CompressFormat.JPEG, 90, out)
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

fun Bitmap.scaleDownToMaxDimension(maxDimension: Int = MAX_DECODE_DIMENSION): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxDimension || width <= 0 || height <= 0) return this
    val ratio = maxDimension / largest.toFloat()
    val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
    val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
    val scaled = this.scale(targetWidth, targetHeight)
    if (scaled !== this && !isRecycled) recycle()
    return scaled
}

fun ContentResolver.decodePhotoBitmap(uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(this, uri)
            ) { decoder, info, _ ->
                decoder.isMutableRequired = true
                val largestDimension = maxOf(info.size.width, info.size.height)
                if (largestDimension > MAX_DECODE_DIMENSION) {
                    val sample =
                        ceil(largestDimension / MAX_DECODE_DIMENSION.toDouble()).toInt()
                    decoder.setTargetSize(
                        info.size.width / sample,
                        info.size.height / sample
                    )
                }
            }
        }.getOrNull()?.let { return it }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DECODE_DIMENSION) {
        sampleSize *= 2
    }
    return openInputStream(uri)?.use {
        BitmapFactory.decodeStream(
            it,
            null,
            BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }
}

@OptIn(ExperimentalContracts::class)
fun checkThrowFaceException(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw ChaoxingFaceImageException(message)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> Bitmap.use(block: (Bitmap) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    try {
        return block(this)
    } finally {
        if (!isRecycled) recycle()
    }
}