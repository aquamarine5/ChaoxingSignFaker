/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity


@Composable
inline fun CourseSignActivityColumnCard(
    activity: ChaoxingSignActivityEntity,
    crossinline onSignAction: (Any) -> Unit
) {
    val isAvailable = activity.status == 1
    val context = LocalContext.current
    val snackbarHost= LocalSnackbarHostState.current
    val hapticFeedback= LocalHapticFeedback.current
    val coroutineScope= rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                if (isAvailable) {
                    ChaoxingSignHelper.getSignDestination(context, activity)?.let {
                        onSignAction(it)
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHost?.showSnackbar("活动未开始或已结束")
                    }
                }
            }) {
        Icon(
            painter = ChaoxingSignHelper.getSignIcon(activity),
            contentDescription = null,
            tint = if (isAvailable) LocalContentColor.current else Color.Gray
        )
        Column {
            Text(activity.nameOne)
            Text("结束时间：${activity.nameFour}")
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}