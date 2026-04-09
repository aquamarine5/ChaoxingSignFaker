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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hyphenate.chat.EMGroup
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

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
        var group by remember { mutableStateOf<List<EMGroup>>(emptyList()) }
        LaunchedEffect(Unit) {
            coroutineScope.launch {
//                ChaoxingIMHelper.loginIM(
//                    ChaoxingHttpClient.instance!!,
//                    context,
//                    ChaoxingIMHelper.getIMConfig(ChaoxingHttpClient.instance!!)
//                )
//                group = ChaoxingIMHelper.getIMGroups()
            }
        }
        Text("暂未完成开发")
        LazyColumn() {
            items(group) { item ->
                Button(onClick = {
                    navToGroupDetail(GroupDetailDestination(item.groupId))
                }) {
                    Text("${item.groupId} ${item.groupName}")
                }
            }
        }
    }
}