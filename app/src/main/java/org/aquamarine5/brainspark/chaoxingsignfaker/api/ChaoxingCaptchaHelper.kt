/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingCaptchaResult
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.OnlyAppDevelopedMode
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.StoredData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.storedData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ChaoxingCaptchaHelper {
    val URL_REMOTE_CAPTCHA_MEMORIES =
        "http://cdn.aquamarine5.fun/chaoxingsignfaker_captcha_memories.json".toHttpUrl()

    const val SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION = 1

    var storedCaptchaMemories: StoredData<Context, HashMap<String, Float>> =
        storedData { getCaptchaMemories(it) }

    suspend fun getCaptchaMemories(context: Context): HashMap<String, Float> {
        return context.chaoxingDataStore.data.first().captchaMemories.memoriesList.associateTo(
            HashMap()
        ) { it.token to it.xPosition }
    }

    @OnlyAppDevelopedMode
    suspend fun saveCaptchaMemories(context: Context) {
        context.chaoxingDataStore.updateData { dataStore ->
            dataStore.toBuilder()
                .setCaptchaMemories(
                    dataStore.captchaMemories.toBuilder()
                        .clearMemories()
                        .addAllMemories(
                            storedCaptchaMemories.getValue(context).map { (token, xPosition) ->
                                ChaoxingCaptchaResult.newBuilder()
                                    .setToken(token)
                                    .setXPosition(xPosition)
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
        }
    }

    suspend fun updateRemoteCaptchaMemoriesData(
        context: Context,
        onHigherManifestVersionWarning: (currentVersion: Int, supportVersion: Int) -> Unit
    ): Result<Unit> =
        runCatching {
            ChaoxingHttpClient.instance!!.newCall(
                Request.Builder()
                    .url(URL_REMOTE_CAPTCHA_MEMORIES)
                    .get()
                    .build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                withContext(Dispatchers.IO) {
                    val jsonObject = JSONObject.parseObject(response.body.string())
                    jsonObject.getInteger("manifestVersion")?.let { currentVersion ->
                        if (currentVersion > SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION) {
                            onHigherManifestVersionWarning(
                                currentVersion,
                                SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION
                            )
                        }
                    }
                    context.chaoxingDataStore.updateData { dataStore ->
                        dataStore.toBuilder()
                            .setCaptchaMemories(
                                dataStore.captchaMemories.toBuilder()
                                    .setLastCheckRemoteMemoriesTimestamp(System.currentTimeMillis())
                                    .apply {
                                        if (jsonObject.getInteger("memoriesVersion") > dataStore.captchaMemories.memoriesVersion) {
                                            setMemoriesVersion(jsonObject.getInteger("memoriesVersion"))
                                            clearMemories()
                                            addAllMemories(
                                                jsonObject.getJSONArray("captchaMemories")
                                                    .let { array ->
                                                        List(array.size) { index ->
                                                            val jsonMemory =
                                                                array.getJSONObject(index)
                                                            return@List ChaoxingCaptchaResult.newBuilder()
                                                                .setToken(jsonMemory.getString("token"))
                                                                .setXPosition(jsonMemory.getFloat("xPosition"))
                                                                .build()
                                                        }.also { memories ->
                                                            storedCaptchaMemories.setValue(
                                                                memories.associateTo(
                                                                    HashMap()
                                                                ) { it.token to it.xPosition }
                                                            )
                                                        }
                                                    })
                                        }
                                    }
                                    .build()
                            )
                            .build()
                    }
                }
            }
        }


    @OnlyAppDevelopedMode
    suspend fun buildCaptchaMemoriesDataToJson(context: Context): String {
        return JSONObject().apply {
            put("manifestVersion", SUPPORT_CAPTCHA_MEMORIES_MANIFEST_VERSION)
            put(
                "memoriesVersion",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()
            )
            put(
                "captchaMemories",
                storedCaptchaMemories.getValue(context).mapTo(JSONArray()) { memory ->
                    JSONObject().apply {
                        put("token", memory.key)
                        put("xPosition", memory.value)
                    }
                })
        }.toJSONString(JSONWriter.Feature.PrettyFormat)
    }
}