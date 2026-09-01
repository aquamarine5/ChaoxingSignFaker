/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

const val DONT_SAVE_NEARBY_DISTANCE = 500.0

fun ChaoxingLocationSignEntity.toChaoxingLocation(label: String = address): ChaoxingLocation {
    return ChaoxingLocation.newBuilder()
        .setAddress(address)
        .setLabel(label)
        .setLatitude(latitude)
        .setLongitude(longitude)
        .build()
}

@Composable
fun SaveFavoriteLocationDialog(
    location: ChaoxingLocationSignEntity,
    onDismiss: () -> Unit,
    onSaveToFavorite: () -> Unit = {}
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
            Column {
                Text(
                    "位置：${location.address}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "经度: ${"%.5f".format(location.longitude)}，纬度: ${
                        "%.5f".format(location.latitude)
                    }",
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                coroutineScope.launch(Dispatchers.IO) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().addLocations(location.toChaoxingLocation()).build()
                    }
                }
                onSaveToFavorite()
                onDismiss()
            }) {
                Text("是")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch(Dispatchers.IO) {
                        context.chaoxingDataStore.updateData {
                            it.toBuilder()
                                .addDontSaveNearbyPosition(location.toChaoxingLocation())
                                .build()
                        }
                    }
                    onDismiss()
                }) {
                    Text("此地点附近不再提醒")
                }
                FilledTonalButton(onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onDismiss()
                }) {
                    Text("否")
                }
            }
        },
        title = {
            Text("是否收藏刚才的签到位置？")
        }
    )
}
