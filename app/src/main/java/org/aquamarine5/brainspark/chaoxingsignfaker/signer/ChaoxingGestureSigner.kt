/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpRequester
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GestureSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable

class ChaoxingGestureSigner(
    client: ChaoxingHttpRequester,
    private val destination: GestureSignDestination,
    baseSignInfo: JSONObject? = null
) : ChaoxingSigner(
    client,
    destination.activeId,
    destination.classId,
    destination.courseId,
    destination.extContent,
    baseSignInfo,
    destination.isOldSignVersion,
    destination.chatId
) {
    companion object {
        private val URL_CHECK_GESTURE =
            "https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/checkSignCode".toHttpUrl()
        private val URL_WEIXIN_PRESIGN =
            "https://mobilewx.chaoxing.com/widget/weixin/sign/stuSignController/preSign".toHttpUrl()
        private val SIGN_CODE_PATTERN = """id="signCode"\s+value="([^"]+)""".toRegex()
    }

    suspend fun getGestureSignCode(): String = withContext(Dispatchers.IO) {
        if (isOldSignVersion) return@withContext ""
        client.newCall(
            Request.Builder().get().url(
                URL_WEIXIN_PRESIGN.newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .addQueryParameter("puid", client.puid.toString())
                    .build()
            ).build()
        ).execute().use {
            it.checkResponseThrowException()
            val signCode = SIGN_CODE_PATTERN.find(it.body.string())?.groupValues?.get(1)
            requirePredictable(!signCode.isNullOrBlank()) { "未能从微信签到页获取手势图案" }
            signCode
        }
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
        if (isOldSignVersion) return@withContext true
        client.newCall(
            Request.Builder().url(
                URL_CHECK_GESTURE.newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .addQueryParameter("signCode", gestureOrderCode)
                    .build()
            ).build()
        ).execute().use {
            it.checkResponseThrowException()
            return@withContext JSONObject.parseObject(it.body.string()).getInteger("result") == 1
        }
    }

    suspend fun sign(
        gestureOrderCode: String,
        position: ChaoxingLocationSignEntity? = null,
        captchaValidate: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (isOldSignVersion) {
            return@withContext client.newCall(
                Request.Builder().url(
                    buildOldSignUrlBuilder().build()
                ).build()
            ).execute().use {
                it.checkResponseThrowException()
                return@use it.checkSignResult()
            }
        }
        client.newCall(
            Request.Builder().url(
                URL_SIGN_NO_PARAMETER.newBuilder()
                    .addQueryParameter("activeId", destination.activeId.toString())
                    .addCourseIdParameter()
                    .addQueryParameter("uid", client.puid.toString())
                    .addQueryParameter("clientip", "")
                    .addQueryParameter(
                        "latitude",
                        if (position != null) "%.6f".format(position.randomizedLatitude) else "-1"
                    )
                    .addQueryParameter(
                        "longitude",
                        if (position != null) "%.6f".format(position.randomizedLongitude) else "-1"
                    )
                    .addQueryParameter("appType", "15")
                    .addQueryParameter("fid", client.configuredFid.toString())
                    .addQueryParameter("name", client.name)
                    .addQueryParameter("signCode", gestureOrderCode)
                    .addQueryParameter("deviceCode", client.deviceCode)
                    .addLocationParameter(position, false)
                    .addLocationResultParameter(position)
                    .addEnc2Parameter(signEnc2)
                    .addValidateQueryParameter(captchaValidate)
                    .build()
            ).header(
                "Referer", getOriginPreSignUrl().toString()
            ).build()
        ).execute().use {
            it.checkResponseThrowException()
            return@use it.checkSignResult(position)
        }
    }

    @Deprecated(
        "Use sign instead",
        ReplaceWith("sign(gestureOrderCode, position, captchaValidate)")
    )
    suspend fun signWithCaptcha(
        gestureOrderCode: String,
        validateValue: String,
        position: ChaoxingLocationSignEntity? = null
    ) = sign(gestureOrderCode, position, validateValue)

    override val notSignedPageMarkers: List<String> = listOf("传达的手势图案")
}
