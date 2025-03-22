/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

@Composable
fun RequireLoginAlertDialog(
    naviBack: () -> Unit,
    onSharedEntityReceived: (ChaoxingOtherUserSharedEntity) -> Unit
) {
    var isShowDialog by remember { mutableStateOf(true) }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineContext = rememberCoroutineScope()
    if (isShowDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text("重新登录")
            },
            text = {
                Column {
                    Text("如果需要使用登录其他用户、帮助签到的功能，需要升级程序数据库，重新登录学习通账号。")
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("手机号") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(errorText, color = Color.Red)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineContext.launch {
                            runCatching {
                                ChaoxingHttpClient.checkSharedEntity(phoneNumber, password, context)
                            }.onFailure {
                                errorText = it.message ?: "登录失败"
                            }.onSuccess {
                                onSharedEntityReceived(it)
                                isShowDialog = false
                            }
                        }
                    }
                ) {
                    Text("登录")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isShowDialog = false
                        naviBack()
                    }
                ) {
                    Text("退出", color = Color.Red)
                }
            }
        )
    }
}