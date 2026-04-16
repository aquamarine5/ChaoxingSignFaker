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

class ChaoxingParseDataException(
    message: String? = null,
    throwable: Throwable? = null,
    data: String? = null
) : ChaoxingPredictableException("${message}@#[${data}]", throwable)

fun Throwable.getPredictableMessage(): String {
    return if (this is ChaoxingPredictableException) {
        this.message?.split("@#")
            ?.let { parts ->
                if (parts.size == 2) {
                    parts[1]
                } else {
                    parts[0]
                }
            } ?: this.toString()
    } else {
        "An unexpected error occurred: ${this::class.java.name}: ${this.localizedMessage ?: this.toString()}"
    }
}