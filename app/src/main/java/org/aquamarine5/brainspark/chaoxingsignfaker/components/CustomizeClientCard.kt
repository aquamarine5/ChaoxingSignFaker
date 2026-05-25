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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

const val CHAOXING_XUEZAIXIDIAN_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 16; 23113RKC6C Build/BP2A.250605.031.A3; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.137 Mobile Safari/537.36 (schild:be536573b69ec1ae359e359d11f7f3e3) (device:23113RKC6C) Language/zh_CN com.chaoxing.mobile.xuezaixidian/ChaoXingStudy_1000149_6.3.7_android_phone_6005_249 (@Kalimdor)_f8777230ca1e45b2831ec7e36a9da1ea"
const val CHAOXING_DEFAULT_USER_AGENT =
    "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9006 Build/8aba9e4.0) (schild:ce31140dfcdc2fcd113ccdd86f89a9aa) (device:SM-N9006) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_3_6.5.1_android_phone_10837_265 (@Kalimdor)_68f184fd763546c1a04ab3a09b3deebb"

const val CHAOXING_XUEZAIXIDIAN_USER_AGENT_IDENTITY = "@@xuezaixidian"
const val CHAOXING_DEFAULT_USER_AGENT_IDENTITY = "@@default"
var CHAOXING_USER_AGENT = CHAOXING_DEFAULT_USER_AGENT

private enum class AvailableUserAgent {
    DEFAULT,
    XUEZAIXIDIAN,
    CUSTOM
}

fun getUserAgent(content: String): String {
    return when (content) {
        CHAOXING_DEFAULT_USER_AGENT_IDENTITY -> CHAOXING_DEFAULT_USER_AGENT
        CHAOXING_XUEZAIXIDIAN_USER_AGENT_IDENTITY -> CHAOXING_XUEZAIXIDIAN_USER_AGENT
        else -> content
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
            when (CHAOXING_USER_AGENT) {
                CHAOXING_DEFAULT_USER_AGENT -> AvailableUserAgent.DEFAULT
                CHAOXING_XUEZAIXIDIAN_USER_AGENT -> AvailableUserAgent.XUEZAIXIDIAN
                else -> AvailableUserAgent.CUSTOM
            }
        )
    }
    var customUserAgent by remember { mutableStateOf(if (selectedOption == AvailableUserAgent.CUSTOM) CHAOXING_USER_AGENT else "") }

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
            Icon(painterResource(R.drawable.ic_settings), contentDescription = "sponsor")
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
                Text("如果你的学校使用了定制版的学习通，可以在这里选择对应的 UserAgent 来模拟定制版客户端，或者输入完整的 UserAgent 来模拟其他版本的客户端。")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == AvailableUserAgent.DEFAULT,
                        onClick = { selectedOption = AvailableUserAgent.DEFAULT }
                    )
                    Text(buildAnnotatedString {
                        append("学习通\n")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("com.chaoxing.mobile")
                        }
                    }, modifier = Modifier.clickable {
                        selectedOption = AvailableUserAgent.DEFAULT
                    })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == AvailableUserAgent.XUEZAIXIDIAN,
                        onClick = { selectedOption = AvailableUserAgent.XUEZAIXIDIAN }
                    )
                    Text(buildAnnotatedString {
                        append("学在西电\n")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("com.chaoxing.mobile.xuezaixidian")
                        }
                    }, modifier = Modifier.clickable {
                        selectedOption = AvailableUserAgent.XUEZAIXIDIAN
                    })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == AvailableUserAgent.CUSTOM,
                        onClick = { selectedOption = AvailableUserAgent.CUSTOM }
                    )
                    Text("自定义 USER_AGENT", modifier = Modifier.clickable {
                        selectedOption = AvailableUserAgent.CUSTOM
                    })
                }
                if (selectedOption == AvailableUserAgent.CUSTOM) {
                    OutlinedTextField(
                        value = customUserAgent,
                        onValueChange = { customUserAgent = it },
                        label = { Text("输入自定义 User Agent") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }, confirmButton = {
            Button(onClick = {
                val uaToSave = when (selectedOption) {
                    AvailableUserAgent.DEFAULT -> CHAOXING_DEFAULT_USER_AGENT_IDENTITY
                    AvailableUserAgent.XUEZAIXIDIAN -> CHAOXING_XUEZAIXIDIAN_USER_AGENT_IDENTITY
                    else -> customUserAgent
                }
                CHAOXING_USER_AGENT = when (selectedOption) {
                    AvailableUserAgent.DEFAULT -> CHAOXING_DEFAULT_USER_AGENT
                    AvailableUserAgent.XUEZAIXIDIAN -> CHAOXING_XUEZAIXIDIAN_USER_AGENT
                    else -> customUserAgent
                }
                coroutineScope.launch {
                    context.chaoxingDataStore.updateData { dataStore ->
                        dataStore.toBuilder().apply {
                            preferences = preferences.toBuilder()
                                .setCustomizedUserAgent(uaToSave)
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
