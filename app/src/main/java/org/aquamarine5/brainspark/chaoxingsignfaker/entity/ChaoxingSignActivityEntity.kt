package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChaoxingSignActivityEntity(
    val startTime: Long,
    val endTime:Long,
    val userStatus:Int,
    val nameTwo:String,
    val otherId:String,
    val source:Int,
    val isLook:Boolean,
    val type:Int,
    val releaseNum:Int,
    val attendNum:Int,
    val activeType:Int,
    val nameOne:String,
    val id:Long,
    val status:Int,
    val nameFour:String,
    val course: ChaoxingCourseEntity,
    val ext:String
){
    object SignActivityNavType:NavType<ChaoxingSignActivityEntity>(false){
        override fun get(bundle: Bundle, key: String): ChaoxingSignActivityEntity? {
            return Json.decodeFromString(bundle.getString(key)?:return null)
        }

        override fun parseValue(value: String): ChaoxingSignActivityEntity {
            return Json.decodeFromString(value)
        }

        override fun put(bundle: Bundle, key: String, value: ChaoxingSignActivityEntity) {
            bundle.putString(key,Json.encodeToString(value))
        }
    }
}
