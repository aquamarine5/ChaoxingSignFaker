/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.components.QRCodeScanDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner.Companion.URL_SIGN_INFO

object ChaoxingSignHelper {
    suspend fun getSignInfo(activeId: Long): JSONObject = withContext(Dispatchers.IO) {
        ChaoxingHttpClient.instance!!.newCall(
            Request.Builder().get().url(
                URL_SIGN_INFO.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .build()
            ).build()
        ).execute().use {
            return@withContext JSONObject.parseObject(it.body?.string()).getJSONObject("data")
        }
    }

    @Composable
    fun getSignIcon(activity: ChaoxingSignActivityEntity): Painter = when (activity.otherId) {
        "0" -> painterResource(R.drawable.ic_square_mouse_pointer)
        "3" -> painterResource(R.drawable.ic_git_branch)
        "4" -> painterResource(R.drawable.ic_map_pin)
        "2" -> painterResource(R.drawable.ic_scan_qr_code)
        "5" -> painterResource(R.drawable.ic_binary)
        else -> painterResource(R.drawable.ic_clipboard_pen_line)
    }


    fun getSignDestination(context: Context, activityEntity: ChaoxingSignActivityEntity): Any? =
        when (activityEntity.otherId) {
            "4" -> GetLocationDestination.parseFromSignActivityEntity(activityEntity)
            "2" -> QRCodeScanDestination
            else -> {
                Toast.makeText(context, "暂不支持该活动类型", Toast.LENGTH_SHORT).show()
                null
            }
        }


}