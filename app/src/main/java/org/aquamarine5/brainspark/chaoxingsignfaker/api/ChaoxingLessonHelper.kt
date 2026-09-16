/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLesson
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.RecommendActivityEntity
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

    private fun getIntegerOrNull(jsonObject: JSONObject, vararg keys: String): Int? {
        keys.forEach { key ->
            runCatching { jsonObject.getInteger(key) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun getLongOrNull(jsonObject: JSONObject, vararg keys: String): Long? {
        keys.forEach { key ->
            runCatching { jsonObject.getLong(key) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun getStringOrNull(jsonObject: JSONObject, vararg keys: String): String? {
        keys.forEach { key ->
            jsonObject.getString(key)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    suspend fun refreshLessons(
        client: ChaoxingHttpClient,
        context: Context
    ): List<ChaoxingLesson> = withContext(Dispatchers.IO) {
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
        val jsonResult = runCatching { JSONObject.parseObject(responseBody) }.getOrNull()
        val data = jsonResult?.getJSONObject("data")
        val curriculum = data?.getJSONObject("curriculum")
        val sectionTimes = curriculum?.getJSONArray("lessonTimeConfigArray")
            ?.map { it.toString() } ?: emptyList()
        val contentMd5 = data?.let {
            val canonical = it.clone() as JSONObject
            canonical.remove("sysTime")
            md5Hex(canonical.toJSONString())
        }
        val parsedLessons =
            parseLessons(data?.getJSONArray("lessonArray"), sectionTimes)
        context.chaoxingDataStore.updateData { datastore ->
            val scheduleBuilder = datastore.classSchedule.toBuilder()
                .setLastFetchTimestamp(System.currentTimeMillis())
            if (contentMd5 != null && contentMd5 == datastore.classSchedule.contentMd5) {
                return@updateData datastore.toBuilder()
                    .setClassSchedule(scheduleBuilder)
                    .build()
            }
            datastore.toBuilder().setClassSchedule(
                scheduleBuilder
                    .clearLessons()
                    .addAllLessons(parsedLessons)
                    .setFirstWeekDate(curriculum?.getLongValue("firstWeekDate") ?: 0L)
                    .setContentMd5(contentMd5 ?: "")
                    .build()
            ).build()
        }
        return@withContext parsedLessons
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
        lessonArray: List<Any>?,
        sectionTimes: List<String>
    ): List<ChaoxingLesson> {
        if (lessonArray == null) return emptyList()
        return buildList {
            for (rawItem in lessonArray) {
                val lessonItem = (rawItem as? JSONObject) ?: continue
                val courseName =
                    getStringOrNull(lessonItem, "name", "nameOne", "courseName") ?: continue
                val dayOfWeek = getIntegerOrNull(lessonItem, "dayOfWeek", "week") ?: continue
                val beginNumber = getIntegerOrNull(lessonItem, "beginNumber") ?: continue
                val length = getIntegerOrNull(lessonItem, "length") ?: 1
                val endNumber = beginNumber + length - 1
                val startMinuteOfDay = sectionTimes.getOrNull(beginNumber - 1)
                    ?.let { parseSectionTimeToMinute(it, true) } ?: continue
                val endMinuteOfDay = sectionTimes.getOrNull(endNumber - 1)
                    ?.let { parseSectionTimeToMinute(it, false) } ?: continue
                val classId = getIntegerOrNull(lessonItem, "classId") ?: 0
                val courseId = getLongOrNull(lessonItem, "courseId") ?: 0L
                add(
                    ChaoxingLesson.newBuilder()
                        .setCourseId(courseId)
                        .setClassId(classId)
                        .setCourseName(courseName)
                        .setTeacherName(getStringOrNull(lessonItem, "teacherName") ?: "")
                        .setLocation(getStringOrNull(lessonItem, "location") ?: "")
                        .setDayOfWeek(dayOfWeek)
                        .setStartMinuteOfDay(startMinuteOfDay)
                        .setEndMinuteOfDay(endMinuteOfDay)
                        .setWeeks(getStringOrNull(lessonItem, "weeks") ?: "")
                        .build()
                )
            }
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
        client: ChaoxingHttpClient,
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