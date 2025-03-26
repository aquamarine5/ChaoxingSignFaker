/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingQRCodeSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

@Serializable
data class QRCodeSignDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): QRCodeSignDestination {
            return QRCodeSignDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@Composable
fun QRCodeSignScreen(destination: QRCodeSignDestination, navBack: () -> Unit) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isCurrentAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val signer = ChaoxingQRCodeSigner(ChaoxingHttpClient.instance!!, destination)
    val context = LocalContext.current
    var isMapRequired by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching {
            isAlreadySigned = signer.preSign()
            isMapRequired = signer.getQRCodeSignInfo().isPositionRequired
        }.onFailure {
            Sentry.captureException(it)
            Toast.makeText(context, "获取签到事件详情失败", Toast.LENGTH_SHORT).show()
        }
    }
    when (isAlreadySigned) {
        true -> {
            AlreadySignedNotice(onSignForOtherUser = {
                isAlreadySigned = false
                isCurrentAlreadySigned = true
            }) { navBack() }
        }

        false -> {
            var isQRCodeScanning by remember { mutableStateOf(false) }
            var isQRCodeScanPause = remember { mutableStateOf(false) }
            var isQRCodeParsing = remember { mutableStateOf(false) }
            var isQRCodeIllegal by remember { mutableStateOf(false) }
            var isMapGetting by remember { mutableStateOf(false) }
            var isSignExecuted by remember { mutableStateOf(false) }
            var qrcodeIllegalText by remember { mutableStateOf("二维码不合法") }
            val signUserList = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
            var locationData by remember { mutableStateOf<ChaoxingLocationSignEntity?>(null) }
            var job by remember { mutableStateOf<Job?>(null) }
            val userSelections = remember { mutableStateListOf(true) }
            val signStatus = remember { mutableStateListOf(ChaoxingSignStatus()) }
            Scaffold { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(8.dp)
                        .zIndex(0f)
                ) {
                    Column {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(83, 83, 83)
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
                                    painterResource(R.drawable.ic_info),
                                    contentDescription = "Info",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    buildAnnotatedString {
                                        append("通常情况下，")
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("随地大小签")
                                        }
                                        append(" 的二维码签到功能是用于给其他用户签到的，而不是用于仅给自己签到。")
                                    },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.W500
                                )
                            }
                        }
                        LaunchedEffect(Unit) {
                            signUserList.addAll(context.chaoxingDataStore.data.first().otherUsersList)
                            userSelections.addAll(List(signUserList.size) { false })
                            signStatus.addAll(Array(signUserList.size + 1) { ChaoxingSignStatus() })
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Checkbox(
                                    checked = userSelections[0],
                                    onCheckedChange = { isChecked ->
                                        userSelections[0] = isChecked
                                    }, enabled = isCurrentAlreadySigned == true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("给自己签到")
                                Text(
                                    signStatus[0].getText(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.End)
                                )
                            }
                            signUserList.forEachIndexed { index, userSelection ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = userSelections[1 + index],
                                        onCheckedChange = { isChecked ->
                                            userSelections[1 + index] = isChecked
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = userSelection.name)
                                    Text(
                                        signStatus[1 + index].getText(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentWidth(Alignment.End)
                                    )
                                }
                            }
                        }

                        Button(onClick = {
                            if (isMapRequired) {
                                isMapGetting = true
                            } else {
                                isQRCodeScanning = true
                                isQRCodeScanPause.value = false
                                isQRCodeParsing.value = false
                            }
                        }) {
                            Text("签到")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    ) {
                        AnimatedVisibility(isMapGetting) {
                            GetLocationComponent(confirmButtonText = {
                                Text("设置")
                            }) {
                                isMapGetting = false
                                isQRCodeScanning = true
                                isQRCodeScanPause.value = false
                                isQRCodeParsing.value = false
                                locationData = it
                            }
                            BackHandler(isMapGetting) {
                                isMapGetting = false
                            }
                        }
                        AnimatedVisibility(isQRCodeScanning) {
                            BackHandler(isQRCodeScanning) {
                                isQRCodeScanning = false
                                isQRCodeParsing.value = false
                                isQRCodeScanPause.value = false
                                isQRCodeIllegal = false
                            }
                            QRCodeScanComponent(isQRCodeScanPause, isQRCodeParsing, onClose = {
                                isQRCodeScanning = false
                            }, onScanResult = {
                                isQRCodeScanPause.value = true
                                isQRCodeParsing.value = true
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            return@runCatching signer.parseQRCode(it)
                                        }.onSuccess { enc ->
                                            isQRCodeScanning = false
                                            runCatching {
                                                signer.sign(enc, locationData)
                                            }.onSuccess {
                                                signStatus[0].success()
                                            }.onFailure {
                                                it.printStackTrace()
                                                signStatus[0].failed(it)
                                            }
                                            signUserList.forEachIndexed { index, it ->
                                                runCatching {
                                                    ChaoxingQRCodeSigner(
                                                        ChaoxingHttpClient.loadFromOtherUserSession(
                                                            it
                                                        ), destination
                                                    ).apply {
                                                        if (preSign()) {
                                                            throw ChaoxingSigner.AlreadySignedException()
                                                        } else {
                                                            sign(enc, locationData)
                                                        }
                                                    }
                                                }.onSuccess {
                                                    signStatus[1 + index].success()
                                                }.onFailure {
                                                    it.printStackTrace()
                                                    signStatus[1 + index].failed(it)
                                                }
                                            }
                                        }.onFailure {
                                            it.printStackTrace()
                                            isQRCodeIllegal = true
                                            isQRCodeScanPause.value = true
                                            qrcodeIllegalText =
                                                it.message ?: "二维码解析失败，不是正确码。"
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
                                                    Text("扫描签到二维码")
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
        }

        null -> {
            CenterCircularProgressIndicator()
        }
    }
}