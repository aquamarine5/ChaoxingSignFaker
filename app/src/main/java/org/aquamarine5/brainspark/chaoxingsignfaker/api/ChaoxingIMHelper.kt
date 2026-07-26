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
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.MessageBody
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.Meta
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingEasemobIMConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingEasemobIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingGroupSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMGroup
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException

object ChaoxingIMHelper {
    private class ChaoxingIMConfigParseException(
        arg: String,
        message: String? = null,
        data: String? = null
    ) : ChaoxingParseDataException(
        "IM配置解析异常: $arg 获取失败，${message ?: "未知错误"}",
        data = data
    )

    @Deprecated(
        message = "`im.chaoxing.com` is deprecated",
        replaceWith = ReplaceWith("URL_EASEMOB_IM_TOKEN")
    )
    val URL_IM_ME = "https://im.chaoxing.com/webim/me".toHttpUrl()

    @Deprecated(message = "`im.chaoxing.com` is deprecated")
    val URL_IM_GROUPS = "https://im.chaoxing.com/webim/message/list/getMessageList".toHttpUrl()

    val URL_EASEMOB_IM_TOKEN = "https://a1-vip6.easemob.com/cx-dev/cxstudy/token".toHttpUrl()
    val URL_EASEMOB_IM_JOINED_GROUPS =
        "https://a1-vip6.easemob.com/cx-dev/cxstudy/users/295148424/joined_chatgroups?detail=true&version=v3&pagenum=1&pagesize=200"

    const val URL_MESSAGE_ROAMING =
        "https://a1-vip6.easecdn.com/cx-dev/cxstudy/users/%s/messageroaming"
    const val USER_AGENT_EASEMOB = "Easemob-SDK(Android) 4.9.0.1"

//
//    fun initializeEasemobClient(httpClient: ChaoxingHttpClient, context: Context) {
//        EMClient.getInstance().init(context, EMOptions().apply {
//            appKey = "cx-dev#cxstudy"
//        })
//        EMClient.getInstance()
//            .login(httpClient.userEntity.uid.toString(), "kwe371", object : EMCallBack {
//                override fun onSuccess() {
//                    Log.i("ChaoxingIMHelper", "Success")
//                }
//
//                override fun onError(p0: Int, p1: String?) {
//                    Log.e("ChaoxingIMHelper", "$p0 $p1")
//                }
//            })
//    }
//
//    fun getHistoryMessages() {
////        EMClient.getInstance().chatManager().asyncFetchHistoryMessages()
//    }
//
//    suspend fun getConversations(): List<EMConversation> =
//        suspendCancellableCoroutine { continuation ->
//            EMClient.getInstance().chatManager().asyncFetchConversationsFromServer(
//                100,
//                "",
//                object : EMValueCallBack<EMCursorResult<EMConversation>> {
//                    override fun onSuccess(p0: EMCursorResult<EMConversation>?) {
//                        Log.i("ChaoxingIMHelper", p0!!.data.toString())
//                        continuation.resume(p0!!.data)
//                    }
//
//                    override fun onError(p0: Int, p1: String?) {
//                        TODO("Not yet implemented")
//                    }
//                })
//        }
//
//
//    suspend fun getEasemobGroups(
//        httpClient: ChaoxingHttpClient,
//        config: ChaoxingEasemobIMConfig
//    ): List<ChaoxingIMGroup> {
//        return TODO()
//    }

