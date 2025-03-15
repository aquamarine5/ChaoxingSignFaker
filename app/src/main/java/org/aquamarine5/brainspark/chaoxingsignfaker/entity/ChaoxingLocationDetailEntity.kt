package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.navigation.NavType
import com.baidu.mapapi.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class ChaoxingLocationDetailEntity(
    val latitude: Double,
    val longitude: Double,
    val locationRange: String
) {
    fun toLatLng() = LatLng(latitude, longitude)


}
