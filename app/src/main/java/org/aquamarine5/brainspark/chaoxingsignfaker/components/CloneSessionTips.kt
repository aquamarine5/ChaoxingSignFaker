/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

@Composable
fun CloneSessionTips(onExitCloning: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_square_stack), null)
        Spacer(modifier = Modifier.width(4.dp))
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text("当前克隆登录用户：")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        ChaoxingHttpClient.cloneInstance?.userEntity?.pic, null
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            ChaoxingHttpClient.cloneInstance?.userEntity?.name ?: "未知用户",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 14.sp
                        )
                        Text(
                            " (${ChaoxingHttpClient.cloneInstance?.userEntity?.schoolName[0]})",
                            color = Color.Gray, fontSize = 10.sp, lineHeight = 10.sp
                        )
                    }
                }
            }
            Button(onClick = {
                onExitCloning()
            }
            ) {
                Text("退出克隆")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}