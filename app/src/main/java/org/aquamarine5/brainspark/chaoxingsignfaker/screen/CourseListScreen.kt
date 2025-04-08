/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Serializable
object CourseListDestination

@Serializable
object SignGraphDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
) {
    var activitiesData: List<ChaoxingCourseEntity> by rememberSaveable(
        saver = ChaoxingCourseEntity.Saver
    ) { mutableStateOf(emptyList()) }

    var rawActivitiesData: List<ChaoxingCourseEntity> by rememberSaveable(
        saver = ChaoxingCourseEntity.Saver
    ) { mutableStateOf(emptyList()) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (activitiesData.isEmpty()) {
            runCatching {
                val preferredCourse =
                    context.chaoxingDataStore.data.first().preferCourseList.reversed()
                ChaoxingHttpClient.instance?.let { httpClient ->
                    ChaoxingCourseHelper.getAllCourse(httpClient).apply {
                        rawActivitiesData= this
                        activitiesData = this.filter {
                            preferredCourse.contains(it.courseId)
                        }.map { it.apply { isPreferred = true } } + this.filter {
                            !preferredCourse.contains(it.courseId)
                        }
                    }
                }
            }.onFailure {
                Sentry.captureException(it)
                Toast.makeText(context, "获取课程列表失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { ChaoxingHttpClient.instance!!.okHttpClient })
            )
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }.crossfade(true).build()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        if (activitiesData.isEmpty()) {
            CenterCircularProgressIndicator()
        } else {
            var isRefreshing by remember { mutableStateOf(false) }
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        ChaoxingHttpClient.instance?.let {
                            activitiesData = ChaoxingCourseHelper.getAllCourse(it)
                            delay(1000)
                            isRefreshing = false
                        }
                    }
                }
            ) {
                LazyColumn {
                    items(activitiesData) { data ->
                        key(data.courseId) {
                            CourseInfoColumnCard(
                                data,
                                imageLoader,
                                modifier = Modifier.animateItem(
                                    placementSpec = spring(
                                        stiffness = Spring.StiffnessLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                    ),
                                    fadeInSpec = spring(Spring.StiffnessLow),
                                    fadeOutSpec = spring(Spring.StiffnessLow)
                                ),
                                onPreferredResort = { isPreferred ->
                                    if (isPreferred)
                                        coroutineScope.launch {
                                            context.chaoxingDataStore.updateData {
                                                it.toBuilder().addPreferCourse(data.courseId)
                                                    .build()
                                            }
                                            activitiesData =
                                                listOf(data) + activitiesData.filter {
                                                    it.courseId != data.courseId
                                                }
                                        }
                                    else {
                                        coroutineScope.launch {
                                            context.chaoxingDataStore.updateData { dataStore ->
                                                dataStore.toBuilder().apply {
                                                    val newList =
                                                        preferCourseList.filterNot { it == data.courseId }
                                                    clearPreferCourse()
                                                    addAllPreferCourse(newList)
                                                }.build()
                                            }
                                            activitiesData = activitiesData.filter {
                                                it.courseId != data.courseId
                                            } + listOf(data)
                                        }
                                    }
                                }
                            ) {
                                navToDetailDestination(data)
                            }
                        }
                    }
                }
            }
        }
    }
}