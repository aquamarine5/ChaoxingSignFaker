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
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper.NO_SIGN_OFF_EVENT
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PasswordSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.ChaoxingLocationSignException
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.Companion.CLASSTAG

class ChaoxingPasswordSigner(
    client: ChaoxingHttpClient, private val destination: PasswordSignDestination
) : ChaoxingSigner(
    client,
    destination.activeId,
    destination.classId,
    destination.courseId,
    destination.extContent,
) {
    companion object {
        const val URL_CHECK_SIGN_CODE =
            "https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/checkSignCode"
    }

    override suspend fun checkAlreadySign(response: String): Boolean {
        return response.contains("输入发起者设置的签到码完成签到").not()
    }

    suspend fun getPasswordInfo(): Pair<Int, ChaoxingSignOutEntity> = withContext(Dispatchers.IO) {
        getSignInfo().let { jsonResult ->
            jsonResult.getInteger("numberCount") to ChaoxingSignOutEntity(
                jsonResult.getLong("signInId"),
                jsonResult.getLong("signOutId"),
                jsonResult.getLong("signOutPublishTimeStamp").let { time ->
                    if (time == NO_SIGN_OFF_EVENT) null else time
                },
                destination.classId,
                destination.courseId
            )
        }
    }

    suspend fun checkSignCode(signCode: Int): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_CHECK_SIGN_CODE.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .addQueryParameter("signCode", signCode.toString())
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            val body = JSONObject.parseObject(it.body.string())
            return@withContext body.getInteger("result") == 1
        }
    }

    suspend fun sign(signCode: Int): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", "")
                    .addQueryParameter("longitude", "")
                    .addQueryParameter("activeId", destination.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("signCode", signCode.toString())
                    .addQueryParameter("deviceCode", client.deviceCode)
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
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

    suspend fun signWithCaptcha(signCode: Int, validateValue: String) =
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
                        .addQueryParameter("signCode", signCode.toString())
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .addQueryParameter("validate", validateValue)
                        .build()
                ).build()
            ).execute().use {
                if (it.checkResponse(client.context)) {
                    throw ChaoxingHttpClient.ChaoxingNetworkException()
                }
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
}