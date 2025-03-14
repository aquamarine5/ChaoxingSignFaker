package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Serializable
data class CourseDetailDestination(val courseEntity: ChaoxingCourseEntity)

@Composable
fun CourseDetailScreen(courseEntity: ChaoxingCourseEntity) {
    var activitiesData by remember { mutableStateOf<ChaoxingCourseActivitiesEntity?>(null) }
    ChaoxingHttpClient.CheckInstance()
    LaunchedEffect(Unit){
        ChaoxingHttpClient.instance?.let{
            activitiesData=ChaoxingActivityHelper.getActivities(it,courseEntity)
        }
    }
    if(activitiesData==null){
        CircularProgressIndicator()
    }else{
        LazyColumn {
            items(activitiesData!!.signActivities){
                key(it.id){
                    CourseColumnCard(it)
                }
            }
        }
    }
}