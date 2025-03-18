package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
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
    navToDetailDestination: (ChaoxingCourseEntity) -> Unit,
) {
    var activitiesData: List<ChaoxingCourseEntity> by rememberSaveable(
        saver = ChaoxingCourseEntity.Saver
    ) { mutableStateOf(emptyList()) }
    ChaoxingHttpClient.CheckInstance()
    LaunchedEffect(Unit) {
        if (activitiesData.isEmpty()) {
            ChaoxingHttpClient.instance?.let {
                activitiesData = ChaoxingCourseHelper.getAllCourse(it)
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

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
                                delay(500)
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
                                okHttpClient = OkHttpClient.Builder()
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
                        item{
                            SponsorCard()
                        }
                        items(activitiesData) {
                            key(it.courseId) {
                                CourseInfoColumnCard(it) {
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