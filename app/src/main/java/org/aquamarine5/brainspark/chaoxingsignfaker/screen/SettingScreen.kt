/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingRecommendHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AnalyserCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.RecommendHabit
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
            .padding(16.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        var isRecommendEnabled by remember { mutableStateOf(true) }
        val context = LocalContext.current
        val fontGilroy = remember { FontFamily(Font(R.font.gilroy)) }
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val userEntity = remember { ChaoxingHttpClient.instance!!.userEntity }
        var isShowSignoffDialog by remember { mutableStateOf(false) }
        val allRecommendHabits = remember { mutableStateListOf<RecommendHabit>() }
        LaunchedEffect(Unit) {
            context.chaoxingDataStore.data.first().apply {
                isRecommendEnabled = disableRecommend.not()
                allRecommendHabits.addAll(recommendHabitsList)
            }
        }
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
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
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
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                )
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowSignoffDialog = true
                    }
                ) { Icon(painterResource(R.drawable.ic_log_out), null, tint = Color.White) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SponsorCard()

        if (false) {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    3.5.dp, Brush.linearGradient(
                        listOf(
                            Color(0xFF76E4F4),
                            Color(0xFF9E6FCD),
                            Color(0xFFC777A9)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .padding(22.dp, 8.dp, 10.dp, 8.dp)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_brain_cog), null)
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "推测签到活动功能",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 21.sp
                                )
                                Text(
                                    "根据日常的签到时间，在打开应用时推测可能的签到课程和事件（测试中）",
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(isRecommendEnabled, onCheckedChange = { value ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isRecommendEnabled = value
                                coroutineScope.launch {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setDisableRecommend(value.not())
                                            .build()
                                    }
                                }
                            }, modifier = Modifier.padding(start = 8.dp))
                        }
                        AnimatedVisibility(
                            isRecommendEnabled,
                            enter = slideInVertically(),
                            exit = slideOutVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(3.dp))
                                if (!allRecommendHabits.isEmpty()) {
                                    Text("已经学习的签到习惯：", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    allRecommendHabits.forEachIndexed { index, item ->
                                        key(index) {
                                            Card(
                                                elevation = CardDefaults.cardElevation(4.dp),
                                                modifier = Modifier.padding(8.dp, 4.dp, 3.dp, 4.dp)
                                            ) {
                                                Row {
                                                    Text(buildAnnotatedString {
                                                        append("星期${ChaoxingRecommendHelper.dayOfWeekTextList[item.dayOfWeek]}的 ")
                                                        withStyle(SpanStyle(fontFamily = fontGilroy)) {
                                                            append(
                                                                "${item.minuteOfDay.div(60)}:${
                                                                    (item.minuteOfDay % 60).toString()
                                                                        .padStart(2, '0')
                                                                }"
                                                            )
                                                        }
                                                        append(" 在${item.className}的签到活动")
                                                    }, modifier = Modifier.weight(1f))
                                                    IconButton(onClick = {
                                                        allRecommendHabits.removeAt(index)
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.TextHandleMove
                                                        )
                                                        coroutineScope.launch(Dispatchers.IO) {
                                                            context.chaoxingDataStore.updateData { dataStore ->
                                                                dataStore.toBuilder().apply {
                                                                    removeRecommendHabits(index)
                                                                }.build()
                                                            }
                                                        }
                                                    }) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_delete),
                                                            null,
                                                            tint = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        "还没有学习到任何签到习惯，继续更多的使用随地大小签吧~",
                                        fontSize = 13.sp,
                                        lineHeight = 15.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        AnalyserCard()
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                runCatching {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    context.startActivity(Intent(Intent.ACTION_SEND).apply {
                        setData("mailto:aquamarine5forever@gmail.com".toUri())
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
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
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/aquamarine5/ChaoxingSignFaker".toUri()
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
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
        Spacer(modifier = Modifier.height(8.dp))
    }
}