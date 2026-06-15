/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.BlockedContent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CloneSessionTips
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.RecommendActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
data class CourseListDestination(
    val isCloneSession: Boolean = false
)

@Serializable
object SignGraphDestination

private const val SORT_TOP = 100
private const val SORT_STAR = 10
private const val SORT_UNFAVOURED = 5
private const val SORT_COMMON = 0

@Composable
fun CourseListScreen(
    destination: CourseListDestination,
    stackbricksService: StackbricksService,
    imageLoader: ImageLoader,
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
    onNewVersionAvailable: () -> Unit,
    navToSettingDestination: () -> Unit,
    navToSignActivityDestination: (SignDestination) -> Unit,
    navToLoginDestination: () -> Unit,
    navToGroupDestination: () -> Unit
) {
    val activitiesData =
        rememberSaveable(saver = ChaoxingCourseEntity.Saver) { mutableStateListOf() }
    val preferredClassIds = rememberSaveable {
        mutableListOf<Int>()
    }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    var newestVersionData by remember { mutableStateOf<StackbricksVersionData?>(null) }
    var isForceInstall by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = LocalSnackbarHostState.current
    var recommendActivities by remember { mutableStateOf<List<RecommendActivityEntity>?>(null) }
    var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            launch {
                runCatching {
                    withTimeout(2.seconds) {
                        if (stackbricksService.internalVersionData == null) {
                            newestVersionData = stackbricksService.isNeedUpdate()
                            newestVersionData?.forceInstallLessVersion?.let {
                                isForceInstall =
                                    (it > BuildConfig.VERSION_CODE)
                            }
                        }
                    }
                }.onFailure {
                    it.snackbarReport(snackbarHost, coroutineScope, "检查更新失败", hapticFeedback)
                }
            }
            recommendActivities =
                ChaoxingRecommendHelper.checkRecommendedActivities(context, snackbarHost)
            if (activitiesData.isEmpty()) {
                isFetchedFailure = runCatching {
                    /**
                    if (false) {
                    context.chaoxingDataStore.data.first().apply {
                    if (version <= 0) {
                    ChaoxingHttpClient.instance?.let { httpClient ->
                    ChaoxingCourseHelper.getAllCourse(
                    httpClient,
                    context,
                    navToLoginDestination
                    ).let { data ->
                    context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder().apply {
                    addAllPreferCourseClass(preferClassIdList.map { classId ->
                    ChaoxingCourseClass.newBuilder()
                    .setClassId(classId)
                    .setCourseId(data.first { it.classId == classId }.courseId)
                    .build()
                    })
                    setVersion(1)
                    }.build()
                    }
                    }
                    }
                    }
                    }
                    } //TODO: Recommend preferred class
                     */

                    preferredClassIds.addAll(
                        context.chaoxingDataStore.data.first().preferClassIdList.reversed()
                    )
                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                        ?.let { httpClient ->
                            ChaoxingCourseHelper.getAllCourse(
                                httpClient,
                                context,
                                destination.isCloneSession,
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
    var isEmergencyToSkipUpdate by remember { mutableStateOf(false) }
    if (isEmergencyToSkipUpdate) {
        AlertDialog(onDismissRequest = {
            isEmergencyToSkipUpdate = false
        }, dismissButton = {
            TextButton(onClick = {
                isEmergencyToSkipUpdate = false
                newestVersionData = null
            }) {
                Text("着急签到一会更新")
            }
        }, confirmButton = {
            Button(onClick = {
                navToSettingDestination()
            }) {
                Text("现在去更新")
            }
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_arrow_big_up_dash),
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }, text = {
            Text("此版本设置了强制更新，强烈建议进行更新，忽略更新可能导致签到失败或其他意外的BUG。")
        })
    }
    if (newestVersionData != null) {
        onNewVersionAvailable()
        AlertDialog(onDismissRequest = {
            if (isForceInstall) {
                Toast.makeText(context, "必须更新应用", Toast.LENGTH_SHORT).show()
            } else {
                newestVersionData = null
            }
        }, dismissButton = {
            TextButton(onClick = {
                if (isForceInstall)
                    isEmergencyToSkipUpdate = true
                else
                    newestVersionData = null
            }) {
                Text("我着急签到，来不及更新")
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
        }, icon = {
            Icon(painterResource(R.drawable.ic_circle_arrow_up), null)
        })
    }
    BlockedContent {
        Column(
            modifier = Modifier
                .padding(16.dp, 4.dp, 16.dp, 0.dp)
        ) {
            Crossfade(isFetchedFailure) { v ->
                if (activitiesData.isNotEmpty()) {
                    var pullToRefreshState by remember { mutableStateOf(false) }
                    PullToRefreshBox(
                        isRefreshing = pullToRefreshState,
                        onRefresh = {
                            pullToRefreshState = true
                            coroutineScope.launch(Dispatchers.IO) {
                                isFetchedFailure = runCatching {
                                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                                        ?.let { httpClient ->
                                            ChaoxingCourseHelper.getAllCourse(
                                                httpClient,
                                                context,
                                                destination.isCloneSession,
                                                navToLoginDestination
                                            )
                                                .apply {
                                                    val newActivities = this.filter {
                                                        preferredClassIds.contains(it.classId)
                                                    }.map {
                                                        it.apply {
                                                            isPreferred = true
                                                        }
                                                    } + this.filter {
                                                        !preferredClassIds.contains(it.classId)
                                                    }
                                                    activitiesData.clear()
                                                    activitiesData.addAll(newActivities)
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
                                delay(500.milliseconds)
                                pullToRefreshState = false
                            }
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (destination.isCloneSession && ChaoxingHttpClient.cloneInstance != null)
                                CloneSessionTips()
                            /**
                            if (false) {
                            AnimatedVisibility(
                            recommendActivities != null,
                            enter = fadeIn() + slideInVertically()
                            ) {
                            recommendActivities?.forEachIndexed { index, item ->
                            Card(
                            onClick = {
                            hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.ContextClick
                            )
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
                            Icon(
                            painterResource(R.drawable.ic_brain_circuit),
                            null
                            )
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
                            } //TODO: Recommend
                             */
                            var debouncePreviousTime = 0L
                            LazyColumn {
                                if (!destination.isCloneSession || ChaoxingHttpClient.cloneInstance == null)
                                    item {
                                        Card {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp, 8.dp)
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.ic_circle_question_mark),
                                                    null
                                                )
                                                Column(
                                                    modifier = Modifier.padding(
                                                        start = 12.dp
                                                    )
                                                ) {
                                                    Text(
                                                        "找不到要签到的班级或者签到的活动？可能老师是在群聊里面发起的签到",
                                                        fontSize = 14.sp,
                                                        lineHeight = 17.sp,
                                                        style = TextStyle.Default.copy(
                                                            lineBreak = LineBreak(
                                                                strategy = LineBreak.Strategy.HighQuality,
                                                                strictness = LineBreak.Strictness.Strict,
                                                                wordBreak = LineBreak.WordBreak.Default
                                                            )
                                                        )
                                                    )
                                                    OutlinedButton(
                                                        onClick = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.ContextClick
                                                            )
                                                            navToGroupDestination()
                                                        },
                                                        shape = RoundedCornerShape(18.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.ic_users_round),
                                                                null,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Text(
                                                                "从群聊列表查找签到",
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                items(activitiesData) { data ->
                                    key(data.classId) {
                                        CourseInfoColumnCard(
                                            data,
                                            imageLoader,
                                            modifier = Modifier.animateItem(
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessVeryLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                                ),
                                                fadeInSpec = spring(Spring.StiffnessVeryLow),
                                                fadeOutSpec = spring(Spring.StiffnessVeryLow)
                                            ),
                                            onPreferredResort = { isPreferred ->
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
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
                                                                val newList =
                                                                    preferClassIdList.filterNot { it == data.classId }
                                                                clearPreferClassId()
                                                                addAllPreferClassId(newList)
                                                            }.build()
                                                        }
                                                        preferredClassIds.remove(data.classId)
                                                        activitiesData.sortByDescending {
                                                            if (it.classId == data.classId)
                                                                return@sortByDescending SORT_UNFAVOURED
                                                            if (preferredClassIds.contains(
                                                                    it.classId
                                                                )
                                                            ) return@sortByDescending SORT_STAR
                                                            else return@sortByDescending SORT_COMMON
                                                        }
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
                    }
                } else if (v == null) {
                    CenterCircularProgressIndicator()
                } else if (v.isFailure) {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)
                                    ?.let { httpClient ->
                                        ChaoxingCourseHelper.getAllCourse(
                                            httpClient,
                                            context,
                                            destination.isCloneSession,
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