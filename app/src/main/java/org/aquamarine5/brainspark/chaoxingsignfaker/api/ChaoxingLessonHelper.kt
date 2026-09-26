/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLesson
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.RecommendActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object ChaoxingLessonHelper {
    private val URL_MY_LESSONS =
        "https://kb.chaoxing.com/pc/curriculum/getMyLessons".toHttpUrl()

    val LESSONS_CACHE_INTERVAL = TimeUnit.DAYS.toMillis(7)

    suspend fun refreshLessons(
        client: ChaoxingHttpRequester,
        context: Context
    ): Result<List<ChaoxingLesson>> = runCatching {
        withContext(Dispatchers.IO) {
            val responseBody = client.newCall(
                Request.Builder().get().url(
                    URL_MY_LESSONS.newBuilder()
                        .addQueryParameter("curTime", System.currentTimeMillis().toString())
                        .build()
                ).build()
            ).execute().use {
                it.checkResponseThrowException()
                it.body.string()
            }
            val jsonResult = JSONObject.parseObject(responseBody)
                ?: throw ChaoxingParseDataException("课表数据解析失败", data = responseBody)
            val data = jsonResult.getJSONObject("data")
                ?: throw ChaoxingParseDataException(
                    "课表数据解析失败",
                    data = jsonResult.toJSONString()
                )
            val curriculum = data.getJSONObject("curriculum")
                ?: throw ChaoxingParseDataException(
                    "课表配置解析失败",
                    data = data.toJSONString()
                )
            val sectionTimes = curriculum.getJSONArray("lessonTimeConfigArray")
                ?.map { it.toString() }
                ?: throw ChaoxingParseDataException(
                    "课表节次时间解析失败",
                    data = curriculum.toJSONString()
                )
            val contentMd5 = run {
                val canonical = data.clone()
                canonical.remove("sysTime")
                md5Hex(canonical.toJSONString())
            }
            val lessonArray = data.getJSONArray("lessonArray")
                ?: throw ChaoxingParseDataException(
                    "课表课程列表解析失败",
                    data = data.toJSONString()
                )
            val parsedLessons = parseLessons(lessonArray, sectionTimes)
            context.chaoxingDataStore.updateData { datastore ->
                val scheduleBuilder = datastore.classSchedule.toBuilder()
                    .setLastFetchTimestamp(System.currentTimeMillis())
                if (contentMd5 == datastore.classSchedule.contentMd5) {
                    return@updateData datastore.toBuilder()
                        .setClassSchedule(scheduleBuilder)
                        .build()
                }
                datastore.toBuilder().setClassSchedule(
                    scheduleBuilder
                        .clearLessons()
                        .addAllLessons(parsedLessons)
                        .setFirstWeekDate(curriculum.getLongValue("firstWeekDate"))
                        .setContentMd5(contentMd5)
                        .build()
                ).build()
            }
            return@withContext parsedLessons
        }
    }

    private fun md5Hex(raw: String): String =
        MessageDigest.getInstance("MD5")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun parseSectionTimeToMinute(sectionTime: String, isStart: Boolean): Int? {
        val time = if (isStart) sectionTime.substringBefore("-") else
            sectionTime.substringAfter("-", "")
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun parseLessons(
        lessonArray: JSONArray,
        sectionTimes: List<String>
    ): List<ChaoxingLesson> = buildList {
        for (i in lessonArray.indices) {
            val lessonItem = lessonArray.getJSONObject(i) ?: continue
            val courseName = lessonItem.getString("name") ?: continue
            val dayOfWeek = lessonItem.getIntValue("dayOfWeek")
            val beginNumber = lessonItem.getIntValue("beginNumber")
            if (dayOfWeek <= 0 || beginNumber <= 0) continue
            val length = lessonItem.getIntValue("length").takeIf { it > 0 } ?: 1
            val endNumber = beginNumber + length - 1
            val startMinuteOfDay = sectionTimes.getOrNull(beginNumber - 1)
                ?.let { parseSectionTimeToMinute(it, true) } ?: continue
            val endMinuteOfDay = sectionTimes.getOrNull(endNumber - 1)
                ?.let { parseSectionTimeToMinute(it, false) } ?: continue
            add(
                ChaoxingLesson.newBuilder()
                    .setCourseId(lessonItem.getLongValue("courseId"))
                    .setClassId(lessonItem.getIntValue("classId"))
                    .setCourseName(courseName)
                    .setTeacherName(lessonItem.getString("teacherName") ?: "")
                    .setLocation(lessonItem.getString("location") ?: "")
                    .setDayOfWeek(dayOfWeek)
                    .setStartMinuteOfDay(startMinuteOfDay)
                    .setEndMinuteOfDay(endMinuteOfDay)
                    .setWeeks(lessonItem.getString("weeks") ?: "")
                    .build()
            )
        }
    }

    fun getCurrentWeekOfSemester(firstWeekDate: Long): Int {
        if (firstWeekDate <= 0) return 1
        val todayStart = LocalDateTime.now().toLocalDate().atStartOfDay()
        val firstWeekStart = Instant.ofEpochMilli(firstWeekDate)
            .atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay()
        val daysBetween = ChronoUnit.DAYS.between(firstWeekStart, todayStart)
        return (daysBetween / 7).toInt() + 1
    }

    suspend fun getCurrentLessons(
        context: Context,
        beforeMinute: Int = 30,
        afterMinute: Int = 30
    ): List<ChaoxingLesson> = withContext(Dispatchers.IO) {
        val datastoreData = context.chaoxingDataStore.data.first().classSchedule
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value
        val currentMinuteOfDay = now.hour * 60 + now.minute
        val currentWeek = getCurrentWeekOfSemester(datastoreData.firstWeekDate)
        datastoreData.lessonsList.filter { lesson ->
            lesson.dayOfWeek == currentDayOfWeek &&
                    lesson.weeks.split(",").map { it.trim() }
                        .contains(currentWeek.toString()) &&
                    lesson.startMinuteOfDay - beforeMinute <= currentMinuteOfDay &&
                    currentMinuteOfDay <= lesson.endMinuteOfDay + afterMinute
        }
    }

    private fun normalizeCourseName(name: String): String {
        val normalized = name
            .map { c ->
                if (c.code in 0xFF01..0xFF5E) (c.code - 0xFEE0).toChar() else c
            }
            .joinToString("")
            .lowercase()
            .filter { !it.isWhitespace() && !"[()（）\\[\\]【】《》<>《》·．._\\-—~、，,]".contains(it) }
        return normalized.replace(Regex("(一|二|三|四|五|六|七|八|九|十|\\d+|i{1,3}|iv|v)+$"), "")
    }

    private fun isCourseNameMatched(
        lessonCourseName: String,
        datastoreCourseName: String
    ): Boolean {
        val lessonName = normalizeCourseName(lessonCourseName)
        val courseName = normalizeCourseName(datastoreCourseName)
        if (lessonName.isEmpty() || courseName.isEmpty()) return false
        if (courseName.contains(lessonName) || lessonName.contains(courseName)) return true
        return courseNameBigramSimilarity(lessonName, courseName) >= 0.5
    }

    private fun courseNameBigramSimilarity(nameA: String, nameB: String): Double {
        if (nameA.length < 2 || nameB.length < 2) return 0.0
        val bigramsA = buildSet {
            for (i in 0 until nameA.length - 1) add(nameA.substring(i, i + 2))
        }
        val bigramsB = buildSet {
            for (i in 0 until nameB.length - 1) add(nameB.substring(i, i + 2))
        }
        val intersection = bigramsA.intersect(bigramsB).size.toDouble()
        return 2 * intersection / (bigramsA.size + bigramsB.size)
    }

    suspend fun checkCurrentLessonSignActivities(
        client: ChaoxingHttpRequester,
        context: Context,
        courses: List<ChaoxingCourseEntity>
    ): List<RecommendActivityEntity> {
        val currentLessons = getCurrentLessons(context)
        if (currentLessons.isEmpty()) return emptyList()
        return buildList {
            currentLessons.forEach { lesson ->
                if (lesson.classId > 0) {
                    ChaoxingActivityHelper.checkCourseHaveAvailableActivity(
                        client,
                        lesson.classId,
                        lesson.courseId
                    )?.let { add(it) }
                    return@forEach
                }
                if (courses.isEmpty()) return@forEach
                courses.filter { course ->
                    isCourseNameMatched(lesson.courseName, course.courseName)
                }.distinctBy { it.classId }.forEach { course ->
                    ChaoxingActivityHelper.checkCourseHaveAvailableActivity(
                        client,
                        course.classId,
                        course.courseId
                    )?.let { add(it) }
                }
            }
        }.distinctBy { it.classId to it.startTime }
    }
}