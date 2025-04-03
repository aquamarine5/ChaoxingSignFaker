/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.RequireLoginAlertDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

@Serializable
object OtherUserDestination

@Composable
fun OtherUserScreen(naviBack: () -> Unit) {
    Scaffold { innerPadding ->
        val context = LocalContext.current
        val isQRCodeScanPause = remember { mutableStateOf(false) }
        var isQRCodeScanning by remember { mutableStateOf(false) }
        var isQRCodeIllegal by remember { mutableStateOf(false) }
        val isQRCodeParsing = remember { mutableStateOf(false) }
        var isQRCodeImportSuccess by remember { mutableStateOf(false) }
        var isLocalSharedEntityReady by remember { mutableStateOf<Boolean?>(null) }
        var currentImportData by remember { mutableStateOf("") }
        var qrcodeIllegalText by remember { mutableStateOf("") }
        var importSharedEntity by remember { mutableStateOf<ChaoxingOtherUserSharedEntity?>(null) }
        val otherUserSessions = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
        var qrCode by remember { mutableStateOf<Bitmap?>(null) }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            isLocalSharedEntityReady =
                ChaoxingOtherUserHelper.checkSharedEntity(context.chaoxingDataStore.data.first())
        }
        var job: Job? = null
        BackHandler(isQRCodeScanning) {
            isQRCodeScanning = false
        }
        if (isLocalSharedEntityReady == false) {
            RequireLoginAlertDialog(naviBack) {
                importSharedEntity = it
                isLocalSharedEntityReady = true
            }
        } else if (isLocalSharedEntityReady == true) {
            LaunchedEffect(Unit) {
                qrCode = ChaoxingOtherUserHelper.generateQRCode(context, importSharedEntity)
            }
        }
        Box(
            modifier = Modifier
                .zIndex(0f)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val qrcodeSize = ChaoxingOtherUserHelper.getQRCodeDpSize(context)
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(9.dp)
                        )
                        .size(qrcodeSize + 18.dp, qrcodeSize + 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCode != null) {
                        Image(bitmap = qrCode!!.asImageBitmap(), contentDescription = "QR Code")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        append("使用其他设备打开 ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("随地大小签APP")
                        }
                        append(" 扫描二维码\n以将你的账号添加到其他设备中")
                    },
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8D86A)
                    ), modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp, 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painterResource(R.drawable.ic_triangle_alert),
                            contentDescription = "Alert",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "随意分享此二维码给他人会增加你的学习通账号风险，他人可以通过二维码登录从而控制你的账号，但这个分享行为并不会暴露你的明文密码。",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        isQRCodeScanning = true
                    }, shape = RoundedCornerShape(18.dp), modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp, 0.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_user_round_plus),
                            contentDescription = "Add User"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加其他用户", fontSize = 16.sp)
                    }
                }
                LaunchedEffect(Unit) {
                    context.chaoxingDataStore.data.first().let {
                        otherUserSessions.addAll(it.otherUsersList)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            "已添加的用户",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    if (otherUserSessions.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无其他用户",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else
                        otherUserSessions.forEachIndexed { index, user ->
                            key(user.phoneNumber) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(17.dp, 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${user.name} (${user.phoneNumber})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    context.chaoxingDataStore.updateData { datastore ->
                                                        datastore.toBuilder()
                                                            .apply { removeOtherUsers(index) }
                                                            .build()
                                                    }
                                                }
                                                otherUserSessions.removeIf { it.phoneNumber == user.phoneNumber }
                                            },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = "Delete"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        }
        if (isQRCodeImportSuccess) {
            AlertDialog(
                onDismissRequest = {
                    isQRCodeImportSuccess = false
                },
                title = {
                    Text("导入成功")
                },
                text = {
                    Text("$currentImportData 用户已经成功导入")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isQRCodeImportSuccess = false
                        }
                    ) {
                        Text("确定")
                    }
                }
            )
        }
        AnimatedVisibility(
            isQRCodeScanning, enter =
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(
                animationSpec = tween(300)
            ), exit =
            scaleOut(targetScale = 0.8f, animationSpec = tween(300)) + fadeOut(
                animationSpec = tween(300)
            )
        ) {
            Column(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QRCodeScanComponent(isQRCodeScanPause, isQRCodeParsing, onClose = {
                    isQRCodeScanning = false
                }, onScanResult = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                isQRCodeParsing.value = true
                                isQRCodeScanPause.value = true
                                return@runCatching ChaoxingOtherUserSharedEntity.parseFromQRCode(it)
                            }.onSuccess { sharedEntity ->
                                coroutineScope.launch {
                                    runCatching {
                                        ChaoxingOtherUserHelper.saveOtherUser(context, sharedEntity)
                                    }.onSuccess {
                                        isQRCodeScanning = false
                                        isQRCodeParsing.value = false
                                        currentImportData =
                                            "${sharedEntity.userName}(手机号：${sharedEntity.phoneNumber})"
                                        isQRCodeImportSuccess = true
                                        otherUserSessions.add(it)
                                        UMengHelper.onAccountOtherUserAddEvent(context, it)
                                    }.onFailure {
                                        it.printStackTrace()
                                        Sentry.captureException(it)
                                        isQRCodeIllegal = true
                                        isQRCodeParsing.value = false
                                        qrcodeIllegalText = it.message ?: "二维码解析失败，登录失败。"
                                        job?.cancel()
                                        job = coroutineScope.launch {
                                            delay(3000)
                                            isQRCodeScanPause.value = false
                                            isQRCodeIllegal = false
                                        }
                                    }
                                }
                            }.onFailure {
                                it.printStackTrace()
                                isQRCodeIllegal = true
                                isQRCodeScanPause.value = true
                                qrcodeIllegalText = it.message ?: "二维码解析失败，不是正确码。"
                                job?.cancel()
                                job = coroutineScope.launch {
                                    delay(3000)
                                    isQRCodeScanPause.value = false
                                    isQRCodeIllegal = false
                                }
                            }
                        }
                    }
                }) {
                    Column(
                        modifier = Modifier
                            .offset(y = Dp(context.resources.displayMetrics.run {
                                0.75f * heightPixels / density
                            }))
                            .zIndex(2f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Crossfade(isQRCodeIllegal) {
                            when (it) {
                                true -> {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                Color(0x72F1441D),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                BorderStroke(2.dp, Color(0xFFF1441D)),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_octagon_alert),
                                            contentDescription = "Illegal QR Code"
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(qrcodeIllegalText)
                                    }
                                }

                                false -> {

                                    Row(
                                        modifier = Modifier
                                            .background(
                                                Color(0x88888888),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                BorderStroke(2.dp, Color(0xFF444444)),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_scan_qr_code),
                                            contentDescription = "Scan QR Code"
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("扫描其他设备的二维码以添加用户")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}