/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseSignActivityColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

typealias CourseDetailDestination = ChaoxingCourseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseEntity: ChaoxingCourseEntity,
    navToSignerDestination: (Any) -> Unit,
    navToListDestination: () -> Unit,
) {
    var activitiesData by remember { mutableStateOf<ChaoxingCourseActivitiesEntity?>(null) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        runCatching {
            if (activitiesData == null) {
                ChaoxingHttpClient.instance?.let {
                    activitiesData = ChaoxingActivityHelper.getActivities(it, courseEntity)
                }
            }
        }.onFailure {
            Sentry.captureException(it)
            Toast.makeText(context, "获取课程活动失败", Toast.LENGTH_SHORT).show()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
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
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                        .width(5.dp)
                )
                Text(
                    "课程名称：${courseEntity.courseName}",
                    color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            var pullToRefreshState by remember { mutableStateOf(false) }
            PullToRefreshBox(
                isRefreshing = pullToRefreshState,
                onRefresh = {
                    pullToRefreshState = true
                    coroutineScope.launch {
                        ChaoxingHttpClient.instance?.let {
                            activitiesData =
                                ChaoxingActivityHelper.getActivities(it, courseEntity)
                        }
                        delay(1000)
                        pullToRefreshState = false
                    }
                }
            ) {
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