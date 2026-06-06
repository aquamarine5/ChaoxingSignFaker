/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.OtherUserTagType
import org.aquamarine5.brainspark.chaoxingsignfaker.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.TAG_COLOR_UNSPECIFIED

@Composable
fun OtherUserSelectorComponent(
    navToOtherUser: () -> Unit,
    signStatus: MutableList<ChaoxingSignStatus>,
    isCurrentAlreadySigned: Boolean,
    isSigning: MutableState<Boolean>,
    userSelections: SnapshotStateList<Boolean>,
    onIgnoreExceptionSignAction: suspend (index: Int, session: ChaoxingOtherUserSession) -> Unit,
    userContent: @Composable ((index: Int) -> Unit)? = null,
    prefixTipsContent: @Composable (() -> Unit),
    suffixContent: @Composable (() -> Unit)? = null,
    onSignAction: (isSelf: Boolean, otherUserSessionList: List<ChaoxingOtherUserSession?>, indexList: List<Int>) -> Unit
) {
    LocalContext.current.let { context ->
        val signUserList = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
        val hapticFeedback = LocalHapticFeedback.current
        val snackbarHost = LocalSnackbarHostState.current
        val coroutineScope = rememberCoroutineScope()
        var tagEntities by remember { mutableStateOf<List<OtherUserTagType>?>(null) }
        var tagContainedUserIndexList by remember { mutableStateOf<List<List<Int>>?>(null) }
        val tagClickState = remember { mutableListOf<MutableState<Boolean>>() }
        var selfPhoneNumber by remember { mutableStateOf<String?>(null) }
        var success by signStatus[0].isSuccess
        var ignoreExceptionUserIndex by remember {
            mutableStateOf<Pair<Int, ChaoxingOtherUserSession>?>(
                null
            )
        }

        if (ignoreExceptionUserIndex != null) {
            AlertDialog(onDismissRequest = {
                ignoreExceptionUserIndex = null
            }, icon = {
                Icon(
                    painterResource(R.drawable.ic_refresh_rounded),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, text = {
                Text("随地大小签会自动检测并拒绝为用户不在班级的情况进行签到，因为强制签到会导致老师的已签名单中出现未选此课不在班的学生。\n如果你认为随地大小签的判断存在问题，请点击【强制重试签到】按钮。")
            }, confirmButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    ignoreExceptionUserIndex = null
                }) {
                    Text("关闭")
                }
            }, dismissButton = {
                var isIgnoreExceptionSigning by remember { mutableStateOf(false) }
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch {
                        isIgnoreExceptionSigning = true
                        onIgnoreExceptionSignAction(
                            ignoreExceptionUserIndex!!.first,
                            ignoreExceptionUserIndex!!.second
                        )
                        ignoreExceptionUserIndex = null
                        isIgnoreExceptionSigning = false
                    }
                }, enabled = isIgnoreExceptionSigning.not()) {
                    Text("强制重试签到")
                }
            })
        }

        fun updateTagClickState() {
            tagContainedUserIndexList?.forEachIndexed { tagIndex, userIndexList ->
                if (userIndexList.isNotEmpty()) {
                    val allChecked = userIndexList.all { userIndex ->
                        userSelections[userIndex + 1]
                    }
                    tagClickState[tagIndex].value = allChecked
                }
            }
        }

        LaunchedEffect(Unit) {
            context.chaoxingDataStore.data.first().let {
                selfPhoneNumber = it.loginSession.phoneNumber
            }
        }

        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val gapPx = with(density) { 80.dp.toPx() }
        val showFab by remember {
            derivedStateOf {
                scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - gapPx
            }
        }

        fun performSign() {
            if (!userSelections.any { it }) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                snackbarHost.displaySnackbar("请选择要签到的用户", coroutineScope)
                return
            }
            if (signStatus.all { it.isSuccess.value == true }) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                snackbarHost.displaySnackbar("所有用户均已签到", coroutineScope)
                return
            }
            val indexList = mutableListOf<Int>()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            val isSelf = userSelections[0] && signStatus[0].isSuccess.value != true
            if (isSelf)
                indexList.add(0)
            val otherUserSessionList =
                signUserList.mapIndexed { index, chaoxingOtherUserSession ->
                    if (userSelections[index + 1] && signStatus[1 + index].isSuccess.value != true) {
                        indexList.add(index + 1)
                        chaoxingOtherUserSession
                    } else {
                        null
                    }
                }
            onSignAction(
                isSelf, otherUserSessionList, indexList
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp, 0.dp)
                    .verticalScroll(scrollState)
            ) {
                prefixTipsContent()

                Card(
                    onClick = {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.ContextClick
                        )
                        navToOtherUser()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10AEC2)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp, 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.5.dp))
                        Icon(
                            painterResource(R.drawable.ic_lightbulb),
                            contentDescription = "Help",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.5.dp))
                        Text(
                            "如果你想给其他用户签到但还没有添加其他用户，可以点击此跳转至添加用户向导。",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val data = context.chaoxingDataStore.data.first().let { datastore ->
                            tagEntities = datastore.tagsLibraryList
                            tagClickState.addAll(List(datastore.tagsLibraryList.size) {
                                mutableStateOf(
                                    false
                                )
                            })
                            tagContainedUserIndexList = datastore.tagsLibraryList.map { tagEntity ->
                                buildList {
                                    datastore.otherUsersList.mapIndexed { index, otherUserSession ->
                                        if (otherUserSession.tagsList.any { it == tagEntity.id })
                                            add(index)
                                    }
                                }
                            }
                            datastore.otherUsersList.filter {
                                it.phoneNumber != datastore.loginSession.phoneNumber
                            }
                        }
                        signStatus.addAll(Array(data.size) {
                            ChaoxingSignStatus(hapticFeedback)
                        })
                        userSelections.addAll(List(data.size) { false })
                        signUserList.addAll(data)

                    }
                    success = isCurrentAlreadySigned
                    userSelections[0] = isCurrentAlreadySigned != true
                }

                suffixContent?.invoke()
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "选择要进行签到的用户：",
                    modifier = Modifier.padding(start = 3.dp),
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 4.dp, 8.dp, 16.dp)
                ) {
                    Row {
                        if (tagEntities != null && tagContainedUserIndexList != null)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy((-8).dp)
                            ) {
                                FilterChip(
                                    selected = userSelections.subList(1, userSelections.size)
                                        .all { it } && (isCurrentAlreadySigned || userSelections[0]),
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.ContextClick
                                        )
                                        val allSelected =
                                            userSelections.subList(1, userSelections.size)
                                                .all { it } && (isCurrentAlreadySigned || userSelections[0])
                                        val target = !allSelected
                                        if (!isCurrentAlreadySigned) {
                                            userSelections[0] = target
                                        }
                                        for (i in 1 until userSelections.size) {
                                            userSelections[i] = target
                                        }
                                        updateTagClickState()
                                    },
                                    label = {
                                        Text("全选")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painterResource(R.drawable.ic_list_checks),
                                            null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    border = BorderStroke(1.5.dp, Color.Gray)
                                )
                                if (tagEntities!!.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AssistChip(onClick = {
                                            navToOtherUser()
                                        }, label = {
                                            Text("点击跳转添加标签...")
                                        }, leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_tag_plus_outline),
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.Gray
                                            )
                                        }, border = BorderStroke(1.5.dp, Color.Gray))
                                    }
                                } else {
                                    tagEntities!!.forEachIndexed { index, type ->
                                        FilterChip(
                                            selected = tagClickState[index].value,
                                            onClick = {
                                                tagClickState[index].value =
                                                    !tagClickState[index].value
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.ContextClick
                                                )
                                                tagContainedUserIndexList!![index].forEach { userIndex ->
                                                    if (userIndex + 1 < userSelections.size)
                                                        userSelections[userIndex + 1] =
                                                            tagClickState[index].value
                                                }
                                            },
                                            label = {
                                                Text(type.name)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painterResource(R.drawable.ic_tag),
                                                    null,
                                                    tint = if (type.color == TAG_COLOR_UNSPECIFIED) {
                                                        if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                                    } else {
                                                        Color(type.color)
                                                    }, modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            border = BorderStroke(1.5.dp, Color.Gray)
                                        )
                                    }
                                }
                            }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = userSelections[0] && signStatus[0].isSuccess.value != true,
                            onCheckedChange = { isChecked ->
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                userSelections[0] = isChecked
                            },
                            enabled = (success == true).not()
                        )
                        Row(modifier = Modifier.clickable((success == true).not()) {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.ContextClick
                            )
                            userSelections[0] = userSelections[0].not()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "给自己签到",
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (success != true) TextDecoration.None else TextDecoration.LineThrough
                                )
                                Text(
                                    "${ChaoxingHttpClient.instance?.userEntity?.name} ($selfPhoneNumber)",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                userContent?.invoke(0)
                                signStatus[0].ResultCard()
                            }
                        }
                    }
                    signUserList.forEachIndexed { index, session ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            (1 + index).let { i ->
                                val successForOtherUser by signStatus[i].isSuccess
                                Checkbox(
                                    checked = userSelections[i] && signStatus[i].isSuccess.value != true,
                                    onCheckedChange = { isChecked ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        userSelections[i] = isChecked
                                        updateTagClickState()
                                    },
                                    enabled = (successForOtherUser == true).not()
                                )
                                Row(modifier = Modifier.clickable((successForOtherUser == true).not()) {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    userSelections[i] = userSelections[i].not()
                                    updateTagClickState()
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = session.name,
                                                color = if (session.isObsoleteSession || signStatus[i].isObsoleteSession.value) Color(
                                                    0xFFFCC307
                                                ) else Color.Unspecified,
                                                textDecoration = if (successForOtherUser != true) TextDecoration.None else TextDecoration.LineThrough
                                            )
                                            Icon(
                                                painterResource(R.drawable.ic_triangle_alert),
                                                null,
                                                tint = Color(0xFFFCC307),
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .size(14.dp)
                                                    .visible(session.isObsoleteSession || signStatus[i].isObsoleteSession.value)
                                            )
                                        }
                                        Text(
                                            session.phoneNumber,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        userContent?.invoke(1 + index)
                                        signStatus[i].ResultCard {
                                            ignoreExceptionUserIndex = index to session
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { performSign() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isSigning.value.not()
                ) {
                    Text("签到")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        if (isSigning.value.not())
                            performSign()
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.ic_clipboard_pen_line),
                        contentDescription = "签到",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}