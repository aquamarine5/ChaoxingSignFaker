package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
    navController: NavController) {
    val context = LocalContext.current.applicationContext
    val coroutineContext = rememberCoroutineScope()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
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
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(onClick = {
                    coroutineContext.launch {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().setAgreeTerms(true).build()
                        }
                    }
                    UMengHelper.init(context)
                    SDKInitializer.setAgreePrivacy(context, true)
                    navController.navigate(LoginDestination)
                }) { Text("允许协议并进入应用") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent().apply {
                        setClass(context, MainActivity::class.java)
                        putExtra(MainActivity.INTENT_EXTRA_EXIT_FLAG, true)
                    })
                }) { Text("退出应用") }
            }
        }
    }
}

@Preview
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(rememberNavController())
}