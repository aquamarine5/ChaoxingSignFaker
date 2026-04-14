/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun NotReadyToSignNoticeComponent(
    onSignForOtherUser: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isExpiredSign: Boolean = false,
    navBack: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(
                if (isExpiredSign) {
                    R.drawable.ic_clipboard_x
                } else {
                    R.drawable.ic_clipboard_check
                }
            ),
            contentDescription = if (isExpiredSign) {
                "签到已截止"
            } else {
                "已签到"
            },
            tint = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (isExpiredSign) {
                "当前签到活动已经截止，无法签到。"
            } else {
                "当前签到活动已经签到，不能重复签到。"
            }, color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        onSignForOtherUser?.let {
            Spacer(modifier = Modifier.width(6.dp))
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                it.invoke()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isExpiredSign) {
                        "仍为其他用户签到"
                    } else {
                        "为其他用户签到"
                    }
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedButton(onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            navBack()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("返回")
        }
        Spacer(modifier = Modifier.width(6.dp))
        TextButton(onClick = {
            onDismiss()
        }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isExpiredSign) {
                    "我认为这是BUG，签到并没有截止。"
                } else {
                    "我认为这是BUG，我并没有签到。"
                }
            )
        }
    }
}