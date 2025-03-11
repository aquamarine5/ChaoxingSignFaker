package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.alibaba.fastjson2.JSONObject

data class ChaoxingCourseActivitiesEntity(
    val ext: JSONObject,
    val course: ChaoxingCourseEntity,
    val signActivities: List<ChaoxingSignActivityEntity>
)