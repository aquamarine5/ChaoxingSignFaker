/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.RequireLoginAlertDialog
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.OtherUserTagType
import org.aquamarine5.brainspark.chaoxingsignfaker.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport
import sh.calvin.reorderable.ReorderableColumn
import kotlin.random.Random

@Serializable
object OtherUserDestination

@Serializable
object OtherUserGraphDestination

const val TAG_COLOR_UNSPECIFIED = -1L

@Composable
fun OtherUserScreen(naviBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHost = LocalSnackbarHostState.current
    var inputUrl by remember { mutableStateOf("") }
    var selectedUserIndexTagDialog by remember { mutableStateOf<Int?>(null) }
    var isTagsSettingDialog by remember { mutableStateOf(false) }
    var isInputDialog by remember { mutableStateOf(false) }
    var isURLSharedDialog by remember { mutableStateOf(false) }
    val isQRCodeScanPause = remember { mutableStateOf(false) }
    var isQRCodeScanning by remember { mutableStateOf(false) }
    var isQRCodeIllegal by remember { mutableStateOf(false) }
    val isQRCodeParsing = remember { mutableStateOf(false) }
    var isQRCodeImportSuccess by remember { mutableStateOf(false) }
    var isLocalSharedEntityReady by remember { mutableStateOf<Boolean?>(null) }
    var currentImportData by remember { mutableStateOf("") }
    var qrcodeIllegalText by remember { mutableStateOf("") }
    var importSharedEntity by remember { mutableStateOf<ChaoxingOtherUserSharedEntity?>(null) }
    val otherUserSessions = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }
    val tagsEntityList = remember { mutableStateListOf<OtherUserTagType>() }
    val userTagList = remember { mutableStateListOf<MutableState<List<OtherUserTagType>>>() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            context.chaoxingDataStore.data.first().let { datastore ->
                tagsEntityList.addAll(datastore.tagsLibraryList)
                otherUserSessions.addAll(datastore.otherUsersList)
                userTagList.addAll(buildList {
                    otherUserSessions.forEach { session ->
                        add(mutableStateOf(buildList {
                            session.tagsList.forEach { tagId ->
                                tagsEntityList.find { it.id == tagId }?.let { add(it) }
                            }
                        }))
                    }
                })
                isLocalSharedEntityReady = ChaoxingOtherUserHelper.checkSharedEntity(datastore)
            }
        }
    }
    var job: Job? = null
    val hapticFeedback = LocalHapticFeedback.current
    BackHandler(isQRCodeScanning) {
        isQRCodeScanning = false
    }
    if (isLocalSharedEntityReady == false) {
        RequireLoginAlertDialog(naviBack) {
            importSharedEntity = it
            isLocalSharedEntityReady = true
        }
    } else if (isLocalSharedEntityReady == true) {
        LaunchedEffect(Unit) {
            qrCode = ChaoxingOtherUserHelper.generateQRCode(context, importSharedEntity)
        }
    }
    if (isInputDialog) {
        AlertDialog(onDismissRequest = {
            isInputDialog = false
        }, confirmButton = {
            OutlinedButton(onClick = {
                isInputDialog = false
            }) {
                Text("关闭")
            }
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_text_cursor_input),
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        }, title = {
            Text("通过账号密码的形式添加他人的用户数据")
        }, text = {
            var phoneNumber by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            Column {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCC307)
                    ), modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painterResource(R.drawable.ic_triangle_alert),
                            contentDescription = "Alert",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "在添加他人账号前请先取得对方同意。",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.W500
                        )
                    }
                }
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("手机号") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                var isPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            isPasswordVisible = !isPasswordVisible
                        }) {
                            Icon(
                                if (isPasswordVisible) painterResource(R.drawable.ic_eye) else painterResource(
                                    R.drawable.ic_eye_closed
                                ), null
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        runCatching {
                            ChaoxingHttpClient.checkSharedEntity(
                                phoneNumber,
                                password,
                                context
                            )
                        }.onFailure {
                            it.snackbarReport(
                                snackbarHost,
                                coroutineScope,
                                "检查登录失败",
                                hapticFeedback
                            )
                        }.onSuccess { entity ->
                            runCatching {
                                ChaoxingOtherUserHelper.saveOtherUser(
                                    context,
                                    entity
                                )
                            }.onSuccess {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                                UMengHelper.onAccountOtherUserAddEvent(context, it)
                                otherUserSessions.add(it)
                                isInputDialog = false
                            }.onFailure {
                                it.snackbarReport(
                                    snackbarHost,
                                    coroutineScope,
                                    "保存用户失败",
                                    hapticFeedback
                                )
                                isInputDialog = false
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("添加") }
            }
        })
    }
    if (isTagsSettingDialog) {
        AlertDialog(onDismissRequest = {
            isTagsSettingDialog = false
        }, confirmButton = {
            OutlinedButton(onClick = {
                isTagsSettingDialog = false
            }) { Text("关闭") }
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_tags),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }, title = {
            Text("管理标签")
        }, text = {
            val mutex = remember { Mutex() }
            val tagUsageList = remember(isTagsSettingDialog) {
                if (isTagsSettingDialog) {
                    mutableStateListOf<MutableState<List<Int>>>().apply {
                        tagsEntityList.forEach { tag ->
                            add(
                                mutableStateOf(buildList {
                                    otherUserSessions.forEachIndexed { index, session ->
                                        if (session.tagsList.any { tag.id == it })
                                            add(index)
                                    }
                                })
                            )
                        }
                    }
                } else {
                    mutableStateListOf()
                }
            }
            var delectTagIndexForSecondaryConfirm by remember { mutableStateOf<Int?>(null) }
            var modifiedTagIndexForUserSelector by remember { mutableStateOf<Int?>(null) }
            var newTagColor by remember { mutableStateOf<Color?>(null) }
            var newTagName by remember { mutableStateOf("") }
            val newTagUserIndexList = remember { mutableListOf<Int>() }
            var isSelectNewTagUserDialog by remember { mutableStateOf(false) }
            var isSelectNewTagColorDialog by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            val createTagAction = {
                if (tagsEntityList.any { it.name == newTagName }) {
                    snackbarHost.displaySnackbar("$newTagName 标签已存在", coroutineScope)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                } else if (newTagName.isBlank()) {
                    snackbarHost.displaySnackbar("标签名称不能为空", coroutineScope)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                } else {
                    val newTagType = OtherUserTagType.newBuilder().apply {
                        setId(Random.nextInt(21_20061215))
                        setColor(newTagColor?.toArgb()?.toLong() ?: TAG_COLOR_UNSPECIFIED)
                        setName(newTagName)
                    }.build()
                    tagsEntityList.add(0, newTagType)
                    tagUsageList.add(0, mutableStateOf(newTagUserIndexList))
                    newTagUserIndexList.forEach {
                        userTagList[it].value = listOf(newTagType) + userTagList[it].value
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        mutex.withLock {
                            context.chaoxingDataStore.updateData { dataStore ->
                                dataStore.toBuilder().apply {
                                    addTagsLibrary(0, newTagType)
                                    newTagUserIndexList.forEach { index ->
                                        val session = otherUserSessions[index]
                                        val newSession = session.toBuilder().apply {
                                            addTags(newTagType.id)
                                        }.build()
                                        val sessionIndex =
                                            dataStore.otherUsersList.indexOfFirst { it.phoneNumber == session.phoneNumber }
                                        if (sessionIndex != -1) {
                                            setOtherUsers(sessionIndex, newSession)
                                        }
                                    }
                                }.build()
                            }
                            newTagName = ""
                            newTagColor = null
                            newTagUserIndexList.clear()
                        }
                    }
                }
            }
            if (delectTagIndexForSecondaryConfirm != null) {
                AlertDialog(
                    onDismissRequest = {
                        delectTagIndexForSecondaryConfirm = null
                    },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val tagId = tagsEntityList[delectTagIndexForSecondaryConfirm!!].id
                                mutex.withLock {
                                    context.chaoxingDataStore.updateData { dataStore ->
                                        dataStore.toBuilder().apply {
                                            otherUsersList.forEachIndexed { index, session ->
                                                if (session.tagsList.any { it == tagId }) {
                                                    val newTagsList =
                                                        session.tagsList.filter { it != tagId }
                                                    val newSession = session.toBuilder().apply {
                                                        clearTags()
                                                        addAllTags(newTagsList)
                                                    }.build()
                                                    setOtherUsers(index, newSession)
                                                }
                                            }
                                            val tagIndex =
                                                dataStore.tagsLibraryList.indexOfFirst { it.id == tagId }
                                            if (tagIndex != -1) {
                                                removeTagsLibrary(tagIndex)
                                            }
                                        }.build()
                                    }
                                }
                                tagUsageList.removeAt(delectTagIndexForSecondaryConfirm!!)
                                tagsEntityList.removeAt(delectTagIndexForSecondaryConfirm!!)
                                delectTagIndexForSecondaryConfirm = null
                            }
                        }) {
                            Text("删除")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = {
                            delectTagIndexForSecondaryConfirm = null
                        }) {
                            Text("取消")
                        }
                    },
                    title = {
                        Text("确认删除标签${tagsEntityList[delectTagIndexForSecondaryConfirm!!].name}？")
                    },
                    text = {
                        Text("删除标签会同时将该标签从所有用户中移除，此操作不可撤销。")
                    },
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            null,
                            tint = Color.Red,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
            if (modifiedTagIndexForUserSelector != null) {
                val modifiedUserList =
                    remember(modifiedTagIndexForUserSelector) {
                        List(otherUserSessions.size) {
                            mutableStateOf<Boolean?>(
                                null
                            )
                        }
                    }
                var isSavingDatastore by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        Button(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            coroutineScope.launch(Dispatchers.IO) {
                                isSavingDatastore = true
                                val newTagUsage = mutableListOf<Int>()
                                modifiedUserList.forEachIndexed { index, state ->
                                    if (state.value == null) {
                                        if (tagUsageList[modifiedTagIndexForUserSelector!!].value.any { it == index })
                                            newTagUsage.add(index)
                                        return@forEachIndexed
                                    }
                                    if (state.value == true) {
                                        newTagUsage.add(index)
                                    }
                                    val tagId = tagsEntityList[modifiedTagIndexForUserSelector!!].id
                                    val session = otherUserSessions[index]
                                    val newUserTag = userTagList[index].value.toMutableList()
                                    val newSession = session.toBuilder().apply {
                                        if (state.value!!) {
                                            newUserTag.add(
                                                0,
                                                tagsEntityList[modifiedTagIndexForUserSelector!!]
                                            )
                                            if (session.tagsList.none { it == tagId }) {
                                                addTags(tagsEntityList[modifiedTagIndexForUserSelector!!].id)
                                            }
                                        } else {
                                            newUserTag.remove(tagsEntityList[modifiedTagIndexForUserSelector!!])
                                            val tagIndexInSession =
                                                session.tagsList.indexOfFirst { it == tagId }
                                            if (tagIndexInSession != -1) {
                                                val newTagsList =
                                                    session.tagsList.filter { it != tagId }
                                                clearTags()
                                                addAllTags(newTagsList)
                                            }
                                        }
                                    }.build()
                                    userTagList[index].value = newUserTag
                                    otherUserSessions[index] = newSession
                                    mutex.withLock {
                                        context.chaoxingDataStore.updateData { dataStore ->
                                            val sessionIndex =
                                                dataStore.otherUsersList.indexOfFirst { it.phoneNumber == session.phoneNumber }
                                            dataStore.toBuilder().apply {
                                                if (sessionIndex != -1) {
                                                    setOtherUsers(sessionIndex, newSession)
                                                }
                                            }.build()
                                        }
                                    }
                                }
                                tagUsageList[modifiedTagIndexForUserSelector!!].value = newTagUsage
                                isSavingDatastore = false
                                modifiedTagIndexForUserSelector = null
                            }
                        }, enabled = !isSavingDatastore) {
                            Text(if (isSavingDatastore) "保存中" else "保存")
                        }
                    }, dismissButton = {
                        OutlinedButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            modifiedTagIndexForUserSelector = null
                        }) {
                            Text("不保存")
                        }
                    }, title = {
                        if (modifiedTagIndexForUserSelector != null)
                            Text(buildAnnotatedString {
                                append("修改 ")
                                withStyle(SpanStyle(color = Color(tagsEntityList[modifiedTagIndexForUserSelector!!].color))) {
                                    append(tagsEntityList[modifiedTagIndexForUserSelector!!].name)
                                }
                                append(" 标签的用户")
                            })
                    }, text = {
                        LazyColumn {
                            otherUserSessions.forEachIndexed { index, session ->
                                item {
                                    key(session.phoneNumber) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                modifiedUserList[index].value
                                                    ?: if (modifiedTagIndexForUserSelector != null) tagUsageList[modifiedTagIndexForUserSelector!!].value.any { it == index } else false,
                                                onCheckedChange = {
                                                    modifiedUserList[index].value = it
                                                }
                                            )
                                            Text(
                                                text = buildAnnotatedString {
                                                    append(session.name)
                                                    withStyle(
                                                        SpanStyle(
                                                            color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                                                            fontSize = 12.sp
                                                        )
                                                    ) {
                                                        append(" (${session.phoneNumber})")
                                                    }
                                                },
                                                fontSize = 14.sp,
                                                lineHeight = 16.sp,
                                                style = TextStyle.Default.copy(
                                                    lineBreak = LineBreak.Paragraph
                                                ),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .clickable {
                                                        modifiedUserList[index].value =
                                                            (modifiedUserList[index].value
                                                                ?: tagUsageList[modifiedTagIndexForUserSelector!!].value.any { it == index }).not()
                                                    }
                                                    .fillMaxWidth()
                                                    .padding(0.dp, 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }, icon = {
                        Icon(
                            painterResource(R.drawable.ic_user_round_cog),
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    })
            }
            if (isSelectNewTagColorDialog) {
                var color by remember(newTagColor) { mutableStateOf(newTagColor) }
                AlertDialog(onDismissRequest = {
                    isSelectNewTagColorDialog = false
                }, confirmButton = {
                    Button(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        newTagColor = color
                        isSelectNewTagColorDialog = false
                    }) {
                        Text("保存")
                    }
                }, dismissButton = {
                    OutlinedButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        newTagColor = null
                        isSelectNewTagColorDialog = false
                    }) {
                        Text("恢复默认")
                    }
                }, icon = {
                    Icon(
                        painterResource(R.drawable.ic_palette),
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }, title = {
                    Text("设置标签颜色")
                }, text = {
                    val controller = rememberColorPickerController()
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HsvColorPicker(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            controller = controller,
                            onColorChanged = {
                                color = it.color
                            })
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color ?: Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSystemInDarkTheme()) Color.White else Color.Black,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }

                })
            }
            if (isSelectNewTagUserDialog) {
                AlertDialog(onDismissRequest = {
                    isSelectNewTagUserDialog = false
                }, confirmButton = {
                    Button(onClick = {
                        isSelectNewTagUserDialog = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }) {
                        Text("关闭")
                    }
                }, title = {
                    Text("为新标签选择用户")
                }, icon = {
                    Icon(
                        painterResource(R.drawable.ic_user_round_cog),
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }, text = {
                    LazyColumn {
                        otherUserSessions.forEachIndexed { index, session ->
                            item {
                                key(session.phoneNumber) {
                                    var isSelected by remember {
                                        mutableStateOf(newTagUserIndexList.contains(index))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            isSelected, onCheckedChange = {
                                                isSelected = it
                                                if (it)
                                                    newTagUserIndexList.add(index)
                                                else
                                                    newTagUserIndexList.remove(index)
                                            }
                                        )
                                        Text(
                                            text = buildAnnotatedString {
                                                append(session.name)
                                                withStyle(
                                                    SpanStyle(
                                                        color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                                                        fontSize = 12.sp
                                                    )
                                                ) {
                                                    append(" (${session.phoneNumber})")
                                                }
                                            },
                                            fontSize = 14.sp,
                                            lineHeight = 16.sp,
                                            style = TextStyle.Default.copy(
                                                lineBreak = LineBreak.Paragraph
                                            ),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clickable {
                                                    isSelected = !isSelected
                                                    if (isSelected)
                                                        newTagUserIndexList.add(index)
                                                    else
                                                        newTagUserIndexList.remove(index)
                                                }
                                                .fillMaxWidth()
                                                .padding(0.dp, 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                })
            }
            Column {
                Card(
                    onClick = {
                        focusRequester.requestFocus()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp, 0.dp)
                    ) {
                        IconButton(onClick = {
                            isSelectNewTagColorDialog = true
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_palette),
                                null,
                                tint = if (newTagColor == null) {
                                    if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                } else newTagColor!!
                            )
                        }
                        BasicTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            cursorBrush = SolidColor(if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions { createTagAction() },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    if (it.isFocused) keyboardController?.show()
                                }
                                .weight(1f)
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = size.height + 3.dp.toPx() - strokeWidth / 2
                                    drawLine(
                                        color = Color.Gray,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth
                                    )
                                },
                            textStyle = TextStyle(
                                color = LocalContentColor.current,
                                lineHeight = 16.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (newTagName.isEmpty()) {
                                        Text(
                                            "新标签名称",
                                            style = LocalTextStyle.current.copy(
                                                color = LocalContentColor.current.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        IconButton(onClick = {
                            isSelectNewTagUserDialog = true
                        }) {
                            Icon(painterResource(R.drawable.ic_user_round_cog), null)
                        }
                        IconButton(onClick = {
                            createTagAction()
                        }) {
                            Icon(painterResource(R.drawable.ic_tag_plus_outline), null)
                        }
                    }
                }
                ReorderableColumn(list = tagsEntityList.toList(), onMove = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }, onSettle = { from, to ->
                    tagUsageList.add(to, tagUsageList.removeAt(from))
                    tagsEntityList.add(to, tagsEntityList.removeAt(from))
                    coroutineScope.launch(Dispatchers.IO) {
                        mutex.withLock {
                            context.chaoxingDataStore.updateData { datastore ->
                                datastore.toBuilder().apply {
                                    val sortedValue = getTagsLibrary(from)
                                    removeTagsLibrary(from)
                                    addTagsLibrary(to, sortedValue)
                                }.build()
                            }
                        }
                        snackbarHost.currentSnackbarData?.dismiss()
                        snackbarHost.showSnackbar("新顺序已保存", withDismissAction = true)
                    }
                }) { index, tagEntity, _ ->
                    key(tagEntity.id) {
                        ReorderableItem {
                            val interactionSource = remember { MutableInteractionSource() }
                            Card(
                                onClick = {},
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp, 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp)
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_tag_outline),
                                        null,
                                        tint = if (tagEntity.color == TAG_COLOR_UNSPECIFIED) {
                                            if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                        } else Color(
                                            tagEntity.color
                                        ),
                                        modifier = Modifier.padding(3.dp, 0.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tagEntity.name)
                                        Text(
                                            buildAnnotatedString {
                                                tagUsageList.getOrNull(index)?.value?.let {
                                                    if (it.isEmpty()) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                                        append("未被使用")
                                                    }
                                                    else
                                                        it.forEachIndexed { index, userIndex ->
                                                            append(otherUserSessions[userIndex].name)
                                                            if (index != it.size - 1) append(", ")
                                                        }
                                                }
                                            },
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp,
                                            color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            modifiedTagIndexForUserSelector = index
                                        }) {
                                            Icon(
                                                painterResource(R.drawable.ic_user_round_cog),
                                                null
                                            )
                                        }
                                        IconButton(onClick = {
                                            delectTagIndexForSecondaryConfirm = index
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = "Delete",
                                                tint = Color(0xFFF1441D)
                                            )
                                        }
                                        IconButton(
                                            modifier = Modifier.draggableHandle(
                                                interactionSource = interactionSource,
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureThresholdActivate
                                                    )
                                                }, onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureEnd
                                                    )
                                                }
                                            ), onClick = {}) {
                                            Icon(
                                                painterResource(R.drawable.ic_drag_handle_rounded),
                                                "",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }
    if (selectedUserIndexTagDialog != null) {
        val modifiedTagIndexList = remember(selectedUserIndexTagDialog) {
            List(tagsEntityList.size) {
                mutableStateOf(userTagList[selectedUserIndexTagDialog!!].value.any { tagEntity ->
                    tagEntity.id == tagsEntityList[it].id
                })
            }
        }
        var isSavingDatastore by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    painterResource(R.drawable.ic_tag),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }, confirmButton = {
                Button(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        isSavingDatastore = true
                        context.chaoxingDataStore.updateData { dataStore ->
                            dataStore.toBuilder().apply {
                                val newTagList = modifiedTagIndexList.mapIndexed { index, state ->
                                    if (!state.value) null
                                    else {
                                        tagsEntityList[index].id
                                    }
                                }.filterNotNull()
                                userTagList[selectedUserIndexTagDialog!!].value =
                                    newTagList.map { tagId ->
                                        tagsEntityList.first { it.id == tagId }
                                    }
                                setOtherUsers(
                                    selectedUserIndexTagDialog!!,
                                    otherUserSessions[selectedUserIndexTagDialog!!].toBuilder()
                                        .apply {
                                            clearTags()
                                            addAllTags(newTagList)
                                        })

                            }.build()
                        }
                        isSavingDatastore = false
                        selectedUserIndexTagDialog = null
                    }
                }, enabled = !isSavingDatastore) {
                    Text(if (isSavingDatastore) "保存中" else "保存")
                }
            }, dismissButton = {
                OutlinedButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    selectedUserIndexTagDialog = null
                }) { Text("不保存退出") }
            }, title = {
                if (selectedUserIndexTagDialog != null)
                    Text("为${otherUserSessions[selectedUserIndexTagDialog!!].name}添加标签")
            }, text = {
                LazyColumn {
                    itemsIndexed(tagsEntityList) { index, tagEntity ->
                        key(tagEntity.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(3.dp)
                            ) {
                                Checkbox(
                                    checked = modifiedTagIndexList[index].value,
                                    onCheckedChange = {
                                        modifiedTagIndexList[index].value = it
                                    })
                                Icon(
                                    painterResource(R.drawable.ic_tag),
                                    null,
                                    tint = Color(tagEntity.color),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(0.dp, 2.dp)
                                        .clickable {
                                            modifiedTagIndexList[index].value =
                                                !modifiedTagIndexList[index].value
                                        }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    tagEntity.name, modifier = Modifier
                                        .clickable {
                                            modifiedTagIndexList[index].value =
                                                !modifiedTagIndexList[index].value
                                        }
                                        .fillMaxWidth())
                            }
                        }
                    }
                }
            })
    }
    if (isURLSharedDialog) {
        AlertDialog(onDismissRequest = {
            isURLSharedDialog = false
        }, confirmButton = {
            OutlinedButton(onClick = {
                isURLSharedDialog = false
            }) { Text("关闭") }
        }, title = {
            Text("通过文本链接的形式分享自己的用户数据")
        }, icon = {
            Icon(
                painterResource(R.drawable.ic_link),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }, text = {
            Column {
                Text("对方将链接从浏览器打开即可导入你的用户数据（对方需更新到1.5版本及以上），或将链接粘贴到以下输入框中：")
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(inputUrl, onValueChange = {
                    inputUrl = it
                }, label = {
                    Text("链接")
                }, singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    IconButton(onClick = {
                        val result =
                            context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(
                                0
                            )?.text
                        if (result.isNullOrEmpty()) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            Toast.makeText(context, "读取剪切板失败", Toast.LENGTH_SHORT).show()
                        } else {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            inputUrl = result.toString()
                        }
                    }) {
                        Icon(painterResource(R.drawable.ic_clipboard_copy), null)
                    }
                    FilledTonalButton(onClick = {
                        if (inputUrl.isNotBlank()) {
                            val url = inputUrl.toHttpUrlOrNull()
                            if (url == null) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                Toast.makeText(context, "链接格式错误", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            val phone = url.queryParameter("phone")
                            val pwd = url.queryParameter("pwd")
                            val name = url.queryParameter("name")
                            if (phone == null || pwd == null || name == null) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                Toast.makeText(context, "链接格式错误", Toast.LENGTH_SHORT).show()
                                return@FilledTonalButton
                            }
                            coroutineScope.launch {
                                runCatching {
                                    ChaoxingOtherUserHelper.saveOtherUser(
                                        context,
                                        ChaoxingOtherUserSharedEntity(phone, pwd, name)
                                    )
                                }.onSuccess {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                                    UMengHelper.onAccountOtherUserAddEvent(context, it)
                                    isURLSharedDialog = false
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "导入失败",
                                        hapticFeedback
                                    )
                                    isURLSharedDialog = false
                                }
                            }
                        } else {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            Toast.makeText(context, "链接不能为空", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_user_round_plus),
                                contentDescription = "Add User"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从链接导入用户", fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        context.startActivity(Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                ChaoxingOtherUserHelper.getSharedUrl(context, importSharedEntity)
                            )
                            putExtra(
                                Intent.EXTRA_TITLE,
                                "${ChaoxingHttpClient.instance!!.userEntity.name} 的用户数据链接"
                            )
                        }, "分享自己的链接给他人"))
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            contentDescription = "Add User"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("分享自己的链接给他人", fontSize = 16.sp)
                    }
                }

            }
        })
    }
    Box(
        modifier = Modifier
            .zIndex(0f)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp, 0.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val qrcodeSize = ChaoxingOtherUserHelper.getQRCodeDpSize(context)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(9.dp)
                    )
                    .size(qrcodeSize + 18.dp, qrcodeSize + 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(qrCode) { v ->
                    if (v != null) {
                        Image(bitmap = v.asImageBitmap(), contentDescription = "QR Code")
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                buildAnnotatedString {
                    append("使用其他设备打开 ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("随地大小签APP")
                    }
                    append(" 扫描二维码\n以将你的账号添加到其他设备中")
                },
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFCC307)
                ), modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp, 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painterResource(R.drawable.ic_triangle_alert),
                        contentDescription = "Alert",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "随意分享此二维码给他人会增加你的学习通账号风险，他人可以通过此二维码来控制你的账号，但这个分享行为只针对于学习通，并不会暴露你的实际密码。",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    isTagsSettingDialog = true
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp, 0.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_tags),
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "管理用户标签（测试版）",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp, 2.dp)
            ) {
                SegmentedButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isQRCodeScanning = true
                    }, shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 3
                    ), selected = false, colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_scan_qr_code),
                            contentDescription = "Add User",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("扫码添加")
                    }
                }
                SegmentedButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isURLSharedDialog = true
                    }, shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 3
                    ), selected = false, colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            contentDescription = "Add User",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("链接添加")
                    }
                }
                SegmentedButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isInputDialog = true
                    }, shape = SegmentedButtonDefaults.itemShape(
                        index = 2,
                        count = 3
                    ), selected = false, colors = SegmentedButtonDefaults.colors(
                        inactiveContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_text_cursor_input),
                            null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("账密添加")
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 5.dp, 8.dp, 8.dp)
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        "已添加的用户",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                var requestedDeleteUserIndex by remember { mutableStateOf<Int?>(null) }
                if (requestedDeleteUserIndex != null) {
                    AlertDialog(
                        onDismissRequest = {
                            requestedDeleteUserIndex = null
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                null,
                                tint = Color.Red,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        title = {
                            Text("删除用户")
                        },
                        text = {
                            Text("确定要删除此用户吗？此操作不可撤销。")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val index = requestedDeleteUserIndex!!
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            context.chaoxingDataStore.updateData { datastore ->
                                                datastore.toBuilder()
                                                    .apply {
                                                        removeOtherUsers(index)
                                                    }
                                                    .build()
                                            }
                                        }
                                    }
                                    otherUserSessions.removeAt(index)
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    requestedDeleteUserIndex = null
                                }
                            ) {
                                Text("删除")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(
                                        HapticFeedbackType.ContextClick
                                    )
                                    requestedDeleteUserIndex = null
                                }
                            ) {
                                Text("取消")
                            }
                        }
                    )
                }

                if (otherUserSessions.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "暂无其他用户",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    val mutex = remember { Mutex() }
                    ReorderableColumn(list = otherUserSessions.toList(), onMove = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }, onSettle = { from, to ->
                        userTagList.add(to, userTagList.removeAt(from))
                        otherUserSessions.add(to, otherUserSessions.removeAt(from))
                        coroutineScope.launch(Dispatchers.IO) {
                            mutex.withLock {
                                context.chaoxingDataStore.updateData { datastore ->
                                    datastore.toBuilder().apply {
                                        val sortedValue = getOtherUsers(from)
                                        removeOtherUsers(from)
                                        addOtherUsers(to, sortedValue)
                                    }.build()
                                }
                            }
                            snackbarHost.currentSnackbarData?.dismiss()
                            snackbarHost.showSnackbar("新顺序已保存", withDismissAction = true)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { index, user, _ ->
                        key(user.phoneNumber) {
                            ReorderableItem {
                                val interactionSource = remember { MutableInteractionSource() }
                                Card(
                                    onClick = {},
                                    interactionSource = interactionSource,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp, 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(17.dp, 2.dp, 6.dp, 2.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(0.dp, 2.dp)
                                            ) {
                                                Text(
                                                    text = buildAnnotatedString {
                                                        append(user.name)
                                                        withStyle(
                                                            SpanStyle(
                                                                color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                                                                fontSize = 12.sp
                                                            )
                                                        ) {
                                                            append(" (${user.phoneNumber})")
                                                        }
                                                    },
                                                    fontSize = 14.sp,
                                                    lineHeight = 16.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier
                                                        .fillMaxWidth(),
                                                )
                                                if (userTagList.getOrNull(index)?.value?.isNotEmpty() == true) {
                                                    Spacer(
                                                        modifier = Modifier.padding(
                                                            0.dp,
                                                            2.dp,
                                                            0.dp,
                                                            0.dp
                                                        )
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            painterResource(R.drawable.ic_tag),
                                                            null
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        val fontSize10spStyle =
                                                            remember { SpanStyle(fontSize = 10.sp) }
                                                        Text(
                                                            buildAnnotatedString {
                                                                userTagList[index].value.forEachIndexed { tagIndex, tagEntity ->
                                                                    withStyle(
                                                                        SpanStyle(
                                                                            color = if (tagEntity.color == TAG_COLOR_UNSPECIFIED) {
                                                                                if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                                                            } else Color(
                                                                                tagEntity.color
                                                                            ), fontSize = 10.sp
                                                                        )
                                                                    ) {
                                                                        append(tagEntity.name)
                                                                    }
                                                                    if (tagIndex != userTagList[index].value.size - 1)
                                                                        withStyle(fontSize10spStyle) {
                                                                            append(", ")
                                                                        }
                                                                }
                                                            },
                                                            lineHeight = 12.sp,
                                                            style = TextStyle.Default.copy(
                                                                lineBreak = LineBreak.Paragraph
                                                            ),
                                                            modifier = Modifier.padding(0.dp, 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        selectedUserIndexTagDialog = index
                                                    }
                                                ) {
                                                    Icon(painterResource(R.drawable.ic_tags), null)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        requestedDeleteUserIndex = index
                                                        hapticFeedback.performHapticFeedback(
                                                            HapticFeedbackType.ContextClick
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_delete),
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFF1441D)
                                                    )
                                                }

                                                IconButton(
                                                    modifier = Modifier.draggableHandle(
                                                        interactionSource = interactionSource,
                                                        onDragStarted = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.GestureThresholdActivate
                                                            )
                                                        }, onDragStopped = {
                                                            hapticFeedback.performHapticFeedback(
                                                                HapticFeedbackType.GestureEnd
                                                            )
                                                        }
                                                    ), onClick = {}) {
                                                    Icon(
                                                        painterResource(R.drawable.ic_drag_handle_rounded),
                                                        "",
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
    if (isQRCodeImportSuccess) {
        AlertDialog(
            onDismissRequest = {
                isQRCodeImportSuccess = false
            },
            title = {
                Text("导入成功")
            },
            text = {
                Text("$currentImportData 用户已经成功导入")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isQRCodeImportSuccess = false
                    }
                ) {
                    Text("确定")
                }
            }
        )
    }
    AnimatedVisibility(
        isQRCodeScanning, enter =
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(
                animationSpec = tween(300)
            ), exit =
            scaleOut(targetScale = 0.8f, animationSpec = tween(300)) + fadeOut(
                animationSpec = tween(300)
            )
    ) {
        Column(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            QRCodeScanComponent(isQRCodeScanPause, isQRCodeParsing, onClose = {
                isQRCodeScanning = false
            }, onScanResult = { qr ->
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            isQRCodeParsing.value = true
                            isQRCodeScanPause.value = true
                            return@runCatching ChaoxingOtherUserSharedEntity.parseFromQRCode(qr)
                        }.onSuccess { sharedEntity ->
                            coroutineScope.launch {
                                runCatching {
                                    ChaoxingOtherUserHelper.saveOtherUser(context, sharedEntity)
                                }.onSuccess {
                                    isQRCodeScanning = false
                                    isQRCodeParsing.value = false
                                    currentImportData =
                                        "${sharedEntity.userName}(手机号：${sharedEntity.phoneNumber})"
                                    isQRCodeImportSuccess = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    otherUserSessions.add(it)
                                    UMengHelper.onAccountOtherUserAddEvent(context, it)
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbarHost,
                                        coroutineScope,
                                        "导入失败",
                                        hapticFeedback
                                    )
                                    isQRCodeIllegal = true
                                    isQRCodeParsing.value = false
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                                    qrcodeIllegalText = it.message ?: "二维码解析失败，登录失败。"
                                    job?.cancel()
                                    job = coroutineScope.launch {
                                        delay(3000)
                                        isQRCodeScanPause.value = false
                                        isQRCodeIllegal = false
                                    }
                                }
                            }
                        }.onFailure {
                            it.snackbarReport(
                                snackbarHost,
                                coroutineScope,
                                "二维码解析失败",
                                hapticFeedback
                            )
                            isQRCodeIllegal = true
                            isQRCodeScanPause.value = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                            qrcodeIllegalText = it.message ?: "二维码解析失败，不是正确码。"
                            job?.cancel()
                            job = coroutineScope.launch {
                                delay(3000)
                                isQRCodeScanPause.value = false
                                isQRCodeIllegal = false
                            }
                        }
                    }
                }
            }) {
                Column(
                    modifier = Modifier
                        .offset(y = Dp(resources.displayMetrics.run {
                            0.75f * heightPixels / density
                        }))
                        .zIndex(2f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Crossfade(isQRCodeIllegal) {
                        when (it) {
                            true -> {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            Color(0x72F1441D),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(2.dp, Color(0xFFF1441D)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_octagon_alert),
                                        contentDescription = "Illegal QR Code"
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(qrcodeIllegalText)
                                }
                            }

                            false -> {
                                Row(
                                    modifier = Modifier
                                        .background(
                                            Color(0x88888888),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(2.dp, Color(0xFF444444)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_scan_qr_code),
                                        contentDescription = "Scan QR Code"
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("扫描其他设备的二维码以添加用户")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
