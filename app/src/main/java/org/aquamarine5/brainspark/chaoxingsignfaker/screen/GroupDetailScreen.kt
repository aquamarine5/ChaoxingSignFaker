/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingEasemobIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingGroupSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@Immutable
@Serializable
data class GroupDetailDestination(
    val groups: List<ChaoxingEasemobIMGroup>
)

@Composable
fun GroupDetailScreen(
    groupDetail: GroupDetailDestination,
    navToGroupListDestination: () -> Unit,
    onSignAction: (SignDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .fillMaxSize()
    ) {
        val coroutineScope = rememberCoroutineScope()
        var messages by remember { mutableStateOf<List<ChaoxingGroupSignActivityEntity>?>(null) }
        val snackbarHostState = LocalSnackbarHostState.current
        val hapticFeedback = LocalHapticFeedback.current
        var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
        LaunchedEffect(Unit) {
            isFetchedFailure = runCatching {
                messages = ChaoxingIMHelper.fetchIMHistoryMessages(
                    groupDetail.groups,
                    ChaoxingHttpClient.instance!!,
                    ChaoxingHttpClient.instance!!.getIMConfig(),
                    coroutineScope
                )
            }.onFailure {
                it.snackbarReport(
                    snackbarHostState,
                    coroutineScope,
                    "获取消息记录失败",
                    hapticFeedback
                )
            }
        }
        Crossfade(isFetchedFailure) { v ->
            when {
                v == null -> {
                    CenterCircularProgressIndicator()
                }

                v.isFailure -> {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                messages = ChaoxingIMHelper.fetchIMHistoryMessages(
                                    groupDetail.groups,
                                    ChaoxingHttpClient.instance!!,
                                    ChaoxingHttpClient.instance!!.getIMConfig(),
                                    coroutineScope
                                )
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHostState,
                                    coroutineScope,
                                    "获取消息记录失败",
                                    hapticFeedback
                                )
                            }
                        }
                        isFetchedFailure = null
                    }
                }

                else -> {
                    val groups = groupDetail.groups
                    val chatName = groups.first().chatName
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    navToGroupListDestination()
                                }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_left),
                                contentDescription = null
                            )
                            Spacer(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(5.dp)
                            )
                            Text(
                                "群聊名称：${chatName}" + if (groups.size > 1) " (x${groups.size})" else "",
                                color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                                textAlign = TextAlign.Left,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                        if (groups.size > 1) {
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
                                    "此群组名称在群聊列表里面出现了${groups.size}次，已将所有群组的签到活动合并显示。",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        if (messages!!.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_package_open),
                                    null
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("该群聊暂无可用签到活动")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyColumn {
                                items(messages!!, key = {
                                    it.activeId
                                }) { message ->
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.ContextClick
                                                    )
                                                    coroutineScope.launch {
                                                        runCatching { message.signDestination.await() }
                                                            .onSuccess { onSignAction(it) }
                                                            .onFailure {
                                                                it.snackbarReport(
                                                                    snackbarHostState,
                                                                    coroutineScope,
                                                                    "获取签到信息失败",
                                                                    hapticFeedback
                                                                )
                                                            }
                                                    }
                                                }) {

                                            Icon(
                                                painter = ChaoxingSignHelper.getPredictedSignIcon(
                                                    message.activeTypeName
                                                ),
                                                contentDescription = null,
                                                tint = LocalContentColor.current
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    message.title,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "开始时间：${message.startTimeTitle}",
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        if (isDevelopedMode)
                                            Text(
                                                "activeId：${message.activeId}",
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(start = 52.dp)
                                            )
                                        Spacer(modifier = Modifier.height(16.dp))
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