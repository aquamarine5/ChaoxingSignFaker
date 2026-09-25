/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AnalyserCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CurrentDataStoreDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CustomizeClientCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SnackbarAlertDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.FontGilroy
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.OnlyAppDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.sentryReport
import org.aquamarine5.brainspark.stackbricks.StackbricksComponent
import org.aquamarine5.brainspark.stackbricks.StackbricksEventTrigger
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData

@Serializable
object SettingGraphDestination

@Serializable
object SettingDestination

private const val BYPASS_BLOCKED_CHECKING_KEY = "ggg1215love"

@OnlyAppDevelopedMode
private const val COMMAND_SET_RANK_COUNT_PREFIX = "setRankCount"

@OnlyAppDevelopedMode
private const val COMMAND_ALWAYS_FORCE_SIGN_PREFIX = "alwaysForceSign "

var isAlwaysForceSign by mutableStateOf(false)

@Composable
fun SettingScreen(
    stackbricksService: StackbricksService,
    naviToLoginScreen: () -> Unit,
    naviToFavoriteLocationSetting: () -> Unit = {}
) {
    val imageLoader = LocalImageLoader.current
    Column(
        modifier = Modifier
            .padding(16.dp, 4.dp, 16.dp, 0.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val context = LocalContext.current
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val userEntity = remember { ChaoxingHttpClient.instance!!.userEntity }
        val displayUserEntity =
            (ChaoxingHttpClient.cloneInstance ?: ChaoxingHttpClient.instance!!).userEntity
        var isShowSignoffDialog by remember { mutableStateOf(false) }
        var isBypassBlockedChecking by remember { mutableStateOf(false) }
        var isUnblockDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            context.chaoxingDataStore.data.first().apply {
                isBypassBlockedChecking = bypassBlockedChecking
            }
            launch(Dispatchers.IO) {
                stackbricksService.deleteTemp()
            }
        }
        if (isUnblockDialog) {
            var inputPassword by remember { mutableStateOf("") }
            SnackbarAlertDialog(onDismissRequest = {
                isUnblockDialog = false
            }, title = {
                Text("输入密码：")
            }, text = {
                TextField(inputPassword, onValueChange = {
                    inputPassword = it
                }, label = {
                    Text("密码")
                })
            }, dismissButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isUnblockDialog = false
                }) {
                    Text("取消")
                }
            }, confirmButton = {
                Button(onClick = {
                    if (inputPassword == BYPASS_BLOCKED_CHECKING_KEY) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        coroutineScope.launch(Dispatchers.IO) {
                            isBypassBlockedChecking = true
                            snackbarHostState.displaySnackbar(
                                "成功解锁@BypassBlockedChecking",
                                coroutineScope
                            )
                            context.chaoxingDataStore.updateData {
                                it.toBuilder().setBypassBlockedChecking(true).build()
                            }
                        }
                    } else if (inputPassword.startsWith(COMMAND_SET_RANK_COUNT_PREFIX)) {
                        inputPassword.substringAfter(COMMAND_SET_RANK_COUNT_PREFIX).toIntOrNull()
                            ?.let { count ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setPreferences(
                                            it.preferences.toBuilder()
                                                .setDisplayRankCount(count.coerceAtLeast(5))
                                        ).build()
                                    }
                                    snackbarHostState.displaySnackbar(
                                        "已设置排行榜显示数量为$count",
                                        coroutineScope
                                    )
                                }
                            }
                    } else if (inputPassword.startsWith(COMMAND_ALWAYS_FORCE_SIGN_PREFIX)) {
                        inputPassword.substringAfter(COMMAND_ALWAYS_FORCE_SIGN_PREFIX)
                            .toBooleanStrictOrNull()
                            ?.let { value ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder().setPreferences(
                                            it.preferences.toBuilder()
                                                .setAlwaysForceSign(value)
                                        ).build()
                                    }
                                    snackbarHostState.displaySnackbar(
                                        "已设置${if (value) "总是强制签到" else "不总是强制签到"}",
                                        coroutineScope
                                    )
                                }
                            }
                    } else {
                        snackbarHostState.displaySnackbar(
                            "密码错误",
                            coroutineScope
                        )
                    }
                }) {
                    Text("确认")
                }
            })
        }
        StackbricksComponent(
            stackbricksService,
            trigger = object : StackbricksEventTrigger() {
                override fun onChannelChanged(isTestChannel: Boolean) {
                    UMengHelper.onStackbricksTestChannelChangedEvent(
                        context,
                        ChaoxingHttpClient.instance!!.name,
                        isTestChannel
                    )
                }

                override fun onCheckUpdate(isTestChannel: Boolean) {
                    UMengHelper.onStackbricksCheckUpdateEvent(context, ChaoxingHttpClient.instance!!.name)
                }

                override fun onCheckUpdateOnLaunchChanged(isChecked: Boolean) {
                    UMengHelper.onStackbricksCheckOnLaunchChangedEvent(
                        context,
                        ChaoxingHttpClient.instance!!.name,
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
                            ChaoxingHttpClient.instance!!.name,
                            versionData
                        )
                    else
                        UMengHelper.onStackbricksInstallNewestEvent(
                            context,
                            ChaoxingHttpClient.instance!!.name,
                            versionData
                        )
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isShowSignoffDialog) {
            SnackbarAlertDialog(
                onDismissRequest = { isShowSignoffDialog = false },
                title = { Text("确定要登出吗？") },
                text = {
                    Text("当你登出时，你的签到统计数据和代签用户不会丢失。")
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isShowSignoffDialog = false
                    }) {
                        Text("取消")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            coroutineScope.launch {
                                ChaoxingHttpClient.cloneInstance = null
                                context.chaoxingDataStore.updateData {
                                    it.toBuilder()
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
                    displayUserEntity.pic,
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
                    "登录用户：${displayUserEntity.name}",
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

        Button(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                naviToFavoriteLocationSetting()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FB0DC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_map_pinned),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "收藏的签到位置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Text(
                        "在地图上收藏常用的签到位置，位置签到时可以直接选用。",
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnalyserCard()
        Spacer(modifier = Modifier.height(8.dp))
        CustomizeClientCard()
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
                }.onFailure {
                    snackbarHostState.displaySnackbar("无法打开链接", coroutineScope)
                    it.sentryReport()
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
                            fontFamily = FontGilroy,
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
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("ChaoxingSignFaker")
                    }
                })
            }
        }

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
                }.onFailure {
                    snackbarHostState.displaySnackbar("无法打开邮件应用", coroutineScope)
                    it.sentryReport()
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
                            fontFamily = FontGilroy,
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
                            fontFamily = FontGilroy,
                            fontSize = 14.sp
                        )
                    ) {
                        append("gmail.com")
                    }
                })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        var clickCount by remember { mutableIntStateOf(0) }
        Text(
            "ChaoxingSignFaker ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})," +
                    " channel: ${BuildConfig.umengChannel}," +
                    " buildDate: ${BuildConfig.releaseDate}," +
                    " " +
                    "${if (isBypassBlockedChecking) " BypassBlockedChecking," else ""} " +
                    "developed by @aquamarine5, All Rights Reserved.",
            fontSize = 10.sp,
            lineHeight = 12.sp,
            color = Color.Gray,
            modifier = Modifier.clickable {
                if (isBypassBlockedChecking || clickCount++ == 0) {
                    val clipboard =
                        context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(
                            0
                        )?.text
                    if (clipboard == BYPASS_BLOCKED_CHECKING_KEY) {
                        isBypassBlockedChecking = true
                        snackbarHostState.displaySnackbar(
                            "成功解锁@BypassBlockedChecking",
                            coroutineScope
                        )
                        coroutineScope.launch(Dispatchers.IO) {
                            context.chaoxingDataStore.updateData {
                                it.toBuilder().setBypassBlockedChecking(true).build()
                            }
                        }
                    } else {
                        isUnblockDialog = true
                    }
                } else if (clickCount >= 2)
                    Intent(
                        Intent.ACTION_VIEW,
                        "orpheus://playlist/13697614404".toUri()
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK

                        if (context.packageManager.resolveActivity(this, 0) != null) {
                            context.startActivity(this)
                        } else {
                            this.data = "https://music.163.com/playlist?id=13697614404".toUri()
                            context.startActivity(this)
                        }
                    }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        var isUiDevelopedMode by remember { mutableStateOf(isDevelopedMode) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(isUiDevelopedMode, onCheckedChange = { value ->
                isUiDevelopedMode = value
                isDevelopedMode = value
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsDevelopedMode(value).build()
                        ).build()
                    }
                }
            })
            Text("启用开发模式", modifier = Modifier.clickable {
                isUiDevelopedMode = !isUiDevelopedMode
                isDevelopedMode = !isDevelopedMode
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setPreferences(
                            it.preferences.toBuilder().setIsDevelopedMode(isDevelopedMode).build()
                        ).build()
                    }
                }
            })
        }

        @OnlyAppDevelopedMode AnimatedVisibility(
            isUiDevelopedMode,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Column {
                var isDataStoreDialogVisible by remember { mutableStateOf(false) }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().clearLearntTooltips().build()
                        }
                    }
                }) {
                    Text("ResetAllStoredLearntTooltips")
                }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isDataStoreDialogVisible = true
                }) {
                    Text("LoadCurrentDataStore")
                }
                if (isDataStoreDialogVisible) {
                    CurrentDataStoreDialog(onDismissRequest = {
                        isDataStoreDialogVisible = false
                    })
                }
            }
        }
    }
}
