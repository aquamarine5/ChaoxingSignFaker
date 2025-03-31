/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun SponsorAlertDialog(showDialog: MutableState<Boolean>) {
    val sponsorList = listOf(
        listOf("催什么崔", "8.88"),
        listOf("不愿透露姓名的耿先生", "8.88"),
        listOf("不愿透露姓名的景先生", "6.66"),
        listOf("不愿透露姓名的张先生", "2.88"),
    )
    var isShowDialog by showDialog
    if (isShowDialog) {
        AlertDialog(onDismissRequest = {
            isShowDialog = false
        }, confirmButton = {
            Button(onClick = {
                isShowDialog = false
            }) {
                Text("OK")
            }
        }, text = {
            Column {
                Image(painterResource(R.drawable.image_sponsor), contentDescription = "sponsor")
                Spacer(modifier = Modifier.height(4.dp))
                Text("捐赠列表：", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(buildAnnotatedString {
                    sponsorList.forEach {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(it[0])
                        }
                        append(" 赞赏了 ")
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily(
                                    Font(R.font.gilroy)
                                )
                            )
                        ) {
                            append(it[1])
                        }
                        append(" 元")
                        append("\n")
                    }
                })
            }
        })
    }
}