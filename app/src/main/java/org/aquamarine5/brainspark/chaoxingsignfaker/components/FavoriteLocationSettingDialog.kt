/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.TitleOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_BUNDLE_ADDRESS
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.MARKER_BUNDLE_LABEL
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.markerTitleOptions

@Composable
fun FavoriteLocationSettingDialog(
    favoriteLocations: SnapshotStateList<ChaoxingLocation>,
    onDismiss: () -> Unit,
    onSelectLocation: (ChaoxingLocation) -> Unit = {},
    onDeleteLocation: (ChaoxingLocation) -> Unit = {},
    favoriteLocationMarkers: MutableList<Marker> = mutableListOf(),
    selectedLocation: ChaoxingLocation? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var selected by remember(selectedLocation) { mutableStateOf(selectedLocation) }
    var editingLocation by remember { mutableStateOf<ChaoxingLocation?>(null) }
    var editLabel by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    SnackbarAlertDialog(
        onDismissRequest = onDismiss,
        title = { _ ->
            Text("管理收藏的签到位置")
        },
        text = { _ ->
            if (favoriteLocations.isEmpty()) {
                Text("还没有收藏任何签到位置，在地图上点击选择位置后即可收藏。")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    favoriteLocations.forEach { location ->
                        val onSelect = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            selected = location
                            onSelectLocation(location)
                            onDismiss()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onSelect)
                                .padding(0.dp, 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == location,
                                onClick = onSelect
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    location.label,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    location.address,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "(%.4f, %.4f)".format(location.latitude, location.longitude),
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    editingLocation = location
                                    editLabel = location.label
                                    editAddress = location.address
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_edit),
                                    contentDescription = "编辑收藏位置",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        },
        icon = {
            Icon(
                painterResource(R.drawable.ic_map_pinned), null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
    editingLocation?.let { target ->
        SnackbarAlertDialog(
            onDismissRequest = {
                editingLocation = null
            },
            title = { _ ->
                Text("编辑收藏位置")
            },
            text = { _ ->
                Column {
                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("位置标签") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("位置描述") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "位置描述会被上传到学习通作为教师端的用户签到位置描述信息，而位置标签仅用于本应用的收藏位置名称显示。",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "(%.4f, %.4f)".format(target.latitude, target.longitude),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        updateFavoriteLocation(
                            context,
                            coroutineScope,
                            target,
                            editLabel.trim(),
                            editAddress.trim(),
                            favoriteLocations,
                            favoriteLocationMarkers
                        )
                        if (selected == target) {
                            selected =
                                favoriteLocations.find {
                                    it.latitude == target.latitude && it.longitude == target.longitude
                                }
                        }
                        editingLocation = null
                    },
                    enabled = editLabel.isNotBlank() && editAddress.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onDeleteLocation(target)
                        if (selected == target) selected = null
                        editingLocation = null
                    }) {
                        Text("删除", color = Color.Red)
                    }
                    FilledTonalButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        editingLocation = null
                    }) {
                        Text("取消")
                    }
                }
            },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_edit), null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

fun updateFavoriteLocation(
    context: Context,
    coroutineScope: CoroutineScope,
    target: ChaoxingLocation,
    newLabel: String,
    newAddress: String,
    favoriteLocations: SnapshotStateList<ChaoxingLocation>,
    favoriteLocationMarkers: MutableList<Marker>
) {
    val updated = target.toBuilder().setLabel(newLabel).setAddress(newAddress).build()
    coroutineScope.launch(Dispatchers.IO) {
        context.chaoxingDataStore.updateData { data ->
            data.locationsList.indexOfFirst {
                it.latitude == target.latitude && it.longitude == target.longitude
            }.takeIf { it >= 0 }?.let { index ->
                data.toBuilder().setLocations(index, updated).build()
            } ?: data
        }
    }
    favoriteLocations.indexOfFirst {
        it.latitude == target.latitude && it.longitude == target.longitude
    }.takeIf { it >= 0 }?.let { index ->
        favoriteLocations[index] = updated
    }
    favoriteLocationMarkers.firstOrNull { marker ->
        marker.position.latitude == target.latitude &&
                marker.position.longitude == target.longitude
    }?.let { marker ->
        marker.extraInfo.putString(MARKER_BUNDLE_LABEL, newLabel)
        marker.extraInfo.putString(MARKER_BUNDLE_ADDRESS, newAddress)
        marker.titleOptions = markerTitleOptions(newLabel)
    }
}

fun removeFavoriteLocation(
    context: Context,
    coroutineScope: CoroutineScope,
    target: ChaoxingLocation,
    favoriteLocations: MutableList<ChaoxingLocation>,
    favoriteLocationMarkers: MutableList<Marker>
) {
    coroutineScope.launch(Dispatchers.IO) {
        context.chaoxingDataStore.updateData { data ->
            data.locationsList.indexOfFirst {
                it.latitude == target.latitude && it.longitude == target.longitude
            }.takeIf { it >= 0 }?.let { index ->
                data.toBuilder().removeLocations(index).build()
            } ?: data
        }
    }
    favoriteLocations.removeAll {
        it.latitude == target.latitude && it.longitude == target.longitude
    }
    favoriteLocationMarkers.removeAll { marker ->
        (marker.position.latitude == target.latitude &&
                marker.position.longitude == target.longitude).also { match ->
            if (match) marker.remove()
        }
    }
}
