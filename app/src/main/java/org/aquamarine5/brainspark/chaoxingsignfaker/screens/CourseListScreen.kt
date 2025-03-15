package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Serializable
object CourseListDestination

@Composable
fun CourseListScreen(
    navController: NavController) {
    var activitiesData by remember { mutableStateOf<List<ChaoxingCourseEntity>?>(null) }
    ChaoxingHttpClient.CheckInstance()
    LaunchedEffect(Unit) {
        ChaoxingHttpClient.instance?.let {
            activitiesData = ChaoxingCourseHelper.getAllCourse(it)
        }
    }
    if (activitiesData == null) {
        CircularProgressIndicator()
    } else {
        LazyColumn {
            items(activitiesData!!) {
                key(it.courseId) {
                    Button(onClick = {
                        navController.navigate(it)
                    }){
                        Text("课程名称：${it.courseName},课程ID：${it.courseId},老师：${it.teacherName}")
                    }
                }
            }
        }
    }
}