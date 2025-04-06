/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.serialization.Serializable

@Serializable
data class ChaoxingCourseEntity(
    val courseName: String,
    val teacherName: String,
    val courseId: Int,
    val classId: Int,
    val className: String,
    val imageUrl: String,
    val schools: String?,
    var isPreferred:Boolean=false
) {
    companion object {
        val Saver: Saver<MutableState<List<ChaoxingCourseEntity>>, *> = listSaver(
            save = { saver ->
                saver.value.map {
                    listOf(
                        it.courseName,
                        it.teacherName,
                        it.courseId,
                        it.classId,
                        it.className,
                        it.imageUrl,
                        it.schools,
                        it.isPreferred
                    )
                }
            },
            restore = { restorer ->
                mutableStateOf(
                    restorer.map {
                        ChaoxingCourseEntity(
                            it[0] as String,
                            it[1] as String,
                            it[2] as Int,
                            it[3] as Int,
                            it[4] as String,
                            it[5] as String,
                            it[6] as String?,
                            it[7] as Boolean
                        )
                    }
                )
            }
        )
    }
}