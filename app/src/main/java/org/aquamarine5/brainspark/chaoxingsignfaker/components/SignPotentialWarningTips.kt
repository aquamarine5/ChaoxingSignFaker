/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.Orange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours

@Composable
fun SignPotentialWarningTips(
    startTime: Long,
    endTime: Long?,
    isLate: Boolean,
    modifier: Modifier = Modifier,
    isPadding: Boolean = false
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    Column(
        modifier = Modifier
            .padding(horizontal = if (isPadding) 8.dp else 0.dp)
            .then(modifier)
    ) {
        if (isLate)
            CommonCardTip(R.drawable.ic_clock_alert, Orange, {
                Text(
                    if (endTime != null)
                        "此签到已经在 ${dateFormatter.format(Instant.ofEpochMilli(endTime))} 截止，现在签到可能会记为迟到。"
                    else
                        "此签到已经截止或未开始，现在签到可能会记为迟到。",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.fillMaxWidth()
                )
            })
        else
            if (System.currentTimeMillis() - startTime > 6.hours.inWholeMilliseconds)
                CommonCardTip(R.drawable.ic_clock_fading, Orange, {
                    Text(
                        buildAnnotatedString {
                            append(
                                "此签到的发布时间 ${
                                    dateFormatter.format(
                                        Instant.ofEpochMilli(
                                            startTime
                                        )
                                    )
                                } 距离现在已经超过 "
                            )
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("6")
                            }
                            append(" 小时，请确认没有选择错签到事件。")
                        },
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier.fillMaxWidth()
                    )
                })
    }
}