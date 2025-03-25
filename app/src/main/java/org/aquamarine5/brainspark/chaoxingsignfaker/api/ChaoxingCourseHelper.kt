/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

object ChaoxingCourseHelper {
    private const val URL_COURSE_LIST =
        "https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&rss=1"

    suspend fun getAllCourse(client: ChaoxingHttpClient): List<ChaoxingCourseEntity> =
        withContext(Dispatchers.IO) {
            val courseList = mutableListOf<ChaoxingCourseEntity>()
            client.newCall(Request.Builder().get().url(URL_COURSE_LIST).build()).execute().use {
                val jsonResult = JSONObject.parseObject(it.body?.string())
                val channelList = jsonResult.getJSONArray("channelList")
                for (i in 0 until channelList.size) {
                    val course = channelList.getJSONObject(i)
                    val content = course.getJSONObject("content")
                    if (!content.containsKey("course")) continue
                    if (!course.containsKey("cataName")) continue
                    val courseContent =
                        content.getJSONObject("course").getJSONArray("data").getJSONObject(0)
                    courseList.add(
                        ChaoxingCourseEntity(
                            courseContent.getString("name"),
                            courseContent.getString("teacherfactor"),
                            courseContent.getInteger("id"),
                            content.getInteger("id"),
                            courseContent.getString("name"),
                            courseContent.getString("imageurl"),
                            courseContent.getString("schools")
                        )
                    )
                }
            }
            return@withContext courseList
        }
}