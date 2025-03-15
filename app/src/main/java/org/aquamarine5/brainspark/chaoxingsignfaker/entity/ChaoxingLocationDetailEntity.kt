package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.baidu.mapapi.model.LatLng
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSignHelper

@Serializable
data class ChaoxingLocationDetailEntity(
    val latitude: Double,
    val longitude: Double,
    val locationRange: String
) : ChaoxingSignHelper.SignerDestination {
    fun isShow() = latitude > 0 && longitude > 0
    fun toLatLng() = LatLng(latitude, longitude)
}
