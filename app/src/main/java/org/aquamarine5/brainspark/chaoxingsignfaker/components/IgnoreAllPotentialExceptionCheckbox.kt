/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun IgnoreAllPotentialExceptionCheckbox(
    isIgnoreAllPotentialExceptions: MutableState<Boolean>,
) {
    var internalIsIgnoreAllPotentialExceptions by isIgnoreAllPotentialExceptions
    val hapticFeedback = LocalHapticFeedback.current
    var isDialog by remember { mutableStateOf(false) }
    if (isDialog) {
        AlertDialog(onDismissRequest = {
            isDialog = false
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_circle_question_mark),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }, text = {
            Text(
                buildAnnotatedString {

                }
            )
        }, confirmButton = {
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                isDialog = false
            }) {
                Text("我知道了")
            }
        })
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(internalIsIgnoreAllPotentialExceptions, onCheckedChange = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            internalIsIgnoreAllPotentialExceptions = it
        })
        Text(
            "忽略可能的错误，强制签到",
            color = if (internalIsIgnoreAllPotentialExceptions) Color.Red else LocalContentColor.current,
            modifier = Modifier.clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                internalIsIgnoreAllPotentialExceptions = !internalIsIgnoreAllPotentialExceptions
            }
        )
        IconButton(onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            isDialog = true
        }) {
            Icon(
                painterResource(R.drawable.ic_circle_question_mark),
                null,
                tint = Color.Gray, modifier = Modifier.size(20.dp)
            )
        }
    }
}