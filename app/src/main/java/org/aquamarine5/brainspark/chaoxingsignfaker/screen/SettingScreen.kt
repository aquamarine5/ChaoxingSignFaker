/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.stackbricks.StackbricksComponent
import org.aquamarine5.brainspark.stackbricks.StackbricksStateService
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuConfiguration
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuMessageProvider
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuPackageProvider
import org.aquamarine5.brainspark.stackbricks.rememberStackbricksStatus
import java.util.concurrent.TimeUnit

@Serializable
object SettingDestination

@Composable
fun SettingScreen(navToOtherUserDestination: () -> Unit) {

    Column {
        val stackbricksState by rememberStackbricksStatus()
        QiniuConfiguration(
            "cdn.aquamarine5.fun",
            referer = "http://cdn.aquamarine5.fun/",
            configFilePath = "chaoxingsignfaker_stackbricks_v1_config.json",
            okHttpClient = ChaoxingHttpClient.instance!!.okHttpClient.newBuilder()
                .callTimeout(20, TimeUnit.MINUTES)
                .readTimeout(20, TimeUnit.MINUTES)
                .writeTimeout(20, TimeUnit.MINUTES)
                .build()
        ).let {
            StackbricksComponent(
                StackbricksStateService(
                    LocalContext.current,
                    QiniuMessageProvider(it),
                    QiniuPackageProvider(it),
                    stackbricksState
                ), checkUpdateOnLaunch = true
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        SponsorCard()
        Button(
            onClick = {
                navToOtherUserDestination()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        *arrayOf(
                            0f to Color(0xff1eeefb),
                            0.6f to Color(0xffdaaaec),
                            0.8f to Color(0xffffe67f)
                        )
                    ), RoundedCornerShape(18.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_users_round),
                    contentDescription = "多用户"
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    "添加其他用户以进行二维码签到",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}