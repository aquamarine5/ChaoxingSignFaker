/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.getThisOrCauseNetworkExceptionMessage

@Composable
fun httpClientGuard(onNavigateBack: () -> Unit): ChaoxingHttpClient? {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var client by remember { mutableStateOf(ChaoxingHttpClient.instance) }
    var isRetrying by remember { mutableStateOf(false) }
    var retryFailure by remember { mutableStateOf<Throwable?>(null) }
    val currentClient = client ?: ChaoxingHttpClient.instance
    if (currentClient != null) return currentClient

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(R.drawable.ic_wifi_off),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "登录状态未就绪",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                retryFailure?.let {
                    it.getThisOrCauseNetworkExceptionMessage()
                        ?: it.localizedMessage
                        ?: "网络连接失败，请检查网络后重试。"
                } ?: "网络连接失败或登录已过期，请检查网络后重试。",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            if (isRetrying) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    coroutineScope.launch {
                        isRetrying = true
                        retryFailure = null
                        runCatching {
                            withContext(Dispatchers.IO) {
                                ChaoxingHttpClient.instance
                                    ?: ChaoxingHttpClient.loadFromDataStore(
                                        context.chaoxingDataStore.data.first(),
                                        context
                                    )
                            }
                        }.onSuccess {
                            client = it
                        }.onFailure {
                            if (it is CancellationException) throw it
                            retryFailure = it
                        }
                        isRetrying = false
                    }
                }) {
                    Text("重试")
                }
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
            }
        }
    }
    return null
}
