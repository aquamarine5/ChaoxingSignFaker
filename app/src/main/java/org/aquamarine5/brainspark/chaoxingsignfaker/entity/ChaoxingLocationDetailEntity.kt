package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.baidu.mapapi.model.LatLng

data class ChaoxingLocationDetailEntity(
    val latitude: Double,
    val longitude: Double,
    val locationRange: String
) {
    fun toLatLng() = LatLng(latitude, longitude)
}