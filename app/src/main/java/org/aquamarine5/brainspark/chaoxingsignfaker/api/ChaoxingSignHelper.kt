/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

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
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

object ChaoxingSignHelper {
    const val TIMEOUT_SHOW_SPONSOR_AFTER_ALL_SIGNED = 250L

    class ChaoxingUnsupportedSignTypeException : ChaoxingPredictableException("不支持此签到类型")

    @Composable
    fun getSignIcon(activity: ChaoxingSignActivityEntity): Painter = when (activity.otherId) {
        "0" -> painterResource(R.drawable.ic_square_mouse_pointer)
        "3" -> painterResource(R.drawable.ic_git_branch)
        "4" -> painterResource(R.drawable.ic_map_pin)
        "2" -> painterResource(R.drawable.ic_scan_qr_code)
        "5" -> painterResource(R.drawable.ic_binary)
        else -> painterResource(R.drawable.ic_clipboard_pen_line)
    }

    fun getSignDestination(
        context: Context,
        activityEntity: ChaoxingSignActivityEntity,
        isLate: Boolean = false
    ): Any? =
        when (activityEntity.otherId) {
            "4" -> GetLocationDestination.parseFromSignActivityEntity(activityEntity, isLate)
            "2" -> QRCodeSignDestination.parseFromSignActivityEntity(activityEntity, isLate)
            "0" -> PhotoSignDestination.parseFromSignActivityEntity(activityEntity, isLate)
            else -> {
                Toast.makeText(context, "暂不支持该活动类型", Toast.LENGTH_SHORT).show()
                null
            }
        }

    suspend fun getRedirectDestination(
        activeId: Long,
        classId: Int,
        courseId: Int,
        context: Context
    ): Any =
        withContext(Dispatchers.IO) {
            ChaoxingHttpClient.instance!!.newCall(
                Request.Builder().get().url(
                    ChaoxingSigner.URL_SIGN_INFO.toHttpUrl().newBuilder()
                        .addQueryParameter("activeId", activeId.toString())
                        .build()
                ).build()
            ).execute().use {
                if (it.checkResponse(context)) {
                    throw ChaoxingHttpClient.ChaoxingNetworkException()
                }
                val result = JSONObject.parseObject(it.body.string()).getJSONObject("data")
                val endTime = result.getLong("endTime")
                when (result.getInteger("otherId")) {
                    0 -> PhotoSignDestination(
                        activeId,
                        classId,
                        courseId,
                        "",
                        result.getLong("starttime"),
                        endTime,
                        if (endTime != null) System.currentTimeMillis() > endTime else false
                    )

                    2 -> QRCodeSignDestination(
                        activeId,
                        classId,
                        courseId,
                        "",
                        result.getLong("starttime"),
                        endTime,
                        if (endTime != null) System.currentTimeMillis() > endTime else false
                    )

                    4 -> GetLocationDestination(
                        activeId,
                        classId,
                        courseId,
                        "",
                        result.getLong("starttime"),
                        endTime,
                        if (endTime != null) System.currentTimeMillis() > endTime else false
                    )

                    else -> {
                        throw ChaoxingUnsupportedSignTypeException()
                    }
                }
            }
        }
}