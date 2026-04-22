/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

open class ChaoxingPredictableException(
    override val message: String?,
    throwable: Throwable? = null
) : Throwable(message, throwable) {
    class ApplicationIllegalChannelException(
    ) : Exception("Illegal channel detected. Please check your app version and channel.")
}

open class ChaoxingParseDataException(
    message: String? = null,
    throwable: Throwable? = null,
    val data: String? = null
) : ChaoxingPredictableException(message, throwable)

fun Throwable.getPredictableMessage(): String {
    return if (this is ChaoxingPredictableException) {
        this.message ?: this.toString()
    } else {
        "预期外错误: ${this::class.java.name}: ${this.localizedMessage ?: this.toString()}"
    }
}