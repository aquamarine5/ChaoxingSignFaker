/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

object ChaoxingCaptchaPredictor {
    private const val MODEL_FILENAME = "captcha.ort"
    private const val MODEL_INPUT_SIZE = 640
    private const val SCORE_THRESHOLD = 0.25f
    private const val NMS_IOU_THRESHOLD = 0.45f

    private const val EDGE_OFFSET = 4

    const val CAPTCHA_VALIDATE_MAX_REUSE_COUNT = 1

    private var ortSession: OrtSession? = null

    private var cachedValidate: String? = null
    private var cachedValidateResolvedByModel: Boolean = false
    private var remainingValidateReuses: Int = 0

    @Volatile
    var isAvailable: Boolean = true
        private set

    @Volatile
    var lastResolveByModel: Boolean = false

    fun initialize(context: Context) {
        if (ortSession != null || !isAvailable) return
        synchronized(this) {
            if (ortSession != null || !isAvailable) return
            runCatching {
                context.assets.open(MODEL_FILENAME).use { stream ->
                    ortSession = OrtEnvironment.getEnvironment().createSession(stream.readBytes())
                }
            }.onFailure { e ->
                e.printStackTrace()
                isAvailable = false
                // 标记不可用后继续抛出，让调用方（验证码弹窗）把真实原因展示出来，
                // 而不是静默降级导致预测永远返回 null 且无任何提示
                throw e
            }
        }
    }

    @Synchronized
    fun cacheValidate(validate: String, resolvedByModel: Boolean) {
        cachedValidate = validate
        cachedValidateResolvedByModel = resolvedByModel
        remainingValidateReuses = CAPTCHA_VALIDATE_MAX_REUSE_COUNT - 1
        lastResolveByModel = resolvedByModel
    }

    @Synchronized
    fun consumeCachedValidate(): String? {
        val validate = cachedValidate
        if (validate == null || remainingValidateReuses <= 0) {
            cachedValidate = null
            remainingValidateReuses = 0
            return null
        }
        remainingValidateReuses--
        if (remainingValidateReuses <= 0) {
            cachedValidate = null
        }
        // 复用缓存的 validate 时也要还原本次的解析来源，
        // 否则除第一次外所有签到都不会标记“由模型识别”
        lastResolveByModel = cachedValidateResolvedByModel
        return validate
    }

    @Synchronized
    fun invalidateCachedValidate() {
        cachedValidate = null
        remainingValidateReuses = 0
        cachedValidateResolvedByModel = false
    }

    fun predictSliderXOffset(originalImage: Bitmap): Int? {
        val session = ortSession ?: return null
        val environment = OrtEnvironment.getEnvironment()
        val paddedLength = max(originalImage.width, originalImage.height)
        val paddedImage =
            createBitmap(paddedLength, paddedLength)
        Canvas(paddedImage).drawBitmap(originalImage, 0f, 0f, null)
        val scaledImage = paddedImage.scale(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
        val pixels = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        scaledImage.getPixels(
            pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE
        )
        val pixelCount = pixels.size
        val inputData = FloatArray(pixelCount * 3)
        for ((index, pixel) in pixels.withIndex()) {
            inputData[index] = (pixel shr 16 and 0xFF) / 255f
            inputData[pixelCount + index] = (pixel shr 8 and 0xFF) / 255f
            inputData[pixelCount * 2 + index] = (pixel and 0xFF) / 255f
        }
        OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(inputData),
            longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())
        ).use { tensor ->
            session.run(mapOf("images" to tensor)).use { results ->
                @Suppress("UNCHECKED_CAST") val output =
                    (results[0].value as Array<Array<FloatArray>>)[0]
                val centerXs = output[0]
                val centerYs = output[1]
                val widths = output[2]
                val heights = output[3]
                val scores = output[4]
                val candidates = centerXs.indices.mapNotNull { index ->
                    val score = scores[index]
                    if (score < SCORE_THRESHOLD) return@mapNotNull null
                    SliderGapDetection(
                        centerXs[index] - widths[index] / 2,
                        centerYs[index] - heights[index] / 2,
                        widths[index],
                        heights[index],
                        score
                    )
                }
                val best = nonMaxSuppression(candidates).maxByOrNull { it.score }
                best ?: return null
                return (best.x * paddedLength / MODEL_INPUT_SIZE - EDGE_OFFSET + 0.5f).toInt()
            }
        }
    }

    private fun nonMaxSuppression(detections: List<SliderGapDetection>): List<SliderGapDetection> {
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val kept = mutableListOf<SliderGapDetection>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            kept.add(current)
            sorted.removeAll { iou(current, it) > NMS_IOU_THRESHOLD }
        }
        return kept
    }

    private fun iou(a: SliderGapDetection, b: SliderGapDetection): Float {
        val intersectWidth = min(a.x + a.width, b.x + b.width) - max(a.x, b.x)
        val intersectHeight = min(a.y + a.height, b.y + b.height) - max(a.y, b.y)
        if (intersectWidth <= 0f || intersectHeight <= 0f) return 0f
        val intersection = intersectWidth * intersectHeight
        val union = a.width * a.height + b.width * b.height - intersection
        return intersection / union
    }

    private data class SliderGapDetection(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val score: Float
    )
}