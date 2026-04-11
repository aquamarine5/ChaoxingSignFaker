/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.util.Base64
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.MessageBody
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.Meta
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingGroupSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMGroup

object ChaoxingIMHelper {
    private class ChaoxingIMConfigParseException(arg: String, message: String? = null) :
        Exception("IM配置解析异常: $arg 获取失败，${message ?: "未知错误"}")

    val URL_IM_ME = "https://im.chaoxing.com/webim/me".toHttpUrl()
    val URL_IM_GROUPS = "https://im.chaoxing.com/webim/message/list/getMessageList".toHttpUrl()

    const val URL_MESSAGE_ROAMING =
        "https://a1-vip6.easecdn.com/cx-dev/cxstudy/users/%s/messageroaming"
    const val IM_APPKEY = "cx-dev#cxstudy"

    suspend fun getIMGroups(
        httpClient: ChaoxingHttpClient,
        config: ChaoxingIMConfig
    ): List<ChaoxingIMGroup> {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder().url(URL_IM_GROUPS).post(
                    FormBody.Builder()
                        .add("tuid", config.imTuid)
                        .add("puid", config.imPuid)
                        .add("token", config.imToken)
                        .build()
                )
                    .header("Accept", "application/json, text/javascript, */*; q=0.01")
                    .build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                val responseBody = response.body.string()
                val json = JSONObject.parseObject(responseBody)
                val data = json.getJSONArray("data")
                return@use data.map { ChaoxingIMGroup.fromJson(it as JSONObject) }
            }
        }
    }

    suspend fun getIMConfig(httpClient: ChaoxingHttpClient): ChaoxingIMConfig {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(Request.Builder().url(URL_IM_ME).build()).execute().use { response ->
                val responseBody = response.body.string()

                fun extractSpan(id: String): String {
                    runCatching {
                        return """<span\s+id="$id"[^>]*>(.*?)</span>""".toRegex(RegexOption.DOT_MATCHES_ALL)
                            .find(responseBody)?.groupValues?.get(1)?.trim()!!
                    }.getOrElse { throw ChaoxingIMConfigParseException(id, it.message) }
                }

                val tuid = extractSpan("myTuid")
//                val img = extractSpan("myImg")
                val name = extractSpan("myName")
                val token = extractSpan("myToken")
                val puid = extractSpan("myPuid")
                val fid = extractSpan("myFid")
                return@use ChaoxingIMConfig("", name, token, tuid, puid, fid)
            }
        }
    }

    fun parseIMMessageBody(imMessages: List<MessageBody>) {
        val signActivities = mutableListOf<ChaoxingGroupSignActivityEntity>()
        imMessages.forEach {
            it.extList?.forEach { ext ->
                if (ext.key == "attachment") {
                    val attachObject = JSONObject.parseObject(ext.stringValue)
                    if (attachObject.getInteger("attachmentType") == 15) {
                        val signInfo = attachObject.getJSONObject("att_chat_course")
                        val courseInfo = signInfo.getJSONObject("courseInfo")
                        val activeId = signInfo.getLong("aid")
                        val classId = courseInfo.getInteger("classid")
                        val courseId = courseInfo.getString("courseid").toInt()
                        signActivities.add(
                            ChaoxingGroupSignActivityEntity(
                                ChaoxingSignHelper.getIMSignDestination(
                                    signInfo.getString("atypeName"),
                                    activeId,
                                    classId,
                                    courseId
                                )!!,
                                signInfo.getString("title"),
                                activeId,
                                classId,
                                courseId,
                                courseInfo.getString("coursename"),
                                signInfo.getString("subTitle")
                            )
                        )
                    }
                    return@forEach
                }
            }
        }
    }

    suspend fun fetchIMHistoryMessages(
        imGroup: ChaoxingIMGroup,
        httpClient: ChaoxingHttpClient,
        imConfig: ChaoxingIMConfig
    ): List<MessageBody> {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder().post(
                    JSONObject()
                        .fluentPut("end", "-1")
                        .fluentPut(
                            "queue", if (imGroup.isGroup) {
                                "${imGroup.chatId}@conference.easemob.com"
                            } else {
                                "${imGroup.chatId}@easemob.com"
                            }
                        )
                        .fluentPut("start", "-1").toString()
                        .toRequestBody("text/plain;charset=UTF-8".toMediaType())
                )
                    .addHeader("Authorization", "Bearer ${imConfig.imToken}")
                    .url(URL_MESSAGE_ROAMING.format(imConfig.imTuid))
                    .build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                val responseBody = response.body.string()
                val json = JSON.parseObject(responseBody)
                val data = json.getJSONObject("data")
                val msgs = data.getJSONArray("msgs")
                val resultList = mutableListOf<MessageBody>()

                for (i in 0 until msgs.size) {
                    val msgObj = msgs.getJSONObject(i)
                    val msgStr = msgObj.getString("msg")

                    val msgBytes = Base64.decode(msgStr, Base64.DEFAULT)
                    val meta = Meta.parseFrom(msgBytes)

                    val messageBody = MessageBody.parseFrom(meta.field6)

                    resultList.add(messageBody)
                }
                parseIMMessageBody(resultList)
                return@use resultList
            }
        }
    }
}