package org.aquamarine5.brainspark.chaoxingsignfaker.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import kotlin.coroutines.coroutineContext

@Composable
fun LoginPage() {
    var chaoxingHttpClient by remember { mutableStateOf<ChaoxingHttpClient?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    TextFieldValue
    val coroutineContext = rememberCoroutineScope()
    Column {
        Text("Login Page")
        Text("输入手机号：")
        OutlinedTextField(
            phoneNumber,
            onValueChange = { phoneNumber = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
        )
        Text("输入密码：")
        OutlinedTextField(
            password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
    }

    chaoxingHttpClient?.let { client ->
        CompositionLocalProvider(LocalChaoxingHttpClient provides client) {
            // Your composable content
        }
    }
}

val LocalChaoxingHttpClient: ProvidableCompositionLocal<ChaoxingHttpClient?> =
    compositionLocalOf { null }