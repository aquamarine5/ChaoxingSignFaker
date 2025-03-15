package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

@Serializable
object LoginDestination

@Composable
fun LoginPage(
    navController: NavController
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var tipsText by remember { mutableStateOf("") }
    val coroutineContext = rememberCoroutineScope()
    val context = LocalContext.current
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                "Login Page",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text("输入手机号：")
            OutlinedTextField(
                phoneNumber,
                onValueChange = { phoneNumber = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text("输入密码：")
            OutlinedTextField(
                password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Button(onClick = {
                coroutineContext.launch {
                    try {
                        ChaoxingHttpClient.create(phoneNumber, password, context)
                    } catch (e: ChaoxingHttpClient.ChaoxingLoginException) {
                        tipsText = e.message ?: "登录失败"
                    }
                }.invokeOnCompletion {
                    if (ChaoxingHttpClient.instance != null) {
                        navController.navigate(CourseListDestination)
                    }
                }
            }) {
                Text("登录")
            }
            Text(tipsText, color = Color.Red)
        }
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    LoginPage(rememberNavController())
}