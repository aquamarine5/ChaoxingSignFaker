/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.Orange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
inline fun CourseSignActivityColumnCard(
    activity: ChaoxingSignActivityEntity,
    crossinline onSignAction: (Any) -> Unit
) {
    val isAvailable = activity.status == 1
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHostState.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                if (isAvailable) {
                    ChaoxingSignHelper.getSignDestination(context, activity)?.let {
                        onSignAction(it)
                    }
                } else {
                    snackbarHost.displaySnackbar("警告：活动未开始或已结束", coroutineScope)
                    ChaoxingSignHelper.getSignDestination(context, activity, true)?.let {
                        onSignAction(it)
                    }
                }
            }) {
        val currentTime = remember { System.currentTimeMillis() }
        BadgedBox(badge = {
            if (currentTime - activity.startTime <= 600000) {
                Box(contentAlignment = Alignment.Center){
                    Badge(
                        containerColor = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(16.dp).zIndex(0f)
                    )
                    Badge(
                        containerColor = Orange,
                        modifier = Modifier.size(10.dp).zIndex(10f)
                    )
                }

            }
        }) {
            Icon(
                painter = ChaoxingSignHelper.getSignIcon(activity),
                contentDescription = null,
                tint = if (isAvailable) LocalContentColor.current else Color.Gray
            )
        }
        val formatter =
            remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(activity.nameOne, fontWeight = FontWeight.Bold)
            Text(
                "开始时间：${
                    formatter.format(
                        Instant.ofEpochMilli(activity.startTime).atZone(ZoneId.systemDefault())
                    )
                }",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = Color.Gray
            )
            Text(
                "结束时间：${activity.nameFour.ifEmpty { "手动结束" }}",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = Color.Gray
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}