/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun AlreadySignedNotice(onSignForOtherUser: (() -> Unit)?, navBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_clipboard_check),
            contentDescription = "已签到",
            tint = MaterialTheme.colorScheme.onBackground
        )
        Text("当前签到活动已经签到，不能重复签到。", color = MaterialTheme.colorScheme.onBackground)
        onSignForOtherUser?.let {
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(onClick = {
                it.invoke()
            }) {
                Text("为其他用户签到")
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Button(onClick = {
            navBack()
        }) {
            Text("返回")
        }
    }
}