/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingIMHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.NetworkExceptionComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingEasemobIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalImageLoader
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.snackbarReport

@Serializable
data class GroupListDestination(
    val isCloneSession: Boolean
)

@Composable
fun GroupListScreen(
    destination: GroupListDestination,
    navToGroupDetail: (GroupDetailDestination) -> Unit,
    navBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .fillMaxSize()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        val hapticFeedback = LocalHapticFeedback.current
        var imGroupsInfo by rememberSaveable(
            stateSaver = Saver(
                save = { list ->
                    list?.let {
                        Json.encodeToString(
                            ListSerializer(ChaoxingEasemobIMGroup.serializer()),
                            it
                        )
                    }
                },
                restore = { value ->
                    Json.decodeFromString(
                        ListSerializer(ChaoxingEasemobIMGroup.serializer()),
                        value
                    )
                }
            )
        ) { mutableStateOf<List<ChaoxingEasemobIMGroup>?>(null) }
        var isFetchedFailure by remember { mutableStateOf<Result<*>?>(null) }
        val preferredGroupIds = remember { mutableStateListOf<String>() }

        LaunchedEffect(Unit) {
            isFetchedFailure = runCatching {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val datastoreData = context.chaoxingDataStore.data.first()
                        preferredGroupIds.addAll(datastoreData.preferGroupIdList)
                    }
                }
                if (imGroupsInfo == null)
                    imGroupsInfo = ChaoxingIMHelper.getEasemobIMGroups(
                        ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)!!,
                        ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)!!
                            .getIMConfig()
                    )
            }.onFailure {
                if (it is CancellationException) return@onFailure
                it.snackbarReport(
                    snackbarHostState,
                    coroutineScope,
                    "获取群列表失败",
                    hapticFeedback
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    navBack()
                }
        ) {
            Icon(
                painterResource(R.drawable.ic_arrow_left),
                contentDescription = "返回"
            )
            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .width(5.dp)
            )
            Icon(
                painterResource(R.drawable.ic_users_round),
                contentDescription = "群聊列表",
                tint= MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "当前正从群聊列表中查找签到",
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        Crossfade(isFetchedFailure) { v ->
            when {
                v == null -> {
                    CenterCircularProgressIndicator()
                }

                v.isFailure -> {
                    NetworkExceptionComponent(v.exceptionOrNull()!!) {
                        coroutineScope.launch {
                            isFetchedFailure = runCatching {
                                imGroupsInfo = ChaoxingIMHelper.getEasemobIMGroups(
                                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)!!,
                                    ChaoxingHttpClient.getHttpInstanceOrClone(destination.isCloneSession)!!
                                        .getIMConfig()
                                )
                            }.onFailure {
                                if (it is CancellationException) return@onFailure
                                it.snackbarReport(
                                    snackbarHostState,
                                    coroutineScope,
                                    "获取群列表失败",
                                    hapticFeedback
                                )
                            }

                        }
                        isFetchedFailure = null
                    }
                }

                imGroupsInfo!!.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.align(Alignment.Center)) {
                            Icon(painterResource(R.drawable.ic_circle_question_mark), null)
                            Text("暂无课程，请检查登录的学习通账号是否正确。")
                        }
                    }
                }

                else -> {
                    var searchQuery by rememberSaveable { mutableStateOf("") }
                    val displayGroups =
                        (if (searchQuery.isBlank()) imGroupsInfo!!
                        else imGroupsInfo!!.filter {
                            it.chatName.contains(searchQuery, ignoreCase = true)
                        }).sortedByDescending { preferredGroupIds.contains(it.id) }
                    val groupedGroups = remember(displayGroups) {
                        displayGroups.groupBy { it.chatName }.values.toList()
                    }
                    LazyColumn {
                        stickyHeader(key = "group_search") {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("搜索群聊名称") },
                                        leadingIcon = {
                                            Icon(
                                                painterResource(R.drawable.ic_search),
                                                contentDescription = "搜索"
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.ContextClick
                                                    )
                                                    searchQuery = ""
                                                }) {
                                                    Icon(
                                                        painterResource(R.drawable.ic_x),
                                                        contentDescription = "清除搜索"
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                }
                            }
                        }
                        if (searchQuery.isNotBlank() && groupedGroups.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_circle_question_mark),
                                        null
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("没有找到与“$searchQuery”匹配的群聊")
                                }
                            }
                        }
                        items(groupedGroups, key = {
                            it.first().chatName
                        }) { group ->
                            val item = group.first()
                            val isPreferred = preferredGroupIds.contains(item.id)
                            val starTint by animateColorAsState(
                                targetValue = if (isPreferred) Color.Yellow else Color.Gray
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        placementSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                        ),
                                        fadeInSpec = spring(
                                            stiffness = Spring.StiffnessMedium),
                                        fadeOutSpec = spring(
                                            stiffness = Spring.StiffnessMedium)
                                    )
                            ) {
                                Button(
                                    onClick = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        navToGroupDetail(GroupDetailDestination(group))
                                    }, shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            GroupAvatar(imageUrl = item.imageUrl)
                                            Text(
                                                text = item.chatName,
                                                modifier = Modifier.padding(start = 16.dp)
                                            )
                                            if (group.size > 1) {
                                                Text(
                                                    text = "(x${group.size})",
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                painterResource(R.drawable.ic_star_fill),
                                                contentDescription = if (isPreferred) "取消星标" else "星标置顶",
                                                tint = starTint,
                                                modifier = Modifier.clickable {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.ContextClick
                                                    )
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        if (isPreferred) {
                                                            context.chaoxingDataStore.updateData { dataStore ->
                                                                dataStore.toBuilder().apply {
                                                                    val newList =
                                                                        preferGroupIdList.filterNot { it == item.id }
                                                                    clearPreferGroupId()
                                                                    addAllPreferGroupId(newList)
                                                                }.build()
                                                            }
                                                            preferredGroupIds.remove(item.id)
                                                        } else {
                                                            context.chaoxingDataStore.updateData {
                                                                it.toBuilder()
                                                                    .addPreferGroupId(item.id)
                                                                    .build()
                                                            }
                                                            preferredGroupIds.add(item.id)
                                                        }
                                                    }
                                                })
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupAvatar(
    imageUrl: String?,
    size: Dp = 50.dp
) {
    val imageLoader = LocalImageLoader.current
    val shape = RoundedCornerShape(3.dp)
    var isLoadFailed by remember(imageUrl) { mutableStateOf(false) }
    val showPlaceholder = imageUrl.isNullOrBlank() || isLoadFailed
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (showPlaceholder) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    shape
                ) else Modifier
            )
            .clip(shape)
    ) {
        if (!showPlaceholder) {
            AsyncImage(
                model = imageUrl,
                modifier = Modifier.fillMaxSize(),
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                onError = {
                    Log.w(
                        "GroupListScreen",
                        "Error loading image: ${it.result}"
                    )
                    isLoadFailed = true
                }
            )
        }
    }
}

