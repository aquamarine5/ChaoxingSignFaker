/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GestureSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.ChaoxingLocationSignException
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.Companion.CLASSTAG

class ChaoxingGestureSigner(
    client: ChaoxingHttpClient, private val destination: GestureSignDestination
) : ChaoxingSigner(
    client,
    destination.activeId,
    destination.classId,
    destination.courseId,
    destination.extContent,
) {
    companion object {
        const val URL_CHECK_GESTURE =
            "https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/checkSignCode"
    }

    suspend fun getGestureSignInfo(): ChaoxingSignOutEntity = withContext(Dispatchers.IO) {
        getSignInfo().let { jsonResult ->
            return@withContext ChaoxingSignOutEntity(
                jsonResult.getLong("signInId"),
                jsonResult.getLong("signOutId"),
                jsonResult.getLong("signOutPublishTimeStamp").let { time ->
                    if (time == -1L || time == 4999L) null else time
                },
                destination.classId,
                destination.courseId
            )
        }
    }

    suspend fun checkSignGesture(gestureOrderCode: String): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_CHECK_GESTURE.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .addQueryParameter("signCode", gestureOrderCode)
                    .build()
            ).build()
        ).execute().use {
            it.checkResponseThrowException()
            return@withContext JSONObject.parseObject(it.body.string()).getInteger("result") == 1
        }
    }

    suspend fun sign(gestureOrderCode: Int): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", "")
                    .addQueryParameter("longitude", "")
                    .addQueryParameter("activeId", destination.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("signCode", gestureOrderCode.toString())
                    .addQueryParameter("deviceCode", client.deviceCode)
                    .build()
            ).build()
        ).execute().use {
            it.checkResponseThrowException()
            val result = it.body.string()
            if (result == "success2")
                throw SignAlreadyEndedException()
            if (result == "您已签到过了") {
                throw AlreadySignedException()
            }
            if (result == "validate") {
                return@use true
            }
            if (result != "success") {
                Log.w(CLASSTAG, result)
                throw ChaoxingLocationSignException(result)
            } else {
                return@use false
            }
        }
    }

    suspend fun signWithCaptcha(gestureOrderCode: Int, validateValue: String) =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN.toHttpUrl().newBuilder()
                        .addQueryParameter("latitude", "")
                        .addQueryParameter("longitude", "")
                        .addQueryParameter("activeId", destination.activeId.toString())
                        .addQueryParameter("uid", client.userEntity.puid.toString())
                        .addQueryParameter("name", client.userEntity.name)
                        .addQueryParameter("fid", client.userEntity.fid.toString())
                        .addQueryParameter("signCode", gestureOrderCode.toString())
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .addQueryParameter("validate", validateValue)
                        .build()
                ).build()
            ).execute().use {
                it.checkResponseThrowException()
                val result = it.body.string()
                if (result == "success2")
                    throw SignAlreadyEndedException()
                if (result == "您已签到过了") {
                    throw AlreadySignedException()
                }
                if (result != "success") {
                    Log.w(CLASSTAG, result)
                    throw ChaoxingPredictableException(result)
                }
            }
        }

    override suspend fun checkAlreadySign(response: String): Boolean =
        !response.contains("传达的手势图案")
}