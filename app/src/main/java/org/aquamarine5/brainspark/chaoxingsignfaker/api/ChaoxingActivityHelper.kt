/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

object ChaoxingActivityHelper {
    enum class SignRedirectStatus {
        COMMON,
        SIGN_IN_PUBLISHED,
        SIGN_OUT,
        SIGN_IN_UNPUBLISHED
    }

    private const val URL_ACTIVITY_LOAD =
        "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&showNotStartedActive=0"

    private const val NO_LIMIT_END_TIME = -1000L

    const val NO_SIGN_OFF_EVENT = 4999L

    suspend fun getActivities(
        client: ChaoxingHttpClient,
        course: ChaoxingCourseEntity,
        context: Context,
        snackbarHostState: SnackbarHostState
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
                if (it.checkResponse(snackbarHostState))
                    throw ChaoxingHttpClient.ChaoxingNetworkException()
                val jsonResult = JSONObject.parseObject(it.body.string()).getJSONObject("data")
                val activeList = jsonResult.getJSONArray("activeList").map { activity ->
                    activity as JSONObject
                }.filter { activity ->
                    activity.getInteger("type") == 2 || activity.getInteger("type") == 74
                }
                return@withContext ChaoxingCourseActivitiesEntity(
                    jsonResult.getJSONObject("ext").toString(),
                    course,
                    List(activeList.size) { i ->
                        val activity = activeList[i]
                        ChaoxingSignActivityEntity(
                            activity.getLong("startTime"),
                            activity.getLong("endTime"),
                            activity.getInteger("userStatus"),
                            activity.getString("otherId"),
                            activity.getInteger("isLook") == 1,
                            activity.getInteger("type"),
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