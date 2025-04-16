/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.sentry.Sentry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData

@Serializable
object CourseListDestination

@Serializable
object SignGraphDestination

private const val SORT_TOP = 100
private const val SORT_STAR = 10
private const val SORT_UNORDERED = 5
private const val SORT_COMMON = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    stackbricksService: StackbricksService,
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
    onNewVersionAvailable: () -> Unit,
    navToSettingDestination: () -> Unit,
    navToLoginDestination: () -> Unit
) {
    val activitiesData = remember { mutableStateListOf<ChaoxingCourseEntity>() }
    var preferredClassIds = remember {
        mutableStateListOf<Int>()
    }
    val context = LocalContext.current
    var newestVersionData by remember { mutableStateOf<StackbricksVersionData?>(null) }
    LaunchedEffect(Unit) {
        newestVersionData = stackbricksService.isNeedUpdate()
        if (activitiesData.isEmpty()) {
            runCatching {
                preferredClassIds =
                    context.chaoxingDataStore.data.first().preferClassIdList.toMutableStateList()
                        .apply {
                            reverse()
                        }
                ChaoxingHttpClient.instance?.let { httpClient ->
                    ChaoxingCourseHelper.getAllCourse(httpClient, context, navToLoginDestination)
                        .apply {
                            activitiesData.addAll(this.filter {
                                preferredClassIds.contains(it.classId)
                            }.map { it.apply { isPreferred = true } } + this.filter {
                                !preferredClassIds.contains(it.classId)
                            })
                        }
                }
            }.onFailure {
                Log.d("CourseListScreen", "获取课程列表失败")
                Sentry.captureException(it)
                Toast.makeText(context, "获取课程列表失败", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
                throw it
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
    if (newestVersionData != null) {
        onNewVersionAvailable()
        AlertDialog(onDismissRequest = {
            newestVersionData = null
        }, confirmButton = {
            navToSettingDestination()
        }, text = {
            Text(buildAnnotatedString {
                append("检测到新版本：")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily(
                            Font(R.font.gilroy)
                        )
                    )
                ) {
                    append(newestVersionData!!.versionName)
                }
                append("\n更新日志：")
                withStyle(SpanStyle(fontSize = 11.sp)) {
                    append(newestVersionData!!.changelog)
                }
            })

        }, title = {
            Text("有新版本可用！")
        })
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        if (activitiesData.isEmpty()) {
            CenterCircularProgressIndicator()
        } else {
            LazyColumn {
                items(activitiesData) { data ->
                    key(data.classId) {
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
                                            it.toBuilder().addPreferClassId(data.classId)
                                                .build()
                                        }
                                        preferredClassIds.add(data.classId)
                                        activitiesData.sortByDescending {
                                            if (it.classId == data.classId)
                                                return@sortByDescending SORT_TOP
                                            if (preferredClassIds.contains(
                                                    it.classId
                                                )
                                            ) return@sortByDescending SORT_STAR
                                            else return@sortByDescending SORT_COMMON
                                        }
                                    }
                                else {
                                    coroutineScope.launch {
                                        context.chaoxingDataStore.updateData { dataStore ->
                                            dataStore.toBuilder().apply {
                                                val newList =
                                                    preferClassIdList.filterNot { it == data.classId }
                                                clearPreferClassId()
                                                addAllPreferClassId(newList)
                                            }.build()
                                        }
                                        preferredClassIds.remove(data.classId)
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