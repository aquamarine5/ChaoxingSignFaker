/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.hyphenate.EMCallBack
import com.hyphenate.EMMessageListener
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMMessage
import com.hyphenate.chat.EMOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.json.JSONObject as OrgJsonObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ChaoxingIMListener {
    const val EASEMOB_APP_KEY = "cx-dev#cxstudy"

    data class IMSignReminderEvent(
        val imUsername: String,
        val activity: ChaoxingIMHelper.IMSignActivityInfo,
        val receivedTimestamp: Long
    )

    private val _signReminderEvents =
        MutableSharedFlow<IMSignReminderEvent>(extraBufferCapacity = 64)
    val signReminderEvents: SharedFlow<IMSignReminderEvent> = _signReminderEvents.asSharedFlow()

    @Volatile
    private var isSdkInitialized = false

    @Volatile
    private var isMessageListenerRegistered = false

    fun ensureInit(context: Context) {
        if (isSdkInitialized) return
        synchronized(this) {
            if (isSdkInitialized) return
            val options = EMOptions().apply {
                appKey = EASEMOB_APP_KEY
                setRequireAck(true)
                setRequireDeliveryAck(false)
                setAutoDownloadThumbnail(false)
                setDeleteMessagesAsExitGroup(true)
                setAutoLogin(true)
            }
            runCatching {
                EMClient.getInstance().init(context.applicationContext, options)
            }.onFailure {
                throw ChaoxingParseDataException("环信 IM SDK 初始化失败", throwable = it)
            }
            registerMessageListener()
            isSdkInitialized = true
        }
    }

    suspend fun ensureLogin(imUsername: String, imPassword: String) {
        val client = EMClient.getInstance()
        if (client.isLoggedIn && client.currentUser == imUsername) {
            registerMessageListener()
            return
        }
        if (client.isLoggedIn) client.logout(false)
        suspendCancellableCoroutine<Unit> { continuation ->
            client.login(imUsername, imPassword, object : EMCallBack {
                override fun onSuccess() {
                    registerMessageListener()
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(code: Int, error: String?) {
                    if (continuation.isActive) continuation.resumeWithException(
                        ChaoxingParseDataException("环信 IM 登录失败[$code]: ${error ?: "未知错误"}")
                    )
                }
            })
        }
    }

    private fun registerMessageListener() {
        synchronized(this) {
            if (isMessageListenerRegistered) return
            runCatching {
                EMClient.getInstance().chatManager()
                    .addMessageListener(object : EMMessageListener {
                        override fun onMessageReceived(messages: MutableList<EMMessage>) {
                            messages.forEach { handleMessage(it) }
                        }
                    })
            }.onFailure {
                throw ChaoxingParseDataException("注册环信消息监听器失败", throwable = it)
            }
            isMessageListenerRegistered = true
        }
    }

    private fun handleMessage(message: EMMessage) {
        if (message.direct() != EMMessage.Direct.RECEIVE) return
        val attachmentValue = message.ext()?.get("attachment") ?: return
        val attachObject = when (attachmentValue) {
            is OrgJsonObject ->
                runCatching { JSONObject.parseObject(attachmentValue.toString()) }
                    .getOrNull() ?: return

            is Map<*, *> ->
                runCatching {
                    JSONObject.parseObject(
                        OrgJsonObject(
                            attachmentValue.entries.associate { it.key.toString() to it.value }
                        ).toString()
                    )
                }.getOrNull() ?: return

            is String ->
                runCatching { JSONObject.parseObject(attachmentValue) }
                    .getOrNull() ?: return

            else -> return
        }
        val activity = ChaoxingIMHelper.parseIMSignActivityInfo(attachObject) ?: return
        val imUsername = EMClient.getInstance().currentUser ?: return
        _signReminderEvents.tryEmit(
            IMSignReminderEvent(
                imUsername,
                activity,
                System.currentTimeMillis()
            )
        )
    }
}
