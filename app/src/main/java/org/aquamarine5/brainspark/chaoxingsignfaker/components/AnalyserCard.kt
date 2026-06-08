/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingAnalyser
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingAnalyserRankAnalysis
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingAnalyserRankRecord
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AnalyserCard() {
    LocalContext.current.let { context ->
        val snackbarHostState = LocalSnackbarHostState.current
        val coroutineScope = rememberCoroutineScope()
        val hapticFeedback = LocalHapticFeedback.current
        val analyser = rememberSaveable(saver = ChaoxingAnalyser.MutableStateAnalyser.Saver) {
            ChaoxingAnalyser.createStateAnalyser()
        }
        var lastUploadTimestamp by remember { mutableLongStateOf(0L) }
        var customRankDisplayName by remember { mutableStateOf("") }
        var isDisableAnalyserRank by remember { mutableStateOf(false) }
        var displayRankCount by remember { mutableIntStateOf(50) }
        var isHideAnalyserSchoolName by remember { mutableStateOf(false) }
        LaunchedEffect(analyser.isLoaded) {
            if (analyser.isLoaded.value.not())
                ChaoxingAnalyser.setupStateAnalyser(context)
            context.chaoxingDataStore.data.first().let {
                customRankDisplayName = it.analysisRankName.ifEmpty {
                    "****${
                        ChaoxingHttpClient.instance!!.userEntity.phoneNumber.takeLast(
                            2
                        )
                    } 用户"
                }
                lastUploadTimestamp = it.lastUploadAnalysisTimestamp
                isDisableAnalyserRank = it.disableAnalysisRank
                isHideAnalyserSchoolName = it.hideAnalysisRankSchoolName
                displayRankCount = it.preferences.displayRankCount.let {
                    if (it == 0) 50 else it
                }
            }
        }
        val fontGilroy = remember {
            FontFamily(Font(R.font.gilroy))
        }
        var clickToDisplayRankDetail by remember { mutableStateOf<ChaoxingAnalyserRankRecord?>(null) }
        var isAnalyserRankDialog by remember { mutableStateOf(false) }
        var isAnalyserRankHelpDialog by remember { mutableStateOf(false) }
        var isChangeDisplayedNameDialog by remember { mutableStateOf(false) }
        var rankData by remember { mutableStateOf<Result<List<ChaoxingAnalyserRankRecord>>?>(null) }
        val focusRequester = remember { FocusRequester() }
        if (isChangeDisplayedNameDialog) {
            AlertDialog(onDismissRequest = {
                isChangeDisplayedNameDialog = false
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, title = {
                Text("修改排行榜显示名称")
            }, text = {
                Column {
                    Text("排行榜显示名称是指在排行榜中展示的用户名，修改排行榜显示名称不会影响学习通账号昵称的修改，填写名称时请注意遵守相关法律法规。\n请注意，任何操作都会在第二天打开应用时提交至服务器进行修改。")
                    TextField(
                        value = customRankDisplayName,
                        onValueChange = { customRankDisplayName = it },
                        label = {
                            Text(
                                "展示名称"
                            )
                        }, placeholder = {
                            Text(
                                "****${
                                    ChaoxingHttpClient.instance!!.userEntity.phoneNumber.takeLast(
                                        2
                                    )
                                } 用户"
                            )
                        },
                        trailingIcon = {
                            if (customRankDisplayName.isNotEmpty()) {
                                IconButton(onClick = {
                                    customRankDisplayName = ""
                                    focusRequester.requestFocus()
                                }) {
                                    Icon(
                                        painterResource(R.drawable.ic_delete),
                                        contentDescription = "清空"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isChangeDisplayedNameDialog = false
                    coroutineScope.launch {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder()
                                .setAnalysisRankName(customRankDisplayName)
                                .build()
                        }
                    }
                }) {
                    Text("保存")
                }
            }, dismissButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isChangeDisplayedNameDialog = false
                }) {
                    Text("取消")
                }
            })
        }
        if (isAnalyserRankHelpDialog) {
            AlertDialog(onDismissRequest = {
                isAnalyserRankHelpDialog = false
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_info),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, title = {
                Text("排行榜说明")
            }, text = {
                Column {
                    Text("随地大小签的签到排行榜每日根据用户的签到数据上传至数据库进行更新，并非实时更新，上传的数据不会包含学习通账号的隐私信息，上传的数据仅用作排行榜展示，不会用于其他用途。随地大小签的排行榜功能仍在测试阶段。\n如果不想展示学校信息，可以勾选下方的【隐藏上传学校信息】。\n请注意，任何操作都会在修改后的第二天打开应用时提交至服务器进行修改。")
                    HorizontalDivider(modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 0.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isHideAnalyserSchoolName,
                            onCheckedChange = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                isHideAnalyserSchoolName = it
                                coroutineScope.launch {
                                    context.chaoxingDataStore.updateData {
                                        it.toBuilder()
                                            .setHideAnalysisRankSchoolName(isHideAnalyserSchoolName)
                                            .build()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("隐藏上传学校信息")
                    }
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isAnalyserRankHelpDialog = false
                }) { Text("关闭") }
            })
        }
        clickToDisplayRankDetail?.let { detail ->
            AlertDialog(onDismissRequest = {
                clickToDisplayRankDetail = null
            }, title = {
                Text(detail.name)
            }, text = {
                Column {
                    Text(
                        "学校: ${
                            detail.schoolName.let { school ->
                                if (school.endsWith("HIDE")) "已隐藏学校信息" else school
                            }
                        }"
                    )
                    Text("拍照签到次数: ${detail.photoSign}")
                    Text("手势签到次数: ${detail.gestureSign}")
                    Text("点击签到次数: ${detail.clickSign}")
                    Text("位置签到次数: ${detail.locationSign}")
                    Text("二维码签到次数: ${detail.qrcodeSign}")
                    Text("签到码签到次数: ${detail.passwordSign}")
                    Text("代签次数: ${detail.otherSign}")
                    Text("总签到次数: ${detail.totalSignCount}")
                    Text("最新更新时间：${detail.latestDate}")
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    clickToDisplayRankDetail = null
                }) { Text("关闭") }
            })
        }
        if (isAnalyserRankDialog) {
            var rankAnalysisData by remember { mutableStateOf<ChaoxingAnalyserRankAnalysis?>(null) }
            LaunchedEffect(Unit) {
                if (rankData == null || rankData!!.isFailure)
                    rankData = ChaoxingAnalyser.getAnalyserTopRank(displayRankCount).onFailure {
                        it.snackbarReport(
                            snackbarHostState,
                            coroutineScope,
                            "获取排行榜失败",
                            hapticFeedback
                        )
                    }
            }
            LaunchedEffect(Unit) {
                if (rankAnalysisData == null)
                    rankAnalysisData = ChaoxingAnalyser.getTotalRankAnalysis().getOrNull()
            }
            AlertDialog(onDismissRequest = {
                isAnalyserRankDialog = false
            }, title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("签到排行榜")
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isAnalyserRankHelpDialog = true
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_chart_line),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, dismissButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isChangeDisplayedNameDialog = true
                }) {
                    Text("修改展示名称")
                }
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isAnalyserRankDialog = false
                }) {
                    Text("关闭")
                }
            }, text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when {
                        rankData == null -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(0.dp, 6.dp)
                            )
                        }

                        rankData!!.isSuccess -> {
                            val list = remember { rankData!!.getOrThrow() }
                            val userIndex =
                                remember { list.indexOfFirst { it.uuid == ChaoxingAnalyser.rankUUID } }
                            var userRank by remember { mutableStateOf(if (userIndex == -1) null else userIndex + 1) }
                            LaunchedEffect(Unit) {
                                if (userRank == null)
                                    userRank =
                                        ChaoxingAnalyser.getUserTopRank(ChaoxingAnalyser.rankUUID)
                                            .getOrNull()
                            }
                            val userRecord = remember { list.getOrNull(userIndex) }

                            val primaryColor = MaterialTheme.colorScheme.primary
                            val highlightSpanStyle = remember {
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    fontFamily = fontGilroy,
                                    fontSize = 17.sp
                                )
                            }
                            val labelSmallSpanStyle = remember {
                                SpanStyle(fontSize = 11.sp)
                            }

                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                itemsIndexed(list) { index, it ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                clickToDisplayRankDetail = it
                                            }
                                            .padding(0.dp, 4.dp)
                                    ) {
                                        Text(
                                            "${index + 1}.",
                                            modifier = Modifier
                                                .width(36.dp)
                                                .padding(end = 6.dp),
                                            textAlign = TextAlign.Center,
                                            fontFamily = fontGilroy,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            autoSize = TextAutoSize.StepBased(
                                                maxFontSize = 18.sp,
                                                minFontSize = 8.sp,
                                                stepSize = 1.sp
                                            )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            if (it.uuid == ChaoxingAnalyser.rankUUID) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = it.name,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        lineHeight = 15.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Text(
                                                        text = " (你)",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = it.name,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                it.schoolName.let { school ->
                                                    if (school.endsWith("HIDE")) "已隐藏学校信息" else school.ifBlank { "未知学校" }
                                                },
                                                fontSize = 10.sp,
                                                lineHeight = 11.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(buildAnnotatedString {
                                                withStyle(labelSmallSpanStyle) {
                                                    append("总签到次数: ")
                                                }
                                                withStyle(highlightSpanStyle) {
                                                    append(it.totalSignCount.toString())
                                                }
                                            })
                                            Text(
                                                "代签次数: ${it.otherSign}",
                                                fontSize = 10.sp,
                                                lineHeight = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, 4.dp)
                                ) {
                                    Text(
                                        "${userRank ?: "-"}.",
                                        modifier = Modifier
                                            .width(36.dp)
                                            .padding(end = 6.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontGilroy,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.primary,
                                        autoSize = TextAutoSize.StepBased(
                                            maxFontSize = 18.sp,
                                            minFontSize = 8.sp,
                                            stepSize = 1.sp
                                        )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        val baseName =
                                            remember(
                                                customRankDisplayName
                                            ) { if (userIndex != -1) userRecord!!.name else customRankDisplayName }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = baseName,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text(
                                                text = " (你)",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            if (userIndex != -1) {
                                                userRecord!!.schoolName.let { school ->
                                                    if (school.endsWith("HIDE")) "已隐藏学校信息" else school.ifBlank { "未知学校" }
                                                }
                                            } else ChaoxingHttpClient.instance!!.userEntity.schoolName,
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(buildAnnotatedString {
                                            withStyle(labelSmallSpanStyle) {
                                                append("总签到次数: ")
                                            }
                                            withStyle(highlightSpanStyle) {
                                                if (userIndex != -1) append(userRecord!!.totalSignCount.toString())
                                                else {
                                                    val total =
                                                        analyser.photoSignCount.value + analyser.gestureSignCount.value + analyser.clickSignCount.value + analyser.locationSignCount.value + analyser.qrcodeSignCount.value + analyser.passwordSignCount.value
                                                    append(total.toString())
                                                }
                                            }
                                        })
                                        val otherSigns =
                                            remember { if (userIndex != -1) userRecord!!.otherSign.toString() else analyser.otherUserSignCount.value.toString() }
                                        Text(
                                            "代签次数: $otherSigns",
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "上次上传排行榜数据时间：\n${
                                            if (lastUploadTimestamp == 0L) "从未成功过或因旧版本暂未记录上传时间" else Instant.ofEpochMilli(
                                                lastUploadTimestamp
                                            ).atZone(
                                                ZoneId.systemDefault()
                                            ).format(
                                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                            )
                                        }",
                                        fontSize = 10.sp,
                                        lineHeight = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(buildAnnotatedString {
                                            withStyle(labelSmallSpanStyle) {
                                                append("共计签到次数: ")
                                            }
                                            withStyle(highlightSpanStyle) {
                                                append(
                                                    rankAnalysisData?.totalRecordSignCount?.toString()
                                                        ?: "-"
                                                )
                                            }
                                        })
                                        Text(
                                            buildAnnotatedString {
                                                withStyle(SpanStyle(color = Color.Gray)) {
                                                    append("统计人数: ")
                                                }
                                                append(
                                                    rankAnalysisData?.userCount?.toString()
                                                        ?: "-"
                                                )
                                            },
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        rankData!!.isFailure -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(onClick = {
                                    rankData = null
                                    coroutineScope.launch {
                                        rankData =
                                            ChaoxingAnalyser.getAnalyserTopRank(displayRankCount)
                                                .onFailure {
                                                    it.snackbarReport(
                                                        snackbarHostState,
                                                        coroutineScope,
                                                        "获取排行榜失败",
                                                        hapticFeedback
                                                    )
                                                }
                                    }
                                }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                }
            })
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF22A2C3))
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp, 8.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalContentColor provides if (isSystemInDarkTheme()) Color.Black else Color.White) {
                    Icon(
                        painterResource(R.drawable.ic_chart_column),
                        contentDescription = "analyser"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedVisibility(
                        analyser.isLoaded.value,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Text("使用次数统计", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            analyser.apply {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf(
                                        photoSignCount to painterResource(R.drawable.ic_camera),
                                        locationSignCount to painterResource(R.drawable.ic_map_pin),
                                        qrcodeSignCount to painterResource(R.drawable.ic_scan_qr_code),
                                        clickSignCount to painterResource(R.drawable.ic_square_mouse_pointer),
                                        gestureSignCount to painterResource(R.drawable.ic_pattern_locking),
                                        passwordSignCount to painterResource(R.drawable.ic_binary),
                                        otherUserSignCount to painterResource(R.drawable.ic_users_round)
                                    ).forEach {
                                        Row(
                                            modifier = Modifier.padding(0.dp, 0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(it.second, null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                it.first.value.toString(),
                                                fontFamily = fontGilroy,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    isAnalyserRankDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painterResource(R.drawable.ic_chart_line),
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("签到次数排行榜")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}