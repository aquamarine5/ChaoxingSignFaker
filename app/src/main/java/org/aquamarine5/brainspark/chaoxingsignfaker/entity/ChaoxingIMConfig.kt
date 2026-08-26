/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.Immutable

@Deprecated(
    "The service of im.chaoxing.com is no longer available. Please use the new interface based on easecdn.com. For more information, please refer to: https://github.com/aquamarine5/ChaoxingSignFaker/issues/188",
    replaceWith = ReplaceWith("ChaoxingEasemobIMConfig")
)
@Immutable
data class ChaoxingIMConfig(
    val imImgUrl: String,
    val imName: String,
    val imToken: String,
    val imTuid: String,
    val imPuid: String,
    val imFid: String
)
