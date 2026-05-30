/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException

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
                            JSONObject()
                                .fluentPut("currentFaceId", objectId)
                                .fluentPut("LiveDetectionStatus", 1)
                                .fluentPut("collectStatus", 1)
                                .fluentPut("cxtime", System.currentTimeMillis())
                                .toJSONString()
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


    suspend fun saveFaceImage(
        context: Context,
        objectId: String,
        phoneNumber: String? = null
    ) {

    }

    suspend fun saveFaceImage(
        client: ChaoxingHttpClient,
        context: Context,
        bitmap: Bitmap,
        phoneNumber: String? = null
    ) =
        withContext(Dispatchers.IO) {
            ChaoxingCloudDriveHelper.uploadImage(
                client,
                bitmap
            ).let { objectId ->
                context.chaoxingDataStore.updateData {
                    it.toBuilder().apply {
                        if (phoneNumber == null) {
                            setLoginSession(
                                loginSession.toBuilder().setFaceImageObjectId(objectId).build()
                            )
                        } else {
                            val index =
                                otherUsersList.indexOfFirst { user -> user.phoneNumber == phoneNumber }
                            if (index != -1) {
                                setOtherUsers(
                                    index,
                                    getOtherUsers(index).toBuilder().setFaceImageObjectId(objectId)
                                        .build()
                                )
                            }
                        }
                    }.build()
                }
            }
        }
}