/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseSignActivityColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

typealias CourseDetailDestination = ChaoxingCourseEntity

@Composable
fun CourseDetailScreen(
    courseEntity: ChaoxingCourseEntity,
    navToSignerDestination: (Any) -> Unit,
    navToListDestination: () -> Unit,
) {
    var activitiesData by remember { mutableStateOf<ChaoxingCourseActivitiesEntity?>(null) }
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    LaunchedEffect(Unit) {
        isFetchedFailure = runCatching {
            if (activitiesData == null) {
                ChaoxingHttpClient.instance?.let {
                    activitiesData = ChaoxingActivityHelper.getActivities(
                        it,
                        courseEntity,
                        context,
                        snackbarHost
                    )
                }
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
            .padding(16.dp, 16.dp, 16.dp, 0.dp).fillMaxSize()
    ) {
        Crossfade(isFetchedFailure) { v ->
            if (v == null) {
                CenterCircularProgressIndicator()
            } else if (v.isFailure) {
                NetworkExceptionComponent(v.exceptionOrNull()!!) {
                    coroutineScope.launch {
                        isFetchedFailure = runCatching {
                            if (activitiesData == null) {
                                ChaoxingHttpClient.instance?.let {
                                    activitiesData = ChaoxingActivityHelper.getActivities(
                                        it,
                                        courseEntity,
                                        context,
                                        snackbarHost
                                    )
                                }
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
                Column {
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
                                isFetchedFailure = runCatching {
                                    if (activitiesData == null) {
                                        ChaoxingHttpClient.instance?.let {
                                            activitiesData = ChaoxingActivityHelper.getActivities(
                                                it,
                                                courseEntity,
                                                context,
                                                snackbarHost
                                            )
                                        }
                                    }
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "获取签到信息失败",
                                        hapticFeedback
                                    )
                                }
                                delay(500)
                                pullToRefreshState = false
                            }
                            isFetchedFailure = null
                        }
                    ) {
                        if (activitiesData!!.signActivities.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(painter = painterResource(R.drawable.ic_package_open), null)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("该课程暂无签到活动")
                            }
                        } else
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    }
}