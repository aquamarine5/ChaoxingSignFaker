/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.alibaba.fastjson2.JSONObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

data class ChaoxingIMGroup(
    val picArray:List<HttpUrl>,
    val chatId:String,
    val chatName:String,
    val isGroup: Boolean
){
    companion object{
        fun fromJson(jsonObject: JSONObject): ChaoxingIMGroup{
            return ChaoxingIMGroup(
                jsonObject.getJSONArray("picArray").map { it.toString().toHttpUrl() },
                jsonObject.getString("chatId"),
                jsonObject.getString("chatName"),
                jsonObject.getInteger("isGroup")==1
            )
        }
    }
}
