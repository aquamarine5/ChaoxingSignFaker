package org.aquamarine5.brainspark.chaoxingsignfaker.screens

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingPostLocationEntity

@Serializable
data class LocationSignDestination(val position: ChaoxingPostLocationEntity)

@Composable
fun LocationSignScreen(position: ChaoxingPostLocationEntity) {

}