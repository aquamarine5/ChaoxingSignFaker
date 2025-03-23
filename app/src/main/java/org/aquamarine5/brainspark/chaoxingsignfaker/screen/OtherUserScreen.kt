/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

@Serializable
data class OtherUserDestination(
    val scanResult: String? = null
)

@Composable
fun OtherUserScreen() {
    Scaffold { innerPadding ->
        var isQRCodeScanPause by remember { mutableStateOf(false) }
        var isQRCodeScanning by remember { mutableStateOf(false) }
        var isQRCodeIllegal by remember { mutableStateOf(false) }
        val coroutineScope= rememberCoroutineScope()
        val context=LocalContext.current
        Box(
            modifier = Modifier
                .zIndex(0f)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var qrCode by remember { mutableStateOf<Bitmap?>(null) }
                val qrcodeSize = ChaoxingOtherUserHelper.getQRCodeDpSize(context)
                LaunchedEffect(Unit) {
                    qrCode = ChaoxingOtherUserHelper.generateQRCode(context)
                }
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .size(qrcodeSize + 18.dp, qrcodeSize + 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCode != null) {
                        Image(bitmap = qrCode!!.asImageBitmap(), contentDescription = "QR Code")
                    }
                }
                Text("使用其他设备打开应用扫描二维码\n以将你的账号添加到其他设备中")
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8D86A)
                    ), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_triangle_alert),
                            contentDescription = "Alert",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "随意分享此二维码给不熟悉的人可能会增加你的学习通账号风险，他人可以通过二维码控制你的账号，但并不会知晓你的明文密码。",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
                Button(onClick = {
                    isQRCodeScanning = true
                }) {
                    Icon(
                        painterResource(R.drawable.ic_user_round_plus),
                        contentDescription = "Add User"
                    )
                }
            }
        }
        if (isQRCodeScanning) {
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxSize()
            ) {
                QRCodeScanComponent(isQRCodeScanPause,onClose = {
                    isQRCodeScanning = false
                }, onScanResult = {
                    runCatching {
                        return@runCatching ChaoxingOtherUserSharedEntity.parseFromQRCode(it)
                    }.onSuccess {
                        isQRCodeScanning = false
                        coroutineScope.launch {
                            ChaoxingOtherUserHelper.saveOtherUser(context,it)
                        }
                    }.onFailure {
                        isQRCodeIllegal = true
                        isQRCodeScanPause=true
                        coroutineScope.launch {
                            delay(1000)
                            isQRCodeScanPause=false
                            delay(2000)
                            isQRCodeIllegal = false
                        }
                    }
                }) {
                    Crossfade(isQRCodeIllegal) {
                        when(it){
                            true->{
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(
                                            Color(0x72F1441D),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            BorderStroke(2.dp, Color(0xFFF1441D)),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_octagon_alert),
                                        contentDescription = "Illegal QR Code"
                                    )
                                    Text("不是有效的二维码")
                                }
                            }
                            false->{
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(
                                            Color(0x88888888),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            BorderStroke(2.dp, Color(0xFF444444)),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_scan_qr_code),
                                        contentDescription = "Scan QR Code"
                                    )
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