/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

const val MARKER_BUNDLE_ADDRESS = "address"
const val MARKER_BUNDLE_LABEL = "label"
const val MARKER_BUNDLE_TYPE = "type"

enum class MarkerBundleType(val value: String) {
    LOCATION("location"), FAVORITE("favorite");

    override fun toString(): String {
        return value
    }
}