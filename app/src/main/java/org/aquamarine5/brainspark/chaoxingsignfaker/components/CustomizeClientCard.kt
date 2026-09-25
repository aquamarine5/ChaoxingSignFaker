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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.isDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar

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
        "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9006 Build/8aba9e4.0) (schild:2d97f7b9439f21333c946878fc4d6ccb) (device:SM-N9006) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_3_6.7.5_android_phone_10941_314 (@Kalimdor)_68f184fd763546c1a04ab3a09b3deebb",
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
            if (identity.isBlank()) return DEFAULT
            return entries.find { it.identity == identity }
        }
    }
}


private fun isValidUserAgent(userAgent: String): Boolean =
    userAgent.all { it.code in 0x20..0x7E }

fun initializeClientInfo(userAgent: String, packageName: String) {
    if (!isValidUserAgent(userAgent)) {
        chaoxingUserAgent = ChaoxingClientInfo.DEFAULT.userAgent
        chaoxingClientIdentity = ChaoxingClientInfo.DEFAULT.identity
        chaoxingApplicationPackageName = packageName.ifBlank {
            ChaoxingClientInfo.DEFAULT.packageName
        }
        return
    }
    ChaoxingClientInfo.fromIdentity(userAgent).let {
        if (it == null) {
            chaoxingUserAgent = userAgent.ifBlank {
                ChaoxingClientInfo.DEFAULT.userAgent
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeClientCard(onClose: (() -> Unit)? = null) {
    var isShowDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            isShowDialog = true
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffeea08c))
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
            Column {
                Text(
                    "自定义学校单位和客户端",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 18.sp
                )
                Text(
                    "如果你在使用时出现课程列表不匹配或者学校使用的学习通是学校定制版，请点击修改成正确的配置，否则请勿修改此设置。",
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (isShowDialog) {
        val client = ChaoxingHttpClient.getClientInstanceOrClone()
        var selectedFid by remember(client) { mutableStateOf(client?.configuredFid) }
        var isSchoolExpanded by remember(client) { mutableStateOf(false) }
        var isSaving by remember { mutableStateOf(false) }
        var selectedOption by remember {
            mutableStateOf(ChaoxingClientInfo.fromIdentity(chaoxingClientIdentity))
        }
        var customUserAgent by remember {
            mutableStateOf(if (selectedOption == null) chaoxingUserAgent else "")
        }
        var customPackageName by remember {
            mutableStateOf(
                if (selectedOption == null) chaoxingApplicationPackageName
                else ChaoxingClientInfo.DEFAULT.packageName
            )
        }
        SnackbarAlertDialog(onDismissRequest = {
            if (!isSaving) {
                isShowDialog = false
                onClose?.invoke()
            }
        }, title = {
            Text("修改学校单位和客户端")
        }, text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (client != null) {
                    val schools = client.userEntity.fidList
                    ExposedDropdownMenuBox(
                        expanded = isSchoolExpanded,
                        onExpandedChange = { if (!isSaving) isSchoolExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = schools.firstOrNull { it.first == selectedFid }?.second
                                ?: "未选择学校单位",
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isSaving && schools.isNotEmpty(),
                            label = { Text("学校单位") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    enabled = !isSaving && schools.isNotEmpty()
                                ),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(isSchoolExpanded)
                            },
                            supportingText = if (isDevelopedMode) {
                                { Text("fid=$selectedFid", color = Color.Gray, fontSize = 11.sp) }
                            } else null
                        )
                        ExposedDropdownMenu(
                            expanded = isSchoolExpanded,
                            onDismissRequest = { isSchoolExpanded = false }
                        ) {
                            schools.forEach { (fid, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(name)
                                            if (isDevelopedMode) {
                                                Text(
                                                    "fid=$fid",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedFid = fid
                                        isSchoolExpanded = false
                                    },
                                    enabled = !isSaving,
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text("如果你的学校使用了定制版的学习通，可以在这里选择对应的选项来模拟此客户端，或者输入完整的 UserAgent 来模拟其他版本的客户端。")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == ChaoxingClientInfo.DEFAULT,
                        enabled = !isSaving,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedOption = ChaoxingClientInfo.DEFAULT
                        }
                    )
                    Column(
                        modifier = Modifier
                            .clickable(enabled = !isSaving) {
                                selectedOption = ChaoxingClientInfo.DEFAULT
                            }
                            .fillMaxWidth()) {
                        Text("学习通")
                        Text(
                            ChaoxingClientInfo.DEFAULT.packageName,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == ChaoxingClientInfo.XUEZAIXIDIAN,
                        enabled = !isSaving,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedOption = ChaoxingClientInfo.XUEZAIXIDIAN
                        }
                    )
                    Column(
                        modifier = Modifier
                            .clickable(enabled = !isSaving) {
                                selectedOption = ChaoxingClientInfo.XUEZAIXIDIAN
                            }
                            .fillMaxWidth()) {
                        Text("学在西电")
                        Text(
                            ChaoxingClientInfo.XUEZAIXIDIAN.packageName,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedOption == null,
                        enabled = !isSaving,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selectedOption = null
                        }
                    )
                    Text(
                        "自定义 USER_AGENT", modifier = Modifier
                            .clickable(enabled = !isSaving) {
                                selectedOption = null
                            }
                            .fillMaxWidth())
                }
                if (selectedOption == null) {
                    OutlinedTextField(
                        value = customUserAgent,
                        enabled = !isSaving,
                        onValueChange = { customUserAgent = it },
                        label = { Text("输入自定义 UserAgent") },
                        isError = !isValidUserAgent(customUserAgent),
                        supportingText = if (!isValidUserAgent(customUserAgent)) {
                            { Text("UserAgent只能包含可打印ASCII字符") }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = customPackageName,
                        enabled = !isSaving,
                        onValueChange = { customPackageName = it },
                        label = { Text("输入应用包名（可选）") },
                        placeholder = {
                            Text(ChaoxingClientInfo.DEFAULT.packageName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(R.drawable.ic_triangle_alert),
                            contentDescription = "warning",
                            tint = Color(0xffffa500)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "如果你不知道具体的UserAgent是什么，或者不知道什么是UserAgent时，请勿进行自定义操作，否则将无法正常签到。",
                            fontSize = 11.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }, confirmButton = {
            val snackbarHostState = LocalSnackbarHostState.current
            Button(
                enabled = !isSaving &&
                    (selectedOption != null || isValidUserAgent(customUserAgent)),
                onClick = {
                    if (isSaving) return@Button
                    if (selectedOption == null && !isValidUserAgent(customUserAgent)) {
                        snackbarHostState.displaySnackbar(
                            "UserAgent只能包含可打印ASCII字符",
                            coroutineScope
                        )
                        return@Button
                    }
                    isSaving = true
                    isSchoolExpanded = false
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    val fidToSave = selectedFid
                    val userAgentPreference = selectedOption?.identity
                        ?: customUserAgent.ifBlank { ChaoxingClientInfo.DEFAULT.identity }
                    val packageNamePreference = customPackageName
                    coroutineScope.launch {
                        val result = runCatching {
                            if (client != null && fidToSave != null && fidToSave != client.configuredFid) {
                                client.updateConfiguredFid(context, fidToSave)
                            }
                            context.chaoxingDataStore.updateData { dataStore ->
                                dataStore.toBuilder().apply {
                                    preferences = preferences.toBuilder()
                                        .setCustomizedUserAgent(userAgentPreference)
                                        .setCustomizedPackageName(packageNamePreference)
                                        .build()
                                }.build()
                            }
                            initializeClientInfo(userAgentPreference, packageNamePreference)
                        }
                        isSaving = false
                        result.getOrElse { exception ->
                            if (exception is CancellationException || exception is Error) {
                                throw exception
                            }
                            exception.snackbarReport(
                                snackbarHostState,
                                coroutineScope,
                                prefixTips = "保存学校单位和客户端失败",
                                hapticFeedback = hapticFeedback
                            )
                            return@launch
                        }
                        isShowDialog = false
                        onClose?.invoke()
                    }
                }
            ) {
                Text(if (isSaving) "保存中…" else "确定")
            }
        })
    }
}
