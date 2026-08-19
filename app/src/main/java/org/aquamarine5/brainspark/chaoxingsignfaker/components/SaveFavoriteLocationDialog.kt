/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import org.aquamarine5.brainspark.chaoxingsignfaker.components.SnackbarAlertDialog

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

@Composable
fun SaveFavoriteLocationDialog(
    location: ChaoxingLocationSignEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    SnackbarAlertDialog(
        onDismissRequest = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
            onDismiss()
        },
        text = { _ ->

        },
        confirmButton = {
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                onDismiss()
            }) {
                Text("关闭")
            }
        },
        title = { _ ->
            Text("是否收藏刚才的签到位置？")
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder().setPreferences(
                                it.preferences.toBuilder().setNeverAskSaveFavoriteLocation(true)
                            ).build()
                        }
                    }
                }
            ) { }
        }
    )
}