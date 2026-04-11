/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
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
        var imGroupsInfo by rememberSaveable { mutableStateOf<List<ChaoxingIMGroup>>(emptyList()) }
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                runCatching {
                    if (imGroupsInfo.isEmpty())
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
        LazyColumn() {
            items(imGroupsInfo) { item ->
                Button(onClick = {
                    navToGroupDetail(GroupDetailDestination(item))
                }) {
                    Text("${item.chatName} isGroup: ${item.isGroup}")
                }
            }
        }
    }
}