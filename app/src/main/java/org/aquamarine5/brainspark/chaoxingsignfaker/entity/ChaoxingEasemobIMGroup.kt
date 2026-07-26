/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Immutable
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Immutable
@Serializable
data class ChaoxingEasemobIMGroup(
    val chatName: String,
    val id: String,
    val imageUrl: String?
){
    object ChaoxingEasemobIMGroupNavType : NavType<ChaoxingEasemobIMGroup>(false) {
        override fun get(bundle: Bundle, key: String): ChaoxingEasemobIMGroup? {
            return Json.decodeFromString(bundle.getString(key) ?: return null)
        }

        override fun parseValue(value: String): ChaoxingEasemobIMGroup {
            return Json.decodeFromString(Uri.decode(value))
        }

        override fun serializeAsValue(value: ChaoxingEasemobIMGroup): String {
            return Uri.encode(Json.encodeToString(value))
        }

        override fun put(bundle: Bundle, key: String, value: ChaoxingEasemobIMGroup) {
            bundle.putString(key, Json.encodeToString(value))
        }
    }
}