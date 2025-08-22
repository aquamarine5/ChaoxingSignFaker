/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

data class ChaoxingCaptchaDataEntity(
    val captchaId: String,
    val type: String,
    val version: String,
    val token: String,
    val captchaKey: String,
    val iv: String,
    val shadeImageUrl: String,
    val cutoutImageUrl: String
)
