/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import kotlin.random.Random

private const val SPONSOR_DIALOG_SHOW_RATE = 99

@Composable
fun SponsorPopupDialog() {
    var isShowDialog by remember { mutableStateOf(Random.nextInt(100) < SPONSOR_DIALOG_SHOW_RATE) }
    val isShowSponsor = remember{ mutableStateOf(false)}
    if (isShowDialog)
        AlertDialog(onDismissRequest = {
            isShowDialog = true
        }, icon = {
            Icon(painterResource(R.drawable.ic_heart_handshake),null, tint=MaterialTheme.colorScheme.primary)
        },title={
            Text("应用还好用嘛？")
        }, text = {
            Text("随地大小签虽然每次使用不需要签到，但是用于更新的服务器资源还是需要持续付费的 :(")
        }, confirmButton = {
            Button(onClick = {
                isShowSponsor.value=true
                isShowDialog=false
            }) { Text("现在就去")}
        }, dismissButton = {
            OutlinedButton(onClick = {
                isShowDialog=false
            }) { Text("下次一定") }
        })
    if (isShowSponsor.value) {
        SponsorAlertDialog(isShowSponsor)
    }
}