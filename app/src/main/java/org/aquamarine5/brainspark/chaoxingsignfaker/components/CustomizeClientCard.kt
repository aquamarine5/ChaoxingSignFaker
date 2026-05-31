/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore

lateinit var chaoxingUserAgent: String
lateinit var chaoxingApplicationPackageName: String
lateinit var chaoxingClientIdentity: String

private const val CUSTOM_CLIENT_IDENTITY = "@@custom"

enum class ChaoxingClientInfo(
    val userAgent: String,
    val packageName: String,
    val identity: String
) {
    DEFAULT(
        "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9006 Build/8aba9e4.0) (schild:ce31140dfcdc2fcd113ccdd86f89a9aa) (device:SM-N9006) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_3_6.5.1_android_phone_10837_265 (@Kalimdor)_68f184fd763546c1a04ab3a09b3deebb",
        "com.chaoxing.mobile",
        "@@default"
    ),
    XUEZAIXIDIAN(
        "Mozilla/5.0 (Linux; Android 16; 23113RKC6C Build/BP2A.250605.031.A3; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.137 Mobile Safari/537.36 (schild:be536573b69ec1ae359e359d11f7f3e3) (device:23113RKC6C) Language/zh_CN com.chaoxing.mobile.xuezaixidian/ChaoXingStudy_1000149_6.3.7_android_phone_6005_249 (@Kalimdor)_f8777230ca1e45b2831ec7e36a9da1ea",
        "com.chaoxing.mobile.xuezaixidian",
        "@@xuezaixidian"
    );

    companion object {
        fun fromIdentity(identity: String): ChaoxingClientInfo? {
            if (identity == CUSTOM_CLIENT_IDENTITY) return null
            return entries.find { it.identity == identity }
        }
    }
}


fun initializeClientInfo(userAgent: String, packageName: String) {
    ChaoxingClientInfo.fromIdentity(userAgent).let {
        if (it == null) {
            chaoxingUserAgent = userAgent
            chaoxingClientIdentity = CUSTOM_CLIENT_IDENTITY
            chaoxingApplicationPackageName = packageName.ifBlank {
                ChaoxingClientInfo.DEFAULT.packageName
            }
        } else {
            chaoxingUserAgent = it.userAgent
            chaoxingClientIdentity = it.identity
            chaoxingApplicationPackageName = it.packageName
        }
    }
}

@Composable
fun CustomizeClientCard() {
    var isShowDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedOption by remember {
        mutableStateOf(
            chaoxingClientIdentity.let { ChaoxingClientInfo.fromIdentity(it) }
        )
    }
    var customUserAgent by remember { mutableStateOf(if (selectedOption == null) chaoxingUserAgent else "") }
    var customPackageName by remember { mutableStateOf(if (selectedOption == null) chaoxingApplicationPackageName else ChaoxingClientInfo.DEFAULT.packageName) }
    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            isShowDialog = true
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffea7293))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_settings),
                contentDescription = "sponsor",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("自定义客户端")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (isShowDialog) {
        AlertDialog(onDismissRequest = {
            isShowDialog = false
        }, title = {
            Text("定制专属客户端")
        }, text = {
            Column {
                Text("如果你的学校使用了定制版的学习通，可以在这里选择对应的选项来模拟此客户端，或者输入完整的 UserAgent 来模拟其他版本的客户端。")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == ChaoxingClientInfo.DEFAULT,
                        onClick = { selectedOption = ChaoxingClientInfo.DEFAULT }
                    )
                    Text(
                        buildAnnotatedString {
                            append("学习通\n")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(ChaoxingClientInfo.DEFAULT.packageName)
                            }
                        }, modifier = Modifier
                            .clickable {
                                selectedOption = ChaoxingClientInfo.DEFAULT
                            }
                            .fillMaxWidth())
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == ChaoxingClientInfo.XUEZAIXIDIAN,
                        onClick = { selectedOption = ChaoxingClientInfo.XUEZAIXIDIAN }
                    )
                    Text(
                        buildAnnotatedString {
                            append("学在西电\n")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(ChaoxingClientInfo.XUEZAIXIDIAN.packageName)
                            }
                        }, modifier = Modifier
                            .clickable {
                                selectedOption = ChaoxingClientInfo.XUEZAIXIDIAN
                            }
                            .fillMaxWidth())
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == null,
                        onClick = { selectedOption = null }
                    )
                    Text(
                        "自定义 USER_AGENT", modifier = Modifier
                            .clickable {
                                selectedOption = null
                            }
                            .fillMaxWidth())
                }
                if (selectedOption == null) {
                    OutlinedTextField(
                        value = customUserAgent,
                        onValueChange = { customUserAgent = it },
                        label = { Text("输入自定义 UserAgent") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        label = { Text("输入应用包名（可选）") },
                        placeholder = {
                            Text(ChaoxingClientInfo.DEFAULT.packageName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_triangle_alert),
                            contentDescription = "warning",
                            tint = Color(0xffffa500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("如果你不知道具体的UserAgent是什么，或者不知道什么是UserAgent时，请勿进行自定义操作，否则将无法正常签到。")
                    }
                }
            }
        }, confirmButton = {
            Button(onClick = {
                chaoxingUserAgent = selectedOption?.userAgent ?: customUserAgent
                chaoxingApplicationPackageName =
                    selectedOption?.packageName ?: customPackageName.ifBlank {
                        ChaoxingClientInfo.DEFAULT.packageName
                    }

                coroutineScope.launch {
                    context.chaoxingDataStore.updateData { dataStore ->
                        dataStore.toBuilder().apply {
                            preferences = preferences.toBuilder()
                                .setCustomizedUserAgent(selectedOption?.identity ?: customUserAgent)
                                .build()
                        }.build()
                    }
                }
                isShowDialog = false
            }) {
                Text("确定")
            }
        })
    }
}
