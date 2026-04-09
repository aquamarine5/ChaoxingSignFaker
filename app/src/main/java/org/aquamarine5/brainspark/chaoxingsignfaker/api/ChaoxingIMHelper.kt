/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.util.Log
import com.hyphenate.EMCallBack
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMConversation
import com.hyphenate.chat.EMCursorResult
import com.hyphenate.chat.EMGroup
import com.hyphenate.chat.EMMessage
import com.hyphenate.chat.EMOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingIMConfig

object ChaoxingIMHelper {
    private class ChaoxingIMConfigParseException(arg: String, message: String? = null) :
        Exception("IM配置解析异常: $arg 获取失败，${message ?: "未知错误"}")

    val URL_IM_ME = "https://im.chaoxing.com/webim/me".toHttpUrl()

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
                    setFixedHBInterval(4500)
                    setIMServer("im-api-vip6-v2.easecdn.com")
                    imPort=443
                })
                conn.setDebugMode(true)
                Log.d("ChaoxingIMHelper", "开始IM登录，用户ID: ${imConfig.imTuid}, 用户名: ${imConfig.imName}, Token: ${imConfig.imToken}")
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