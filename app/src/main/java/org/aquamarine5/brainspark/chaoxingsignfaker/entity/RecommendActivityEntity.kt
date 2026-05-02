/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination

data class RecommendActivityEntity(
    val destination: SignDestination,
    val startTime: Long,
    val className: String,
    val classId: Int,
    val courseId: Int,
    val activityName: String
)
