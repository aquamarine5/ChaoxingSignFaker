package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import java.time.Instant

data class ChaoxingSignActivityEntity(
    val startTime: Instant,
    val endTime:Instant,
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
    val nameFour:String
)
