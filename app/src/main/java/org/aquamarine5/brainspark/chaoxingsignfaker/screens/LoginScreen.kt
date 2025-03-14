package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.serialization.Serializable
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

@Serializable
object LoginDestination

@Composable
fun LoginPage() {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TextFieldValue
    val coroutineContext = rememberCoroutineScope()
    val navController= rememberNavController()
    val context= LocalContext.current
    Scaffold { innerPadding->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text("Login Page")
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
                    ChaoxingHttpClient.create(phoneNumber, password,context)
                }.invokeOnCompletion {
                    if (ChaoxingHttpClient.instance != null) {
                        navController.navigate(CourseListDestination)
                    }
                }
            }) {
                Text("登录")
            }
        }
    }
}