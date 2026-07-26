/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingCaptchaResult
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException

object ChaoxingCaptchaHelper {
    val URL_REMOTE_CAPTCHA_MEMORIES =
        "http://cdn.aquamarine5.fun/chaoxingsignfaker_captcha_memories_manifest.json".toHttpUrl()

    const val SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION = 1

    suspend fun updateRemoteCaptchaMemoriesData(context: Context) {
        ChaoxingHttpClient.instance!!.newCall(
            Request.Builder()
                .url(URL_REMOTE_CAPTCHA_MEMORIES)
                .get()
                .build()
        ).execute().use { response ->
            response.checkResponseThrowException()
            withContext(Dispatchers.IO) {
                val jsonObject = JSONObject.parseObject(response.body.string())
                context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder()
                        .setCaptchaMemories(
                            dataStore.captchaMemories.toBuilder()
                                .setMemoriesVersion(jsonObject.getInteger("memoriesVersion"))
                                .clearMemories()
                                .addAllMemories(jsonObject.getJSONArray("captchaMemories").let {
                                    List(it.size) { index ->
                                        val jsonMemory = it.getJSONObject(index)
                                        return@List ChaoxingCaptchaResult.newBuilder()
                                            .setToken(jsonMemory.getString("token"))
                                            .setXPosition(jsonMemory.getInteger("xPosition"))
                                            .build()
                                    }
                                })
                                .build()
                        )
                        .build()
                }
            }
        }
    }
}