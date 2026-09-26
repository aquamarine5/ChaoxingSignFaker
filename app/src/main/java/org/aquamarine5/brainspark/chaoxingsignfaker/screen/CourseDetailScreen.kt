/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseSignActivityColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkPredictable
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class CourseDetailDestination(
    val courses: List<ChaoxingCourseEntity>
)

@Composable
fun CourseDetailScreen(
    destination: CourseDetailDestination,
    navToSignerDestination: (Any) -> Unit,
    navToNonCloningListDestination: () -> Unit,
    navToListDestination: () -> Unit
) {
    var activitiesData by remember { mutableStateOf<ChaoxingCourseActivitiesEntity?>(null) }
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val activitiesListState = rememberLazyListState()
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    var partialFailureCount by remember { mutableIntStateOf(0) }
    val courses = destination.courses
    val courseEntity = courses.firstOrNull()
    if (courseEntity == null) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("课程数据缺失，请返回重试")
        }
        return
    }
    val isCloneSession = courseEntity.isCloneSession
    LaunchedEffect(Unit) {
        isFetchedFailure = runCatching {
            if (activitiesData == null) {
                val client = ChaoxingHttpClient.getClientInstanceOrClone(isCloneSession)
                requirePredictable(client != null) { "登录会话已失效，请重新登录" }
                partialFailureCount = 0
                activitiesData = ChaoxingActivityHelper.getActivitiesEntity(
                    client,
                    courses,
                    onPartialFailure = { partialFailureCount = it }
                )
                checkPredictable(activitiesData != null) { "获取签到信息失败" }
            }
        }.onFailure {
            it.snackbarReport(
                snackbarHost,
                coroutineScope,
                "获取签到信息失败",
                hapticFeedback
            )
        }
    }
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
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
                "课程名称：${courseEntity.courseName}" + if (courses.size > 1) " (x${courses.size})" else "",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (courses.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "此课程名称在课程列表里面出现了${courses.size}次，已将所有班级的签到活动合并显示。",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.Gray
                )
            }
        }
        if (partialFailureCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_x),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "有${partialFailureCount}个班级的签到活动获取失败，已显示其余班级的签到活动。",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Crossfade(isFetchedFailure, modifier = Modifier.weight(1f)) { v ->
            if (v == null) {
                CenterCircularProgressIndicator()
            } else if (v.isFailure) {
                NetworkExceptionComponent(v.exceptionOrNull()!!) {
                    coroutineScope.launch {
                        isFetchedFailure = runCatching {
                            if (activitiesData == null) {
                                val client =
                                    ChaoxingHttpClient.getClientInstanceOrClone(isCloneSession)
                                requirePredictable(client != null) { "登录会话已失效，请重新登录" }
                                partialFailureCount = 0
                                activitiesData = ChaoxingActivityHelper.getActivitiesEntity(
                                    client,
                                    courses,
                                    onPartialFailure = { partialFailureCount = it }
                                )
                                checkPredictable(activitiesData != null) { "获取签到信息失败" }
                                activitiesListState.animateScrollToItem(0)
                            }
                        }.onFailure {
                            it.snackbarReport(
                                snackbarHost,
                                coroutineScope,
                                "获取签到信息失败",
                                hapticFeedback
                            )
                        }
                    }
                    isFetchedFailure = null
                }
            } else {
                var pullToRefreshState by remember { mutableStateOf(false) }
                val nowYear = remember { LocalDate.now().year }
                val yearDateFormatter =
                    remember {
                        DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        )
                    }
                val normalDateFormatter =
                    remember { DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault()) }
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = pullToRefreshState,
                    onRefresh = {
                        pullToRefreshState = true
                        coroutineScope.launch {
                            runCatching {
                                val client =
                                    ChaoxingHttpClient.getClientInstanceOrClone(isCloneSession)
                                requirePredictable(client != null) { "登录会话已失效，请重新登录" }
                                partialFailureCount = 0
                                activitiesData = ChaoxingActivityHelper.getActivitiesEntity(
                                    client,
                                    courses,
                                    onPartialFailure = { partialFailureCount = it }
                                )
                                checkPredictable(activitiesData != null) { "获取签到信息失败" }
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "获取签到信息失败",
                                    hapticFeedback
                                )
                            }
                            delay(500.milliseconds)
                            pullToRefreshState = false
                        }
                    }
                ) {
                    val currentActivitiesData = activitiesData
                    if (currentActivitiesData == null) {
                        CenterCircularProgressIndicator()
                    } else if (currentActivitiesData.signActivities.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_package_open),
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("该课程暂无签到活动")
                        }
                    } else {
                        LazyColumn(
                            state = activitiesListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(
                                items = currentActivitiesData.signActivities,
                                key = { it.id }
                            ) { activity ->
                                CourseSignActivityColumnCard(activity, { startTimestamp ->
                                    Instant.ofEpochMilli(startTimestamp)
                                        .atZone(ZoneId.systemDefault()).let {
                                            if (it.year == nowYear) {
                                                normalDateFormatter.format(it)
                                            } else {
                                                yearDateFormatter.format(it)
                                            }
                                        }
                                }, isCloneSession, isMerged = courses.size > 1) { destination ->
                                    navToSignerDestination(destination)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
