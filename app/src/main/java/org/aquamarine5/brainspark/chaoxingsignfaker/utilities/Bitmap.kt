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
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.ceil

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> { error("ImageLoader not provided") }

const val MAX_DECODE_DIMENSION = 3072

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