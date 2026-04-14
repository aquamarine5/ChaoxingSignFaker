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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingGroupSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

@Serializable
data class GroupDetailDestination(
    val groupEntity: ChaoxingIMGroup
)

@Composable
fun GroupDetailScreen(
    groupDetail: GroupDetailDestination
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
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                runCatching {
                    if (messages == null)
                        messages = ChaoxingIMHelper.fetchIMHistoryMessages(
                            groupDetail.groupEntity,
                            ChaoxingHttpClient.instance!!,
                            ChaoxingHttpClient.instance!!.getIMConfig()
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
        }
        if (messages == null)
            CenterCircularProgressIndicator()
        else
            LazyColumn {
                items(messages!!) { message ->
                    Text(message.toString())
                }
            }
    }
}