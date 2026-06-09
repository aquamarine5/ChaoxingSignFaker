/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import com.alibaba.fastjson2.JSONObject
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException
import java.security.MessageDigest
import java.util.TreeMap

object ChaoxingFaceHelper {
    private val URL_CHECK_FACE_RESULT =
        "https://mobilelearn.chaoxing.com/pptSign/check-face-result?DB_STRATEGY=PRIMARY_KEY&STRATEGY_PARA=activeId".toHttpUrl()

    suspend fun checkFaceResultAndGetEnc(
        client: ChaoxingHttpClient,
        objectId: String,
        activeId: Long
    ): String =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_CHECK_FACE_RESULT.newBuilder()
                        .addQueryParameter("activeId", activeId.toString())
                        .addQueryParameter(
                            "faceResult",
                            buildFaceResult(client, objectId).toJSONString()
                        )
                        .build()
                ).get().build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                val jsonObject = JSONObject.parseObject(response.body.string())
                return@withContext jsonObject.getString("enc") ?: throw ChaoxingParseDataException(
                    "获取faceEnc失败",
                    data = jsonObject.toJSONString()
                )
            }
        }

    private fun buildFaceResult(client: ChaoxingHttpClient, objectId: String): JSONObject {
        val fields = mapOf(
            "currentFaceId" to objectId,
            "LiveDetectionStatus" to "1",
            "collectStatus" to "1"
        )
        val cxtime = System.currentTimeMillis().toString()
        return JSONObject()
            .fluentPut("currentFaceId", objectId)
            .fluentPut("LiveDetectionStatus", 1)
            .fluentPut("collectStatus", 1)
            .fluentPut("cxtime", cxtime)
            .apply {
                runCatching {
                    client.userEntity.clientId?.let { clientId ->
                        addSignToken(clientId, fields, cxtime)
                    }
                }.onFailure {
                    Sentry.captureException(it)
                }
            }
    }

    private fun JSONObject.addSignToken(
        clientId: String,
        fields: Map<String, String>,
        cxtime: String
    ) {
        val deviceInfo = ChaoxingDeviceInfoHelper.decryptClientId(clientId) ?: return
        val cxcid = deviceInfo.getString("cid") ?: return
        val sc = deviceInfo.getString("sc") ?: return
        val signedFields = TreeMap<String, String>().apply {
            putAll(fields)
            put("cxtime", cxtime)
            put("cxcid", cxcid)
        }
        val raw = buildString {
            signedFields.forEach { (key, value) ->
                append(key)
                append(value)
            }
            append(sc)
        }
        fluentPut("cxcid", cxcid)
        fluentPut("signToken", md5(raw))
    }

    private fun md5(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    suspend fun saveFaceImage(
        context: Context,
        objectId: String,
        phoneNumber: String? = null
    ) {

    }

    suspend fun saveFaceImage(
        client: ChaoxingHttpClient,
        context: Context,
        bitmap: Bitmap
    ) =
        withContext(Dispatchers.IO) {
            ChaoxingCloudDriveHelper.uploadImage(
                client,
                bitmap
            ).let { objectId ->
                context.chaoxingDataStore.updateData {
                    it.toBuilder().apply {
                        val phoneNumber = client.userEntity.phoneNumber
                        containsFaceRecognitionConfigures(client.userEntity.phoneNumber)
                    }.build()
                }
            }
        }
}
