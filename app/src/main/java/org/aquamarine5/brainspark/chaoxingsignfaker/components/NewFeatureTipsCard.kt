/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun NewFeatureTipsCard(
    isDisplay: MutableState<Boolean>,
    tipsText: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    AnimatedVisibility(isDisplay.value, enter = slideInVertically(), exit = slideOutVertically()) {
        Column {
            Card(
                modifier = modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(
                        11.dp, 8.dp
                    )
                ) {
                    Icon(
                        painterResource(R.drawable.ic_sparkles),
                        null,
                        tint = Color.Gray,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tipsText,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onDismiss()
                            isDisplay.value = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_x),
                            contentDescription = "关闭提示"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}