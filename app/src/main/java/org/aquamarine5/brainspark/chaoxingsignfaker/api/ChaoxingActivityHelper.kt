package org.aquamarine5.brainspark.chaoxingsignfaker.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseActivitiesEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import java.time.Instant

object ChaoxingActivityHelper {
    private const val URL_ACTIVITY_LOAD =
        "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist?fid=0&courseId=%d&classId=%d&showNotStartedActive=0"

    @Composable
    fun getSignIcon(activity: ChaoxingSignActivityEntity) :Painter{
        val iconMatchList=
            mapOf(
                "0" to painterResource(R.drawable.ic_square_mouse_pointer),
                "3" to painterResource(R.drawable.ic_git_branch),
                "4" to painterResource(R.drawable.ic_map_pin),
                "2" to painterResource(R.drawable.ic_scan_qr_code),
                "5" to painterResource(R.drawable.ic_binary)
            )

        return iconMatchList[activity.activeType.toString()]?:painterResource(R.drawable.ic_clipboard_pen_line)
    }

    suspend fun getActivities(
        client: ChaoxingHttpClient,
        course: ChaoxingCourseEntity
    ): ChaoxingCourseActivitiesEntity =
        withContext(Dispatchers.IO) {
            val url = URL_ACTIVITY_LOAD.format(course.courseId, course.classId)
            client.newCall(Request.Builder().get().url(url).build()).execute().use {
                val jsonResult = JSONObject.parseObject(it.body?.string())
                val activeList = jsonResult.getJSONArray("activeList").map { activity ->
                    activity as JSONObject
                }.filter { activity ->
                    activity.getInteger("type") == 2
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
                            activity.getString("nameFour"),
                            course,
                            jsonResult.getJSONObject("ext")
                        )
                    }
                )
            }
        }
}