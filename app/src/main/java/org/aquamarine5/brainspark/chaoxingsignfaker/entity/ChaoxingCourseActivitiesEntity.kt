package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.serialization.Serializable

@Serializable
data class ChaoxingCourseActivitiesEntity(
    val ext: String,
    val course: ChaoxingCourseEntity,
    val signActivities: List<ChaoxingSignActivityEntity>
) {
    companion object {
        @Deprecated("Do not use this")
        val Saver: Saver<ChaoxingCourseActivitiesEntity?, *> = listSaver(
            save = {
                listOf(
                    it == null,
                    it?.ext,
                    it?.course,
                    it?.signActivities
                )
            },
            restore = {
                if (it[0] as Boolean) return@listSaver null
                ChaoxingCourseActivitiesEntity(
                    ext = it[0] as String,
                    course = it[1] as ChaoxingCourseEntity,
                    signActivities = (it[2] as List<*>).filterIsInstance<ChaoxingSignActivityEntity>()
                )
            }
        )
    }
}