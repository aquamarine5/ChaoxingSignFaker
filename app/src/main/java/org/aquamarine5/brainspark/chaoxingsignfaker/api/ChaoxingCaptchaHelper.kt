/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore

object ChaoxingCaptchaHelper {
    val URL_REMOTE_CAPTCHA_MEMORIES =
        "http://cdn.aquamarine5.fun/chaoxingsignfaker_captcha_memories_manifest.json".toHttpUrl()

    const val SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION=1

    suspend fun updateRemoteCaptchaMemoriesData(context: Context) {
        ChaoxingHttpClient.instance!!.newCall(
            Request.Builder()
                .url(URL_REMOTE_CAPTCHA_MEMORIES)
                .get()
                .build()
        ).execute().use {
            val jsonObject= JSONObject.parseObject(it.body.string())
            context.chaoxingDataStore.updateData {
                it.toBuilder()
                    .setCaptchaMemories(it.captchaMemories.toBuilder()
                        .setMemoriesVersion(jsonObject.getInteger("memoriesVersion"))
                        .build())
                    .build()
            }
        }
    }
}