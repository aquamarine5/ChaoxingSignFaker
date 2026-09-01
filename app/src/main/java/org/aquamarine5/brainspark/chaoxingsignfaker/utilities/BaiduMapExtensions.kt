/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.BitmapDescriptor
import com.baidu.mapapi.map.BitmapDescriptorFactory
import com.baidu.mapapi.map.Marker
import com.baidu.mapapi.map.MarkerOptions
import com.baidu.mapapi.map.TitleOptions
import com.baidu.mapapi.model.LatLng
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLocation

fun BaiduMap.addOrUpdateLocationMarker(
    marker: Marker?,
    position: LatLng,
    icon: BitmapDescriptor
): Marker {
    return if (marker == null) {
        addOverlay(
            MarkerOptions()
                .position(position)
                .icon(icon)
                .draggable(true)
                .extraInfo(Bundle().apply {
                    putString(MARKER_BUNDLE_TYPE, MarkerBundleType.LOCATION.value)
                })
        ) as Marker
    } else {
        marker.position = position
        marker
    }
}

fun BaiduMap.addFavoriteLocationMarker(
    location: ChaoxingLocation,
    starBitmap: BitmapDescriptor
): Marker {
    return addOverlay(
        MarkerOptions()
            .position(LatLng(location.latitude, location.longitude))
            .icon(starBitmap)
            .titleOptions(TitleOptions().text(location.label))
            .extraInfo(Bundle().apply {
                putString(MARKER_BUNDLE_TYPE, MarkerBundleType.FAVORITE.value)
                putString(MARKER_BUNDLE_LABEL, location.label)
                putString(MARKER_BUNDLE_ADDRESS, location.address)
            })
    ) as Marker
}

fun Context.createBitmapDescriptorFromVector(
    @DrawableRes resourceId: Int,
    backgroundColor: Int? = null,
    size: Int = 48
): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    backgroundColor?.let { color ->
        canvas.drawCircle(
            size / 2f, size / 2f, size / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        )
    }
    ContextCompat.getDrawable(this, resourceId)?.let { drawable ->
        val innerSize = (size * 0.6f).toInt()
        val offset = (size - innerSize) / 2
        drawable.setBounds(offset, offset, offset + innerSize, offset + innerSize)
        drawable.draw(canvas)
    }
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
