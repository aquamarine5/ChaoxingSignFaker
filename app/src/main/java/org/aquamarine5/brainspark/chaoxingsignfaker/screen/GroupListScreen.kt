/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

@Serializable
object GroupListDestination

@Composable
fun GroupListScreen(
    imageLoader: ImageLoader,
    navToGroupDetail: (GroupDetailDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .fillMaxSize()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        val hapticFeedback = LocalHapticFeedback.current
        var imGroupsInfo by rememberSaveable { mutableStateOf<List<ChaoxingIMGroup>?>(null) }
        var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
        LaunchedEffect(Unit) {
            isFetchedFailure = runCatching {
                if (imGroupsInfo == null)
                    imGroupsInfo = ChaoxingIMHelper.getIMGroups(
                        ChaoxingHttpClient.instance!!,
                        ChaoxingHttpClient.instance!!.getIMConfig()
                    )
            }.onFailure {
                it.snackbarReport(
                    snackbarHostState,
                    coroutineScope,
                    "获取群列表失败",
                    hapticFeedback
                )
            }
        }
        Crossfade(isFetchedFailure) { v ->
            when {
                v == null || imGroupsInfo == null -> {
                    CenterCircularProgressIndicator()
                }

                v.isFailure -> {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                imGroupsInfo = ChaoxingIMHelper.getIMGroups(
                                    ChaoxingHttpClient.instance!!,
                                    ChaoxingHttpClient.instance!!.getIMConfig()
                                )
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHostState,
                                    coroutineScope,
                                    "获取群列表失败",
                                    hapticFeedback
                                )
                            }

                        }
                        isFetchedFailure = null
                    }
                }

                imGroupsInfo!!.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.align(Alignment.Center)) {
                            Icon(painterResource(R.drawable.ic_circle_question_mark), null)
                            Text("暂无课程，请检查登录的学习通账号是否正确。")
                        }
                    }
                }

                else -> {
                    LazyColumn() {
                        items(imGroupsInfo!!) { item ->
                            Button(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    navToGroupDetail(GroupDetailDestination(item))
                                }, shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item.picArray.let { pics ->
                                            if (pics.size == 1) {
                                                AsyncImage(
                                                    model = pics[0],
                                                    contentDescription = null,
                                                    imageLoader = imageLoader,
                                                    modifier = Modifier.size(50.dp)
                                                )
                                            } else if (pics.size > 1) {
                                                Box(modifier = Modifier.size(50.dp)) {
                                                    Column {
                                                        Row {
                                                            AsyncImage(
                                                                model = pics[0],
                                                                contentDescription = null,
                                                                imageLoader = imageLoader,
                                                                modifier = Modifier.size(25.dp)
                                                            )
                                                            if (pics.size != 2) {
                                                                AsyncImage(
                                                                    model = pics[1],
                                                                    contentDescription = null,
                                                                    imageLoader = imageLoader,
                                                                    modifier = Modifier.size(25.dp)
                                                                )
                                                            } else {
                                                                Spacer(
                                                                    modifier = Modifier.size(
                                                                        25.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        Row {
                                                            if (pics.size > 2) {
                                                                AsyncImage(
                                                                    model = pics[2],
                                                                    contentDescription = null,
                                                                    imageLoader = imageLoader,
                                                                    modifier = Modifier.size(25.dp)
                                                                )
                                                            } else {
                                                                Spacer(
                                                                    modifier = Modifier.size(
                                                                        25.dp
                                                                    )
                                                                )
                                                            }
                                                            if (pics.size == 2) {
                                                                AsyncImage(
                                                                    model = pics[1],
                                                                    contentDescription = null,
                                                                    imageLoader = imageLoader,
                                                                    modifier = Modifier.size(25.dp)
                                                                )
                                                            } else if (pics.size > 3) {
                                                                AsyncImage(
                                                                    model = pics[3],
                                                                    contentDescription = null,
                                                                    imageLoader = imageLoader,
                                                                    modifier = Modifier.size(25.dp)
                                                                )
                                                            } else {
                                                                Spacer(
                                                                    modifier = Modifier.size(
                                                                        25.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Text(
                                            text = item.chatName,
                                            modifier = Modifier.padding(start = 16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}