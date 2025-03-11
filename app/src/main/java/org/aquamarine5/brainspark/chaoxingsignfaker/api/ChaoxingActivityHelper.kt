package org.aquamarine5.brainspark.chaoxingsignfaker.api

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import java.time.Instant

object ChaoxingActivityHelper {
    private const val URL_ACTIVITY_LOAD =
        "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&courseId=%d&classId=%d&showNotStartedActive=0"

    suspend fun getActivities(client: ChaoxingHttpClient, course: ChaoxingCourseEntity) =
        withContext(Dispatchers.IO) {
            val url = URL_ACTIVITY_LOAD.format(course.id, course.classId)
            client.newCall(Request.Builder().get().url(url).build()).execute().use {
                val jsonResult = JSONObject.parseObject(it.body?.string())
                val activeList = jsonResult.getJSONArray("activeList").map { activity ->
                    activity as JSONObject
                }.filter { activity ->
                    activity.getInteger("status") == 2
                }
                return@withContext ChaoxingCourseActivitiesEntity(
                    jsonResult.getJSONObject("ext"),
                    course,
                    List(activeList.size) { i ->
                        val activity = activeList[i]
                        ChaoxingSignActivityEntity(
                            Instant.ofEpochMilli(activity.getLong("startTime")),
                            Instant.ofEpochMilli(activity.getLong("endTime")),
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
                            activity.getString("nameFour")
                        )
                    }
                )
            }
        }
}