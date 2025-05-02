/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AnalyserCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.stackbricks.StackbricksComponent
import org.aquamarine5.brainspark.stackbricks.StackbricksEventTrigger
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData

@Serializable
object SettingGraphDestination

@Serializable
object SettingDestination

@Composable
fun SettingScreen(
    stackbricksService: StackbricksService,
    imageLoader: ImageLoader,
    naviToLoginScreen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val context = LocalContext.current
        val fontGilroy = FontFamily(
            Font(R.font.gilroy)
        )
        val coroutineScope = rememberCoroutineScope()
        val userEntity = ChaoxingHttpClient.instance!!.userEntity
        var isShowSignoffDialog by remember { mutableStateOf(false) }
        StackbricksComponent(
            stackbricksService,
            trigger = object : StackbricksEventTrigger() {
                override fun onChannelChanged(isTestChannel: Boolean) {
                    UMengHelper.onStackbricksTestChannelChangedEvent(
                        context,
                        userEntity,
                        isTestChannel
                    )
                }

                override fun onCheckUpdate(isTestChannel: Boolean) {
                    UMengHelper.onStackbricksCheckUpdateEvent(context, userEntity)
                }

                override fun onCheckUpdateOnLaunchChanged(isChecked: Boolean) {
                    UMengHelper.onStackbricksCheckOnLaunchChangedEvent(
                        context,
                        userEntity,
                        isChecked
                    )
                }

                override fun onDownloadPackage() {

                }

                override fun onInstallPackage(
                    isTestChannel: Boolean,
                    versionData: StackbricksVersionData
                ) {
                    if (isTestChannel)
                        UMengHelper.onStackbricksInstallTestChannelEvent(
                            context,
                            userEntity,
                            versionData
                        )
                    else
                        UMengHelper.onStackbricksInstallNewestEvent(
                            context,
                            userEntity,
                            versionData
                        )
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isShowSignoffDialog) {
            AlertDialog(
                onDismissRequest = { isShowSignoffDialog = false },
                title = { Text("确定要登出吗？") },
                text = {
                    Text(buildAnnotatedString {
                        append("当你登出时，你的签到统计数据不会丢失，但是星标课程")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("会被清除")
                        }
                        append("。")
                    })
                },
                dismissButton = {
                    OutlinedButton(onClick = { isShowSignoffDialog = false }) {
                        Text("取消")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                context.chaoxingDataStore.updateData {
                                    it.toBuilder()
                                        .clearPreferClassId()
                                        .clearLoginSession()
                                        .build()
                                }
                                UMengHelper.profileSignOff()
                                naviToLoginScreen()
                            }
                        }
                    ) {
                        Text("登出")
                    }
                }
            )
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0ADA0))
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp, 8.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    ChaoxingHttpClient.instance!!.userEntity.pic,
                    imageLoader = imageLoader,
                    contentDescription = "头像",
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .clip(
                            RoundedCornerShape(5.dp)
                        )

                )
                Text(
                    "登录用户：${ChaoxingHttpClient.instance!!.userEntity.name}",
                    modifier = Modifier
                        .padding(8.dp, 0.dp)
                        .weight(1f),
                    fontWeight = FontWeight.Medium,
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                )
                Button(
                    onClick = {
                        isShowSignoffDialog = true
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF07C82)
                    )
                ) { Text("登出") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SponsorCard()

        AnalyserCard()
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                        setData(Uri.parse("mailto:aquamarine5forever@gmail.com"))
                        putExtra(Intent.EXTRA_EMAIL, "aquamarine5forever@gmail.com")
                        putExtra(Intent.EXTRA_CC, "aquamarine5forever@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Send to ChaoxingSignFaker:\n")
                        putExtra(Intent.EXTRA_TEXT, "Your content:")
                    })
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC08EAF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_mail), contentDescription = "mail")
                Spacer(modifier = Modifier.width(8.dp))
                Text(buildAnnotatedString {
                    append("想要联系作者？\n发送邮件到：")
                    withStyle(
                        SpanStyle(
                            fontFamily = fontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("aquamarine5forever")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)){
                        append("@")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = fontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("gmail.com")
                    }
                })
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/aquamarine5/ChaoxingSignFaker")
                        )
                    )
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55BB8A))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_github), contentDescription = "github")
                Spacer(modifier = Modifier.width(8.dp))
                Text(buildAnnotatedString {
                    append("前往Github给作者点一个Star吧\n前往：")
                    withStyle(
                        SpanStyle(
                            fontFamily = fontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("aquamarine5")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)){
                        append("/")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = fontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("ChaoxingSignFaker")
                    }
                })
            }
        }
    }
}