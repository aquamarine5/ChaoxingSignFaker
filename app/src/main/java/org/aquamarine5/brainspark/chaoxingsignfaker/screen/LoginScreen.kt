/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

@Serializable
object LoginDestination

@Composable
fun LoginPage(
    navToCourseListDestination: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tipsText by remember { mutableStateOf("") }
    val coroutineContext = rememberCoroutineScope()
    val context = LocalContext.current
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily(
                                Font(R.font.gilroy)
                            ),
                            fontSize = 16.sp
                        )
                    ) {
                        append("ChaoxingSignFaker")
                    }
                    append(" 需要你的学习通账号信息\n请登录你的学习通账号")
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("输入手机号：")
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("输入密码：")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineContext.launch {
                        try {
                            ChaoxingHttpClient.create(phoneNumber, password, context)
                            UMengHelper.onLoginEvent(context, phoneNumber)
                        } catch (e: ChaoxingHttpClient.ChaoxingLoginException) {
                            tipsText = e.message ?: "登录失败"
                        }
                    }.invokeOnCompletion {
                        if (ChaoxingHttpClient.instance != null) {
                            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                            navToCourseListDestination()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (tipsText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .border(
                            BorderStroke(1.dp, Color.LightGray),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_user_round_x),
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tipsText, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    LoginPage {}
}