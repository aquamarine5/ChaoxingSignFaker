package org.aquamarine5.brainspark.chaoxingsignfaker.entity

data class ChaoxingLocationDetailEntity(
    val latitude: Double?,
    val longitude: Double?,
    val locationRange: Int
){
    fun isAvailable(): Boolean {
        return latitude != null && longitude != null
    }
}