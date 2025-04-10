/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.MainActivity
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore

@Serializable
object WelcomeDestination

@Composable
fun WelcomeScreen(
    navToLoginDestination: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineContext = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            "欢迎使用ChaoxingSignFaker",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(
            modifier = Modifier
                .height(16.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                """
ChaoxingSignFaker 是一个用于模拟超星学习通签到的应用，使用本应用前请您仔细阅读以下内容：
本应用仅可用于学习使用，禁止用于任何商业用途。作者不承担任何因使用本应用而导致的法律责任。
ChaoxingSignFaker 根据 GPL-3.0 协议进行开源。https://github.com/aquamarine5/ChaoxingSignFaker

第三方信息共享清单：

使用SDK名称：友盟SDK
服务类型：使用数据分析
收集个人信息类型：设备信息（IMEI/MAC/Android ID/IDFA/OpenUDID/GUID/IP地址/SIM 卡 IMSI 信息等）
隐私权政策链接：https://www.umeng.com/page/policy

使用SDK名称：百度地图SDK
服务类型：使用地图服务获取签到位置
收集个人信息类型：地理位置信息
隐私权政策链接：https://lbs.baidu.com/index.php?title=openprivacy

使用SDK名称：Sentry SDK
服务类型：使用错误收集服务
收集个人信息类型：设备信息、设备运行截图、设备运行日志
隐私权政策链接：https://sentry.io/trust/privacy/""".trimIndent()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(onClick = {
                coroutineContext.launch {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setAgreeTerms(true).build()
                    }
                }
                UMengHelper.init(context)
                SDKInitializer.setAgreePrivacy(context, true)
                LocationClient.setAgreePrivacy(true)
                navToLoginDestination()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "允许协议并进入应用",
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = {
                    context.startActivity(Intent().apply {
                        setClass(context, MainActivity::class.java)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(MainActivity.INTENT_EXTRA_EXIT_FLAG, true)
                    })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("退出应用", fontSize = 16.sp) }
        }

    }
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen {}
}