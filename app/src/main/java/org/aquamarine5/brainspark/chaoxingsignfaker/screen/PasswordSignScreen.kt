/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity


@Serializable
data class PasswordSignDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String,
    val startTime: Long,
    val endTime: Long?,
    val isLate: Boolean
) {
    companion object {
        fun parseFromSignActivityEntity(
            activityEntity: ChaoxingSignActivityEntity,
            isLate: Boolean
        ): PasswordSignDestination {
            return PasswordSignDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext,
                activityEntity.startTime,
                activityEntity.endTime,
                isLate
            )
        }
    }
}

@Composable
fun PasswordSignScreen(
    destination: PasswordSignDestination,
    navToCourseDetailDestination: () -> Unit,
    navToOtherSign: (Any) -> Unit,
    navToOtherUserDestination: () -> Unit
) {
}