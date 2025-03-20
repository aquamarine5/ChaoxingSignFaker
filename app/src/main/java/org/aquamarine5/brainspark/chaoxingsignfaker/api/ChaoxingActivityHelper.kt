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
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination

object ChaoxingActivityHelper {
    private const val URL_ACTIVITY_LOAD =
        "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&showNotStartedActive=0"

    const val NO_LIMIT_END_TIME = -1000L

    @Composable
    fun getSignIcon(activity: ChaoxingSignActivityEntity): Painter = when (activity.otherId) {
        "0" -> painterResource(R.drawable.ic_square_mouse_pointer)
        "3" -> painterResource(R.drawable.ic_git_branch)
        "4" -> painterResource(R.drawable.ic_map_pin)
        "2" -> painterResource(R.drawable.ic_scan_qr_code)
        "5" -> painterResource(R.drawable.ic_binary)
        else -> painterResource(R.drawable.ic_clipboard_pen_line)
    }


    fun getSignDestination(context: Context,activityEntity: ChaoxingSignActivityEntity): Any? =
        when (activityEntity.otherId) {
            "4" -> GetLocationDestination.parseFromSignActivityEntity(activityEntity)
            else -> {
                Toast.makeText(context, "暂不支持该活动类型", Toast.LENGTH_SHORT).show()
                null
            }
        }


    suspend fun getActivities(
        client: ChaoxingHttpClient,
        course: ChaoxingCourseEntity
    ): ChaoxingCourseActivitiesEntity =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().get().url(
                    URL_ACTIVITY_LOAD.toHttpUrl().newBuilder()
                        .addQueryParameter("courseId", course.courseId.toString())
                        .addQueryParameter("classId", course.classId.toString())
                        .build()
                ).build()
            ).execute().use {
                val jsonResult = JSONObject.parseObject(it.body?.string()).getJSONObject("data")
                val activeList = jsonResult.getJSONArray("activeList").map { activity ->
                    activity as JSONObject
                }.filter { activity ->
                    activity.getInteger("type") == 2
                }
                return@withContext ChaoxingCourseActivitiesEntity(
                    jsonResult.getJSONObject("ext").toString(),
                    course,
                    List(activeList.size) { i ->
                        val activity = activeList[i]
                        ChaoxingSignActivityEntity(
                            activity.getLong("startTime"),
                            activity.getLong("endTime") ?: NO_LIMIT_END_TIME,
                            activity.getInteger("userStatus"),
                            activity.getString("nameTwo"),
                            activity.getString("otherId"),
                            activity.getInteger("source"),
                            activity.getInteger("isLook") == 1,
                            activity.getInteger("type"),
                            activity.getInteger("releaseNum"),
                            activity.getInteger("attendNum"),
                            activity.getInteger("activeType"),
                            activity.getString("nameOne"),
                            activity.getLong("id"),
                            activity.getInteger("status"),
                            activity.getString("nameFour"),
                            course,
                            jsonResult.getJSONObject("ext").toString()
                        )
                    }
                )
            }
        }
}