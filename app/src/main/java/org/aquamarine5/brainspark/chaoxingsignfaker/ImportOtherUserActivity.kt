/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

class ImportOtherUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    var errorTips by remember { mutableStateOf<String?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    var isSuccess by remember { mutableStateOf(false) }
                    val data = intent.data
                    val phone = data?.getQueryParameter("phone")
                    val pwd = data?.getQueryParameter("pwd")
                    val name = data?.getQueryParameter("name")
                    Crossfade(errorTips) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (it == null) {
                                if (data == null) {
                                    errorTips = "数据为空，请重新打开链接导入"
                                } else {
                                    if (phone == null || pwd == null || name == null) {
                                        errorTips = "错误的链接格式，请重新打开链接导入"
                                    } else {
                                        if (isLoading) {
                                            CircularProgressIndicator()
                                        }
                                        if (isSuccess) {
                                            Icon(painterResource(R.drawable.ic_check_px80), null)
                                            Text(
                                                "$name 导入成功！",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        LaunchedEffect(Unit) {
                                            isLoading = true
                                            runCatching {
                                                ChaoxingOtherUserHelper.saveOtherUser(
                                                    this@ImportOtherUserActivity,
                                                    ChaoxingOtherUserSharedEntity(
                                                        phone, pwd, name
                                                    )
                                                )
                                            }.onSuccess {
                                                isSuccess = true
                                                isLoading = false
                                                UMengHelper.onAccountOtherUserAddEvent(applicationContext,it)
                                            }.onFailure { failure ->
                                                errorTips =
                                                    failure.message ?: failure.localizedMessage
                                                            ?: "导入失败，请重试"
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(painterResource(R.drawable.ic_user_round_x), null)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "导入失败：$errorTips",
                                        color = Color(0xFFF1441D),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = {
                                if (isSuccess)
                                    startActivity(
                                        Intent(
                                            this@ImportOtherUserActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                finish()
                            }, modifier = Modifier.fillMaxWidth()) {
                                Crossfade(isSuccess) { value ->
                                    if (value) {
                                        Text("返回主页面")
                                    } else {
                                        Text("返回")
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