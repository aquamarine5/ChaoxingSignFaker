/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus

@Composable
fun OtherUserSelectorComponent(
    navToOtherUser: () -> Unit,
    signStatus: MutableList<ChaoxingSignStatus>,
    isCurrentAlreadySigned: Boolean,
    userContent: @Composable ((index: Int) -> Unit)? = null,
    onSignAction: (isSelf: Boolean, otherUserSessionList: List<ChaoxingOtherUserSession>, indexList: List<Int>) -> Unit
) {
    LocalContext.current.let { context ->
        val signUserList = remember { mutableStateListOf<ChaoxingOtherUserSession>() }
        val userSelections = remember { mutableStateListOf(true) }
        var success by signStatus[0].isSuccess
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp, 2.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    onClick = {
                        navToOtherUser()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFCD337)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp, 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.ic_lightbulb),
                            contentDescription = "Help",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            "如果你还没有添加其他用户，可以点击跳转添加用户向导。",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.W500,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        signUserList.addAll(context.chaoxingDataStore.data.first().let { data ->
                            data.otherUsersList.filter {
                                it.phoneNumber != data.loginSession.phoneNumber
                            }
                        })
                        signStatus.addAll(Array(signUserList.size) {
                            ChaoxingSignStatus()
                        })
                        userSelections.addAll(List(signUserList.size) { false })
                        success = isCurrentAlreadySigned
                        userSelections[0] = isCurrentAlreadySigned != true
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "选择要进行签到的用户：",
                    modifier = Modifier.padding(start = 3.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp, 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = userSelections[0] && signStatus[0].isSuccess.value != true,
                            onCheckedChange = { isChecked ->
                                userSelections[0] = isChecked
                            },
                            enabled = (success == true).not()
                        )
                        Row(modifier = Modifier.clickable((success == true).not()) {
                            userSelections[0] = userSelections[0].not()
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "给自己签到",
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (success != true) TextDecoration.None else TextDecoration.LineThrough
                            )
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
                    signUserList.forEachIndexed { index, userSelection ->
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
                                        userSelections[i] = isChecked
                                    },
                                    enabled = (successForOtherUser == true).not()
                                )
                                Row(modifier = Modifier.clickable((successForOtherUser == true).not()) {
                                    userSelections[i] = userSelections[i].not()
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = userSelection.name,
                                        textDecoration = if (successForOtherUser != true) TextDecoration.None else TextDecoration.LineThrough
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        userContent?.invoke(1 + index)
                                        signStatus[i].ResultCard()
                                    }
                                }
                            }
                        }
                    }
                }

                Button(onClick = {
                    if (!userSelections.any { it }) {
                        Toast.makeText(context, "请选择要签到的用户", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }
                    if (signStatus.all { it.isSuccess.value == true }) {
                        Toast.makeText(context, "所有用户均已签到", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }
                    val indexList = mutableListOf<Int>()
                    // 0 1 2 3 4 5 6
                    // 2 3 5
                    if (userSelections[0] && signStatus[0].isSuccess.value != true)
                        indexList.add(0)
                    onSignAction(
                        userSelections[0] && signStatus[0].isSuccess.value != true,
                        signUserList.filterIndexed { index, _ ->
                            (userSelections[index + 1] && signStatus[1 + index].isSuccess.value != true).apply {
                                if (this) {
                                    indexList.add(index + 1)
                                }
                            }
                        }, indexList
                    )
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("签到")
                }
            }
        }
    }
}