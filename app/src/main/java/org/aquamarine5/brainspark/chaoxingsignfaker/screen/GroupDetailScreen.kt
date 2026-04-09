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
import androidx.compose.ui.unit.dp
import com.hyphenate.chat.EMMessage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class GroupDetailDestination(
    val groupId: String
)

@Composable
fun GroupDetailScreen(
    groupDetail: GroupDetailDestination
) {
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .fillMaxSize()
//            .verticalScroll(rememberScrollState())
    ) {
        val coroutineScope = rememberCoroutineScope()
        var messages by remember { mutableStateOf<List<EMMessage>>(emptyList()) }
        LaunchedEffect(Unit) {
            coroutineScope.launch {
//                messages = ChaoxingIMHelper.getIMGroupHistoryMessages(groupDetail.groupId).data
            }
        }
        LazyColumn {
            items(messages) { message ->
                Text(message.body.toString())
                Text(message.ext().toString())
            }
        }
    }
}