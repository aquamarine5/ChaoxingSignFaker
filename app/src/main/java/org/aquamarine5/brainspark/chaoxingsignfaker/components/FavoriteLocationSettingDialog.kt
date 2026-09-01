/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baidu.mapapi.map.Marker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLocation
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

@Composable
fun FavoriteLocationSettingDialog(
    favoriteLocations: List<ChaoxingLocation>,
    onDismiss: () -> Unit,
    onSelectLocation: (ChaoxingLocation) -> Unit = {},
    onDeleteLocation: (ChaoxingLocation) -> Unit = {}
) {
    SnackbarAlertDialog(
        onDismissRequest = onDismiss,
        title = { _ ->
            Text("管理收藏的签到位置")
        },
        text = { _ ->
            if (favoriteLocations.isEmpty()) {
                Text("还没有收藏任何签到位置，在地图上点击选择位置后即可收藏。")
            } else {
                Column {
                    favoriteLocations.forEach { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLocation(location)
                                    onDismiss()
                                }
                                .padding(0.dp, 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "(%.4f, %.4f)".format(location.latitude, location.longitude),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                onDeleteLocation(location)
                            }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = "删除",
                                    tint = Color.Red
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
