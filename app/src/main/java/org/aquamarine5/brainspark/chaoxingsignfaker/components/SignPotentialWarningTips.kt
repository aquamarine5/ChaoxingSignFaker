/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.Orange
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Composable
fun SignPotentialWarningTips(startTime: Long, endTime: Long?, isLate: Boolean) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    if (isLate)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Orange
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp, 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp, 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_clock_alert),
                    contentDescription = "Help",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(9.dp))
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
            }
        }
    else
        if (System.currentTimeMillis() - startTime > TimeUnit.HOURS.toMillis(6))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Orange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp, 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp, 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_clock_alert),
                        contentDescription = "Help",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        "此签到的发布时间 ${dateFormatter.format(Instant.ofEpochMilli(startTime))} 距离现在已经超过 6 小时，请确认没有选择错签到事件。",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
}