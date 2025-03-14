package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.alibaba.fastjson2.JSONObject
import kotlinx.serialization.Serializable

@Serializable
data class ChaoxingCourseEntity(
    val courseName:String,
    val teacherName:String,
    val courseId:Int,
    val cpi:Int,
    val bbsid:String,
    val chatid:Int,
    val classId:Int,
    val className:String
)