    suspend fun getEasemobConfig(httpClient: ChaoxingHttpClient): ChaoxingEasemobIMConfig {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder().url(URL_EASEMOB_IM_TOKEN)
                    .post(
                        JSONObject()
                            .fluentPut("grant_type", "password")
                            .fluentPut("password", "kwe371")
                            .fluentPut("username", httpClient.userEntity.uid)
                            .toJSONString().toRequestBody()
                    )
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", USER_AGENT_EASEMOB)
                    .build()
            ).execute().use {
                it.checkResponseThrowException()
                val jsonObject = JSONObject.parseObject(it.body.string())
                return@use ChaoxingEasemobIMConfig(
                    jsonObject.getString("access_token"),
                    jsonObject.getJSONObject("user").getString("uuid"),
                    jsonObject.getJSONObject("user").getString("username")
                )
            }
        }
    }

    suspend fun getEasemobIMGroups(
        httpClient: ChaoxingHttpClient,
        imConfig: ChaoxingEasemobIMConfig
    ): List<ChaoxingEasemobIMGroup> {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder()
                    .url(URL_EASEMOB_IM_JOINED_GROUPS)
                    .addHeader("Authorization", "Bearer ${imConfig.accessToken}")
                    .header("User-Agent", USER_AGENT_EASEMOB)
                    .build()
            ).execute().use {
                it.checkResponseThrowException()
                val jsonObject = JSONObject.parseObject(it.body.string()).getJSONArray("data")
                return@withContext List(jsonObject.size) { index ->
                    jsonObject.getJSONObject(index).run {
                        val jsonDescription = JSONObject.parseObject(getString("description"))
                        ChaoxingEasemobIMGroup(
                            getString("name").ifEmpty {
                                jsonDescription.getString("coursename")
                            },
                            getString("id"),
                            jsonDescription.getString("imageUrl")
                        )
                    }
                }

            }
        }
    }

    @Suppress("Deprecation")
    @Deprecated(message = "`im.chaoxing.com` is deprecated", level = DeprecationLevel.ERROR)
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
                return@use data.mapNotNull { runCatching { ChaoxingIMGroup.fromJson(it as JSONObject) }.getOrNull() }
            }
        }
    }


    @Suppress("Deprecation")
    @Deprecated(message = "`im.chaoxing.com` is deprecated", level = DeprecationLevel.ERROR)
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

    suspend fun parseIMMessageBody(imMessages: List<MessageBody>): List<ChaoxingGroupSignActivityEntity> {
        val signActivities = mutableListOf<ChaoxingGroupSignActivityEntity>()
        imMessages.forEach forEachImMessages@{
            it.extList?.forEach { ext ->
                if (ext.key == "attachment") {
                    val attachObject = JSONObject.parseObject(ext.stringValue)
                    if (attachObject.getInteger("attachmentType") == 15) {
                        val signInfo = attachObject.getJSONObject("att_chat_course")
                            ?: return@forEachImMessages
                        val courseInfo =
                            signInfo.getJSONObject("courseInfo") ?: return@forEachImMessages
                        val activeId = signInfo.getLong("aid") ?: return@forEachImMessages
                        if (activeId == 0L ||
                            (signInfo.getInteger("atype") != 2 &&
                                    signInfo.getInteger("atype") != 74)
                        )
                            return@forEachImMessages
                        val classId = courseInfo.getInteger("classid")
                        val courseId = courseInfo.getString("courseid").toInt()
                        val activeTypeName = signInfo.getString("atypeName")
                        signActivities.add(
                            ChaoxingGroupSignActivityEntity(
                                ChaoxingSignHelper.getIMSignDestination(
                                    activeTypeName,
                                    activeId,
                                    classId,
                                    courseId
                                ) ?: throw ChaoxingIMConfigParseException(
                                    "atypeName",
                                    "未知签到类型: $activeTypeName",
                                    attachObject.toJSONString()
                                ),
                                signInfo.getString("title"),
                                activeId,
                                classId,
                                courseId,
                                courseInfo.getString("coursename"),
                                signInfo.getString("subTitle"),
                                activeTypeName
                            )
                        )
                    }
                    return@forEachImMessages
                }
            }
        }
        return signActivities
    }

    suspend fun fetchIMHistoryMessages(
        imGroup: ChaoxingEasemobIMGroup,
        httpClient: ChaoxingHttpClient,
        imConfig: ChaoxingEasemobIMConfig
    ): List<ChaoxingGroupSignActivityEntity> {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder().post(
                    JSONObject()
                        .fluentPut("end", "-1")
                        .fluentPut(
                            "queue", "${imGroup.id}@conference.easemob.com"
                        )
                        .fluentPut("start", "-1").toString()
                        .toRequestBody("text/plain;charset=UTF-8".toMediaType())
                )
                    .addHeader("Authorization", "Bearer ${imConfig.accessToken}")
                    .header("User-Agent", USER_AGENT_EASEMOB)
                    .url(URL_MESSAGE_ROAMING.format(imConfig.username))
                    .build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                val responseBody = response.body.string()
                val json = JSON.parseObject(responseBody)
                val data = json.getJSONObject("data")
                val messages = data.getJSONArray("msgs")
                val resultList = mutableListOf<MessageBody>()
                for (i in messages.indices) {
                    val msgObj = messages.getJSONObject(i)
                    val msgStr = msgObj.getString("msg")
                    val msgBytes = Base64.decode(msgStr, Base64.DEFAULT)
                    val meta = Meta.parseFrom(msgBytes)
                    val messageBody = MessageBody.parseFrom(meta.field6)
                    resultList.add(messageBody)
                }
                return@use parseIMMessageBody(resultList)
            }
        }
    }

    @Suppress("Deprecation")
    @Deprecated(message = "`im.chaoxing.com` is deprecated", level = DeprecationLevel.ERROR)
    suspend fun fetchIMHistoryMessages(
        imGroup: ChaoxingIMGroup,
        httpClient: ChaoxingHttpClient,
        imConfig: ChaoxingIMConfig
    ): List<ChaoxingGroupSignActivityEntity> {
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
                val messages = data.getJSONArray("msgs")
                val resultList = mutableListOf<MessageBody>()
                for (i in messages.indices) {
                    val msgObj = messages.getJSONObject(i)
                    val msgStr = msgObj.getString("msg")
                    val msgBytes = Base64.decode(msgStr, Base64.DEFAULT)
                    val meta = Meta.parseFrom(msgBytes)
                    val messageBody = MessageBody.parseFrom(meta.field6)
                    resultList.add(messageBody)
                }
                return@use parseIMMessageBody(resultList)
            }
        }
    }
}