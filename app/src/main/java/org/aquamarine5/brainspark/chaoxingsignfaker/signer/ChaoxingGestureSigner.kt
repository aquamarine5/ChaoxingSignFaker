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
    baseSignInfo
) {
    companion object {
        private val URL_CHECK_GESTURE =
            "https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/checkSignCode".toHttpUrl()
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
