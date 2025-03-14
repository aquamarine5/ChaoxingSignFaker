package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Serializable
data class CourseDetailDestination(val courseEntity: ChaoxingCourseEntity)

@Composable
fun CourseDetailScreen(courseEntity: ChaoxingCourseEntity) {

}