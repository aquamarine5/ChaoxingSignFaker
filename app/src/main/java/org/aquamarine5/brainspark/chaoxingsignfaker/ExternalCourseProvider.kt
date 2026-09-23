/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingCourseHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable

class ExternalCourseProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "org.aquamarine5.brainspark.chaoxingsignfaker.courses"
        private const val METHOD_GET_COURSES = "getCourses"
        private const val KEY_JSON = "json"
        private const val KEY_ERROR = "error"
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val result = Bundle()
        if (method != METHOD_GET_COURSES) {
            result.putString(KEY_ERROR, "未知方法: $method")
            return result
        }
        val context = context
        if (context == null) {
            result.putString(KEY_ERROR, "上下文不可用")
            return result
        }
        runCatching {
            val client = ChaoxingHttpClient.instance ?: runBlocking {
                val datastore = context.chaoxingDataStore.data.first()
                requirePredictable(datastore.agreeTerms && datastore.hasLoginSession()) {
                    "请先打开 ChaoxingSignFaker 登录学习通"
                }
                ChaoxingHttpClient.loadFromDataStore(datastore, context)
            }
            val courses = runBlocking { ChaoxingCourseHelper.getAllCourse(client) }
            val array = JSONArray()
            courses.forEach { course ->
                array.add(
                    JSONObject().apply {
                        put("name", course.courseName)
                        put("teacher", course.teacherName ?: "")
                        put("classId", course.classId)
                        put("courseId", course.courseId)
                    }
                )
            }
            val json = JSONObject().apply {
                put("fid", client.configuredFid)
                put("courses", array)
            }.toJSONString()
            result.putString(KEY_JSON, json)
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            result.putString(KEY_ERROR, throwable.message ?: "获取课程失败")
        }
        return result
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
