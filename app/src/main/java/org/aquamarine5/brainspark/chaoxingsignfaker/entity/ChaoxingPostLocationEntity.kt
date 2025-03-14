package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import kotlinx.serialization.Serializable

@Serializable
data class ChaoxingPostLocationEntity(
    val latitude:Double,
    val longitude:Double,
    val address:String
)
