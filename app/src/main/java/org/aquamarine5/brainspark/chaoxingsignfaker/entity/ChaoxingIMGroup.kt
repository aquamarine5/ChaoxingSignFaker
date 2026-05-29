/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.alibaba.fastjson2.JSONObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingParseDataException

@Serializable
data class ChaoxingIMGroup(
    val picArray: List<String>,
    val chatId: String,
    val chatName: String,
    val isGroup: Boolean
) {
    object ChaoxingIMGroupNavType : NavType<ChaoxingIMGroup>(false) {
        override fun get(bundle: Bundle, key: String): ChaoxingIMGroup? {
            return Json.decodeFromString(bundle.getString(key) ?: return null)
        }

        override fun parseValue(value: String): ChaoxingIMGroup {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun serializeAsValue(value: ChaoxingIMGroup): String {
            return Uri.encode(Json.encodeToString(value))
        }

        override fun put(bundle: Bundle, key: String, value: ChaoxingIMGroup) {
            bundle.putString(key, Json.encodeToString(value))
        }
    }

    companion object {
        fun fromJson(jsonObject: JSONObject): ChaoxingIMGroup {
            return ChaoxingIMGroup(
                jsonObject.getJSONArray("picArray")
                    ?.let { array -> array.mapNotNull { it?.toString() } }
                    ?: listOf(
                        jsonObject.getString("chatIco")
                            ?: "https://im.chaoxing.com/res/images/course_logo.png"
                    ),
                jsonObject.getString("chatId") ?: throw ChaoxingParseDataException(
                    "chatId is null",
                    data = jsonObject.toJSONString()
                ),
                jsonObject.getString("chatName") ?: "无群聊名称",
                jsonObject.getInteger("isGroup") == 0
            )
        }
    }
}
