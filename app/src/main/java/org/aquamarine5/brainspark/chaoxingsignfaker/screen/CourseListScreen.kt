/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.sentry.Sentry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CourseInfoColumnCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.stackbricks.StackbricksComponent
import org.aquamarine5.brainspark.stackbricks.StackbricksStateService
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuConfiguration
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuMessageProvider
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuPackageProvider
import org.aquamarine5.brainspark.stackbricks.rememberStackbricksStatus
import java.util.concurrent.TimeUnit

@Serializable
object CourseListDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    navToOtherUserDestination: () -> Unit,
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
) {
    var activitiesData: List<ChaoxingCourseEntity> by rememberSaveable(
        saver = ChaoxingCourseEntity.Saver
    ) { mutableStateOf(emptyList()) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (activitiesData.isEmpty()) {
            runCatching {
                ChaoxingHttpClient.instance?.let {
                    activitiesData = ChaoxingCourseHelper.getAllCourse(it)
                }
            }.onFailure {
                Sentry.captureException(it)
                Toast.makeText(context, "获取课程列表失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val imageLoader = ImageLoader.Builder(context).components {
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

    val stackbricksState by rememberStackbricksStatus()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
                        item {
                            QiniuConfiguration(
                                "cdn.aquamarine5.fun",
                                referer = "http://cdn.aquamarine5.fun/",
                                configFilePath = "chaoxingsignfaker_stackbricks_v1_config.json",
                                okHttpClient = ChaoxingHttpClient.instance!!.okHttpClient.newBuilder()
                                    .callTimeout(20, TimeUnit.MINUTES)
                                    .readTimeout(20, TimeUnit.MINUTES)
                                    .writeTimeout(20, TimeUnit.MINUTES)
                                    .build()
                            ).let {
                                StackbricksComponent(
                                    StackbricksStateService(
                                        LocalContext.current,
                                        QiniuMessageProvider(it),
                                        QiniuPackageProvider(it),
                                        stackbricksState
                                    ), checkUpdateOnLaunch = true
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                            SponsorCard()
                        }
                        item {
                            Button(
                                onClick = {
                                    navToOtherUserDestination()
                                },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            *arrayOf(
                                                0f to Color(0xff1eeefb),
                                                0.6f to Color(0xffdaaaec),
                                                0.8f to Color(0xffffe67f)
                                            )
                                        ), RoundedCornerShape(18.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_users_round),
                                        contentDescription = "多用户"
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        "添加其他用户以签到",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.W600
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(activitiesData) {
                            key(it.courseId) {
                                CourseInfoColumnCard(it, imageLoader) {
                                    navToDetailDestination(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}