package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import okhttp3.Cookie

data class ChaoxingLoginSessionEntity(
    val phoneName:Int,
    val password:String,
    val cookies:List<Cookie>?
)
