package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseSignActivityColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

typealias CourseDetailDestination = ChaoxingCourseEntity

@Composable
fun CourseDetailScreen(
    courseEntity: ChaoxingCourseEntity,
    navToSignerDestination: (Any) -> Unit,
    navToListDestination: () -> Unit,
) {
    var activitiesData by remember { mutableStateOf<ChaoxingCourseActivitiesEntity?>(null) }
    ChaoxingHttpClient.CheckInstance()
    LaunchedEffect(Unit) {
        ChaoxingHttpClient.instance?.let {
            activitiesData = ChaoxingActivityHelper.getActivities(it, courseEntity)
        }
    }
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (activitiesData == null) {
                CenterCircularProgressIndicator()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navToListDestination()
                        }
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "课程名称：${courseEntity.courseName}",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(activitiesData!!.signActivities) {
                        key(it.id) {
                            CourseSignActivityColumnCard(it) { destination ->
                                navToSignerDestination(destination)
                            }
                        }
                    }
                }

            }
        }
    }
}