package org.aquamarine5.brainspark.chaoxingsignfaker.api

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

object ChaoxingCourseHelper {
    private const val URL_COURSE_LIST =
        "http://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&rss=1"

    suspend fun getAllCourse(client: ChaoxingHttpClient): List<ChaoxingCourseEntity> =
        withContext(Dispatchers.IO) {
            val courseList = mutableListOf<ChaoxingCourseEntity>()
            client.newCall(Request.Builder().get().url(URL_COURSE_LIST).build()).execute().use {
                val jsonResult = JSONObject.parseObject(it.body?.string())
                val channelList = jsonResult.getJSONArray("channelList")
                for (i in 0 until channelList.size) {
                    val course = channelList.getJSONObject(i)
                    val content = course.getJSONObject("content")
                    val classContent = content.getJSONArray("clazz").getJSONObject(0)
                    courseList.add(
                        ChaoxingCourseEntity(
                            course.getString("name"),
                            course.getString("teacherfactor"),
                            content.getInteger("id"),
                            course.getInteger("cpi"),
                            content.getString("bbsid"),
                            classContent.getInteger("chatid"),
                            classContent.getInteger("clazzId"),
                            classContent.getString("clazzName")
                        )
                    )
                }
            }
            return@withContext courseList
        }
}