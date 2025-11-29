/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.getNetworkExceptionMessage

@Composable
fun NetworkExceptionComponent(
    exception: Throwable,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val networkExceptionTips = remember(exception) {
            exception.getNetworkExceptionMessage()
        }
        Column(modifier = Modifier.align(Alignment.Center)) {
            Icon(
                if (networkExceptionTips == null) painterResource(R.drawable.ic_message_circle_x) else painterResource(
                    R.drawable.ic_wifi_off
                ),
                null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                networkExceptionTips ?: "啊哦~出现了未知错误。${exception.localizedMessage}",
                lineHeight = 17.sp,
                fontSize = 14.sp
            )
            Button(onClick = { onRetry() }) {
                Text("重试")
            }
        }
    }
}