/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.MessageBody
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.easemob.Meta
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMConfig

object ChaoxingIMHelper {
    private class ChaoxingIMConfigParseException(arg: String, message: String? = null) :
        Exception("IM配置解析异常: $arg 获取失败，${message ?: "未知错误"}")

    val URL_IM_ME = "https://im.chaoxing.com/webim/me".toHttpUrl()

    const val URL_MESSAGE_ROAMING =
        "https://a1-vip6.easecdn.com/cx-dev/cxstudy/users/%d/messageroaming"
    const val IM_APPKEY = "cx-dev#cxstudy"

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

    suspend fun loginIM(
        httpClient: ChaoxingHttpClient,
        context: Context,
        imConfig: ChaoxingIMConfig
    ) {
        suspendCancellableCoroutine { continuation ->
            EMClient.getInstance().let { conn ->
                conn.init(context, EMOptions().apply {
//                    enableDNSConfig(false)
//                    autoLogin = true
                    usingHttpsOnly = true
                    appKey = IM_APPKEY
                    restServer = "https://a1-vip6.easecdn.com"
//                    setFixedHBInterval(4500)
                    customOSPlatform = 16
                    customDeviceName = "webim"
                    webSocketServer = "im-api-vip6-v2.easecdn.com"
                    imPort = 443
                })
                conn.setDebugMode(true)
                Log.d(
                    "ChaoxingIMHelper",
                    "开始IM登录，用户ID: ${imConfig.imTuid}, 用户名: ${imConfig.imName}, Token: ${imConfig.imToken}"
                )
                conn.loginWithToken(imConfig.imTuid, imConfig.imToken, object : EMCallBack {
                    override fun onSuccess() {
                        Log.d("ChaoxingIMHelper", "IM登录成功，用户ID: ${conn.currentUser}")
                        continuation.resumeWith(Result.success(Unit))
                    }

                    override fun onError(code: Int, error: String?) {
                        throw Exception("IM登录失败，错误码：$code，错误信息：$error")
                    }

                    override fun onProgress(progress: Int, status: String?) {
                        // 登录进度回调（可选）
                    }
                })
            }
        }


    }

    suspend fun getIMGroups(
    ): List<EMGroup> {
        return withContext(Dispatchers.IO) {
            EMClient.getInstance().groupManager().getJoinedGroupsFromServer()
        }
    }

    suspend fun fetchIMHistoryMessages(
        conversationId: String,
        httpClient: ChaoxingHttpClient,
        imConfig: ChaoxingIMConfig
    ): List<MessageBody> {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(
                Request.Builder().get().url(URL_MESSAGE_ROAMING.format(imConfig.imTuid)).build()
            ).execute().use { response ->
                val responseBody = response.body.string() ?: return@use emptyList()
                val json = JSON.parseObject(responseBody)
                val data = json.getJSONObject("data")
                val msgs = data.getJSONArray("msgs")
                val resultList = mutableListOf<MessageBody>()

                for (i in 0 until msgs.size) {
                    val msgObj = msgs.getJSONObject(i)
                    val msgStr = msgObj.getString("msg")

                    val msgBytes = Base64.decode(msgStr, Base64.DEFAULT)
                    val meta = Meta.parseFrom(msgBytes)

                    val field6Str = meta.field6.toStringUtf8()
                    val field6Bytes = Base64.decode(field6Str, Base64.DEFAULT)
                    val messageBody = MessageBody.parseFrom(field6Bytes)

                    resultList.add(messageBody)
                }

                return@use resultList
            }
        }
    }

    suspend fun getIMGroupHistoryMessages(
        groupId: String,
        pageSize: Int = 5,
        lastMessageId: String? = null
    ): EMCursorResult<EMMessage> {
        return withContext(Dispatchers.IO) {
            EMClient.getInstance().chatManager()
                .fetchHistoryMessages(
                    groupId,
                    EMConversation.EMConversationType.GroupChat,
                    pageSize,
                    lastMessageId
                )
        }
    }
}