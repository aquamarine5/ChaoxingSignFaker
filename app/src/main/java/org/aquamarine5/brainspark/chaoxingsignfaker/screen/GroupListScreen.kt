/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

@Serializable
object GroupListDestination

@Composable
fun GroupListScreen(
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
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                runCatching {
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
        }
        if (imGroupsInfo == null) {
            CenterCircularProgressIndicator()
        } else
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
                                            modifier = Modifier.size(50.dp)
                                        )
                                    } else if (pics.size > 1) {
                                        Box(modifier = Modifier.size(50.dp)) {
                                            Column {
                                                Row {
                                                    AsyncImage(
                                                        model = pics[0],
                                                        contentDescription = null,
                                                        modifier = Modifier.size(25.dp)
                                                    )
                                                    if (pics.size != 2) {
                                                        AsyncImage(
                                                            model = pics[1],
                                                            contentDescription = null,
                                                            modifier = Modifier.size(25.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(25.dp))
                                                    }
                                                }
                                                Row {
                                                    if (pics.size > 2) {
                                                        AsyncImage(
                                                            model = pics[2],
                                                            contentDescription = null,
                                                            modifier = Modifier.size(25.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(25.dp))
                                                    }
                                                    if (pics.size == 2) {
                                                        AsyncImage(
                                                            model = pics[1],
                                                            contentDescription = null,
                                                            modifier = Modifier.size(25.dp)
                                                        )
                                                    } else if (pics.size > 3) {
                                                        AsyncImage(
                                                            model = pics[3],
                                                            contentDescription = null,
                                                            modifier = Modifier.size(25.dp)
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(25.dp))
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
                }
            }
    }
}