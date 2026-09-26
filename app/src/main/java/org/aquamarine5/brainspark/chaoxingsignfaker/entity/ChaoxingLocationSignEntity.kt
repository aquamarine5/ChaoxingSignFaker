/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Stable
@Serializable
data class ChaoxingLocationSignEntity(
    val latitude: Double,
    val longitude: Double,
    val address: String
) {
    @Volatile
    var isRandomizationTightened = false
        private set

    val randomizedLatitude: Double
        get() = latitude + Random.nextDouble(-randomRange, randomRange)
    val randomizedLongitude: Double
        get() = longitude + Random.nextDouble(-randomRange, randomRange)

    private val randomRange: Double
        get() = if (isRandomizationTightened) 0.00001 else 0.00005

    fun disableRandomizedLocation() {
        isRandomizationTightened = true
    }
}
