/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.Orange

@Composable
fun CommonCardTip(
    @DrawableRes icon: Int,
    containerColor: Color,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        modifier = modifier
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
                painterResource(icon),
                contentDescription = "Warning",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            text()
        }
    }
}

@Composable
fun CommonCardTip(
    @DrawableRes icon: Int,
    containerColor: Color,
    text: String,
    modifier: Modifier = Modifier
) = CommonCardTip(
    icon = icon,
    containerColor = containerColor,
    text = {
        Text(
            text,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.fillMaxWidth()
        )
    },
    modifier = modifier
)

@Composable
fun OldSignVersionWarningTips() = CommonCardTip(
    icon = R.drawable.ic_warning,
    containerColor = Orange,
    text = "目前随地大小签测试性的支持了旧版本的签到，可能存在各种问题。由于旧版本签到的测试样本不足，如果在使用期间遇到任何问题，请发送邮件给aquamarine5forever@gmail.com"
)