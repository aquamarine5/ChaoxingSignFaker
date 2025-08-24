/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.os.Debug
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse

private const val UNBLOCKED_BUTTON_CLICK_LIMIT = 10

@Composable
fun BlockedContent(content: @Composable () -> Unit) {
    var bannedFidList by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val context = LocalContext.current
    var unblockedButtonClickCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (bannedFidList.isNotEmpty()) {
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            ChaoxingHttpClient.instance?.okHttpClient?.newCall(
                Request.Builder()
                    .get()
                    .url("http://cdn.aquamarine5.fun/chaoxingsignfaker_banlist.json")
                    .build()
            )?.execute().use {
                if (it?.checkResponse(context) == true) {
                    return@use
                }
                bannedFidList =
                    JSONObject.parseObject(it?.body?.string()).getJSONArray("banfids")
                        .toList(Int::class.java)
            }
        }
    }
    Crossfade(
        if (Debug.isDebuggerConnected()) {
            false
        } else {
            unblockedButtonClickCount < UNBLOCKED_BUTTON_CLICK_LIMIT && bannedFidList.contains(
                ChaoxingHttpClient.instance!!.userEntity.fid
            )
        }
    ) {
        if (it) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = {
                    unblockedButtonClickCount++
                }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_user_lock), null)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("受限于应用策略，当前账号无法使用此功能")
            }
        } else {
            LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.Confirm)
            content()
        }
    }
}