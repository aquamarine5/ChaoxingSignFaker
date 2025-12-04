/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.BlockedContent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.RecommendActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData
import java.time.Instant
import java.time.LocalDateTime

@Serializable
object CourseListDestination

@Serializable
object SignGraphDestination

private const val SORT_TOP = 100
private const val SORT_STAR = 10
private const val SORT_UNORDERED = 5
private const val SORT_COMMON = 0

@Composable
fun CourseListScreen(
    stackbricksService: StackbricksService,
    imageLoader: ImageLoader,
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
    onNewVersionAvailable: () -> Unit,
    navToSettingDestination: () -> Unit,
    navToSignActivityDestination: (Any) -> Unit,
    navToLoginDestination: () -> Unit
) {
    val activitiesData =
        rememberSaveable(saver = ChaoxingCourseEntity.Saver) { mutableStateListOf() }
    var preferredClassIds = remember {
        mutableStateListOf<Int>()
    }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var newestVersionData by remember { mutableStateOf<StackbricksVersionData?>(null) }
    var isNewVersionDialogDisplayed = rememberSaveable { false }
    var isForceInstall by remember { mutableStateOf(false) }
    val snackbarHost = LocalSnackbarHostState.current
    var recommendActivities by remember { mutableStateOf<List<RecommendActivityEntity>?>(null) }
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                if (stackbricksService.internalVersionData == null && !isNewVersionDialogDisplayed) {
                    newestVersionData = stackbricksService.isNeedUpdate()
                    newestVersionData?.forceInstallLessVersion?.let {
                        isForceInstall =
                            (it > BuildConfig.VERSION_CODE)
                    }
                }
            }.onFailure {
                it.snackbarReport(snackbarHost, coroutineScope, "检查更新失败", hapticFeedback)
            }
            recommendActivities =
                ChaoxingRecommendHelper.checkRecommendedActivities(context, snackbarHost)
            if (activitiesData.isEmpty()) {
                isFetchedFailure = runCatching {
//                    context.chaoxingDataStore.data.first().apply {
//                        if (version <= 0) {
//                            ChaoxingHttpClient.instance?.let { httpClient ->
//                                ChaoxingCourseHelper.getAllCourse(
//                                    httpClient,
//                                    context,
//                                    navToLoginDestination
//                                ).let { data ->
//                                    context.chaoxingDataStore.updateData { dataStore ->
//                                        dataStore.toBuilder().apply {
//                                            addAllPreferCourseClass(preferClassIdList.map { classId ->
//                                                ChaoxingCourseClass.newBuilder()
//                                                    .setClassId(classId)
//                                                    .setCourseId(data.first { it.classId == classId }.courseId)
//                                                    .build()
//                                            })
//                                            setVersion(1)
//                                        }.build()
//                                    }
//                                }
//                            }
//                        }
//                    }
                    preferredClassIds =
                        context.chaoxingDataStore.data.first().preferClassIdList.toMutableStateList()
                            .apply {
                                reverse()
                            }
                    ChaoxingHttpClient.instance?.let { httpClient ->
                        ChaoxingCourseHelper.getAllCourse(
                            httpClient,
                            context,
                            navToLoginDestination
                        )
                            .apply {
                                activitiesData.addAll(this.filter {
                                    preferredClassIds.contains(it.classId)
                                }.map { it.apply { isPreferred = true } } + this.filter {
                                    !preferredClassIds.contains(it.classId)
                                })
                            }

                    }
                }.onFailure {
                    it.snackbarReport(
                        snackbarHost,
                        coroutineScope,
                        "获取课程列表失败",
                        hapticFeedback
                    )
                }
            }
        }
    }
    if (newestVersionData != null && (isForceInstall || !isNewVersionDialogDisplayed)) {
        onNewVersionAvailable()
        AlertDialog(onDismissRequest = {
            if (isForceInstall) {
                Toast.makeText(context, "必须更新应用", Toast.LENGTH_SHORT).show()
            } else {
                newestVersionData = null
            }
        }, confirmButton = {
            Button(onClick = {
                navToSettingDestination()
            }) {
                Text("去更新")
            }
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
                    append(
                        newestVersionData?.versionName
                            ?: stackbricksService.internalVersionData?.versionName
                    )
                }
                append("\n当前版本：")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily(
                            Font(R.font.gilroy)
                        )
                    )
                ) {
                    append(stackbricksService.getCurrentVersionName())
                }
                append("\n更新日志：\n")
                withStyle(SpanStyle(fontSize = 11.sp)) {
                    append(
                        newestVersionData?.changelog
                            ?: stackbricksService.internalVersionData?.changelog
                    )
                }
            })
        }, title = {
            Text("有新版本可用！")
        })
    }
    BlockedContent {
        Column(
            modifier = Modifier
                .padding(16.dp, 12.dp, 16.dp, 0.dp)
        ) {
            Crossfade(isFetchedFailure) { v ->
                if (activitiesData.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            recommendActivities != null,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            recommendActivities?.forEachIndexed { index, item ->
                                runCatching {
                                    Card(
                                        onClick = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            navToSignActivityDestination(item.destination)
                                        },
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(24.dp, 8.dp)
                                                .padding(3.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_brain_circuit), null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("根据平时的签到习惯推断出可能会点击的签到活动：")
                                                Text(buildAnnotatedString {
                                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append(item.className)
                                                    }
                                                    append(" 在 ")
                                                    withStyle(
                                                        SpanStyle(
                                                            fontFamily = FontFamily(
                                                                Font(
                                                                    R.font.gilroy
                                                                )
                                                            )
                                                        )
                                                    ) {
                                                        append(
                                                            LocalDateTime.from(
                                                                Instant.ofEpochMilli(
                                                                    item.startTime
                                                                )
                                                            ).run {
                                                                "$hour:$minute:$second"
                                                            })
                                                    }
                                                    append(" 的 ")
                                                    append(item.activityName)
                                                })

                                            }
                                        }
                                    }
                                    if (index != recommendActivities?.lastIndex) {
                                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                        }
                        var debouncePreviousTime = 0L
                        LazyColumn {
//                    item {
//                        OutlinedButton(
//                            onClick = {
//
//                            },
//                            shape = RoundedCornerShape(18.dp),
//                            modifier = Modifier
//                                .fillMaxWidth()
//                        ){
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.Start,
//                                modifier = Modifier.fillMaxWidth()
//                            ){
//                                Icon(painter = painterResource(R.drawable.ic_users_round),"")
//                                Spacer(modifier = Modifier.width(14.dp))
//                                Text("为群聊签到")
//                            }
//                        }
//                    }
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
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                            if (isPreferred)
                                                coroutineScope.launch {
                                                    context.chaoxingDataStore.updateData {
                                                        it.toBuilder()
                                                            .addPreferClassId(data.classId)
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
                                                            //remo
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
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - debouncePreviousTime < 1000)
                                            return@CourseInfoColumnCard
                                        debouncePreviousTime = currentTime
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        navToDetailDestination(data)
                                    }
                                }
                            }
                        }
                    }
                } else if (v == null) {
                    CenterCircularProgressIndicator()
                } else if (v.isFailure) {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                ChaoxingHttpClient.instance?.let { httpClient ->
                                    ChaoxingCourseHelper.getAllCourse(
                                        httpClient,
                                        context,
                                        navToLoginDestination
                                    )
                                        .apply {
                                            activitiesData.addAll(this.filter {
                                                preferredClassIds.contains(it.classId)
                                            }.map {
                                                it.apply {
                                                    isPreferred = true
                                                }
                                            } + this.filter {
                                                !preferredClassIds.contains(it.classId)
                                            })
                                        }

                                }
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "获取课程列表失败",
                                    hapticFeedback
                                )
                            }
                        }
                        isFetchedFailure = null
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.align(Alignment.Center)) {
                            Icon(painterResource(R.drawable.ic_circle_question_mark), null)
                            Text("暂无课程，请检查登录的学习通账号是否正确。")
                        }
                    }
                }
            }
        }
    }
}