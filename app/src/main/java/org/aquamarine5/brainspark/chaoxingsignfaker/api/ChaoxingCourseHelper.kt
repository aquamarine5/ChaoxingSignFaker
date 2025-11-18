/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.widget.Toast
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

object ChaoxingCourseHelper {
    private const val URL_COURSE_LIST =
        "https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&rss=1"

    private const val URL_COURSE_QUERY_NAME =
        "https://mooc1-api.chaoxing.com/gas/clazz?fields=name&view=json"

    suspend fun queryClassName(client: ChaoxingHttpClient, classId: Int): String = withContext(
        Dispatchers.IO
    ) {
        client.newCall(
            Request.Builder().get().url(
                URL_COURSE_QUERY_NAME.toHttpUrl().newBuilder()
                    .addQueryParameter("id", classId.toString()).build()
            ).build()
        ).execute().use {
            return@use JSONObject.parseObject(it.body.string()).getJSONArray("data")
                .getJSONObject(0).getString("name")
        }
    }

    suspend fun checkClassValid(
        client: ChaoxingHttpClient,
        classId: Int,
    ): Boolean? = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().get().url(URL_COURSE_LIST).build()).execute().use {
            val jsonResult = JSONObject.parseObject(it.body.string())
            val channelList = jsonResult.getJSONArray("channelList")
            if (jsonResult.getInteger("result") == 0 || channelList == null) {
                return@use null
            }
            for (i in 0 until channelList.size) {
                if (channelList.getJSONObject(i).getJSONObject("content")
                        .getInteger("id") == classId
                )
                    return@use true
            }
            return@use false
        }
    }

    suspend fun getAllCourse(
        client: ChaoxingHttpClient,
        context: Context,
        naviToLogin: () -> Unit
    ): List<ChaoxingCourseEntity> =
        withContext(Dispatchers.IO) {
            val courseList = mutableListOf<ChaoxingCourseEntity>()
            client.newCall(Request.Builder().get().url(URL_COURSE_LIST).build()).execute()
                .use { rawResponse ->
                    if (rawResponse.checkResponse(context)) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "网络异常，请重新登录",
                                Toast.LENGTH_SHORT
                            ).show()
                            naviToLogin()
                        }
                        return@withContext emptyList()
                    }
                    var jsonResult = JSONObject.parseObject(rawResponse.body.string())
                    var channelList = jsonResult.getJSONArray("channelList")
                    if (jsonResult.getInteger("result") == 0 || channelList == null) {
                        if (client.reLogin(context).not()) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "登录信息已过期，请重新登录",
                                    Toast.LENGTH_SHORT
                                ).show()
                                naviToLogin()
                            }
                            return@withContext emptyList()
                        } else {
                            client.newCall(Request.Builder().get().url(URL_COURSE_LIST).build())
                                .execute().use {
                                    if (it.checkResponse(context)) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "网络异常，请重新登录",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            naviToLogin()
                                        }
                                        return@withContext emptyList()
                                    }
                                    jsonResult = JSONObject.parseObject(it.body.string())
                                    channelList = jsonResult.getJSONArray("channelList")
                                    if (jsonResult.getInteger("result") == 0 || channelList == null) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "登录信息已过期，请重新登录",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            naviToLogin()
                                        }
                                    }
                                }
                        }
                    }

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
                                courseContent.getString("imageurl")
                                    ?: "https://p.ananas.chaoxing.com/star3/270_160c/669ca80d6a0c5f74835bb936a41aabca.jpg",
                                courseContent.getString("schools")
                            )
                        )
                    }
                }
            return@withContext courseList
        }
}