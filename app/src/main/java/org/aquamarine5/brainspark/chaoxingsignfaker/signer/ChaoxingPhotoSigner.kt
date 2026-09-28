/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper.NO_SIGN_OFF_EVENT
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpRequester
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.ChaoxingPhotoActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException


class ChaoxingPhotoSigner(
    client: ChaoxingHttpRequester,
    private val photoActivityEntity: ChaoxingPhotoActivityEntity,
    baseSignInfo: JSONObject? = null
) : ChaoxingSigner(
    client,
    photoActivityEntity.activeId,
    photoActivityEntity.classId,
    photoActivityEntity.courseId,
    photoActivityEntity.extContent,
    baseSignInfo
) {
    class ChaoxingPhotoSignException(message: String, throwable: Throwable? = null) :
        ChaoxingParseDataException(message, throwable)

    class ChaoxingIncorrectSignTypeException(throwable: Throwable? = null) :
        ChaoxingParseDataException("签到类型不匹配，应是图片签到", throwable)

    companion object {
        const val URL_CLOUD_UPLOAD = "https://pan-yz.chaoxing.com/upload?_from=mobilelearn&_token="
    }

    suspend fun getSignoffEntity(jsonResult: JSONObject): ChaoxingSignOutEntity =
        withContext(Dispatchers.IO) {
            ChaoxingSignOutEntity(
                jsonResult.getLong("signInId"),
                jsonResult.getLong("signOutId"),
                jsonResult.getLong("signOutPublishTimeStamp").let { time ->
                    if (time == NO_SIGN_OFF_EVENT) null else time
                },
                photoActivityEntity.classId,
                photoActivityEntity.courseId
            )
        }

    private fun HttpUrl.Builder.addPhotoSignParameter(
        objectId: String?,
        captchaValidate: String?
    ): HttpUrl.Builder {
        addQueryParameter("activeId", photoActivityEntity.activeId.toString())
        addCourseIdParameter()
        addQueryParameter("uid", client.puid.toString())
        addQueryParameter("clientip", "")
        addQueryParameter("useragent", "")
        addQueryParameter("latitude", "-1")
        addQueryParameter("longitude", "-1")
        addQueryParameter("appType", "15")
        addQueryParameter("fid", client.configuredFid.toString())
        if (objectId != null) addQueryParameter("objectId", objectId)
        addQueryParameter("name", client.name)
        addValidateQueryParameter(captchaValidate)
        addQueryParameter("deviceCode", client.deviceCode)
        addEnc2Parameter(signEnc2)
        return this
    }

    suspend fun signByClick(captchaValidate: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN_NO_PARAMETER.newBuilder()
                        .addPhotoSignParameter(null, captchaValidate)
                        .build()
                ).get().build()
            ).execute().use {
                it.checkResponseThrowException()
                return@use it.checkSignResult()
            }
        }

    suspend fun signByImage(
        objectId: String,
        captchaValidate: String? = null
    ): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN_NO_PARAMETER.newBuilder()
                        .addPhotoSignParameter(objectId, captchaValidate)
                        .build()
                ).get().build()
            ).execute().use {
                it.checkResponseThrowException()
                return@use it.checkSignResult()
            }
        }

    @Deprecated("Use signByClick instead", ReplaceWith("signByClick(captchaValidate)"))
    suspend fun signByClickWithCaptcha(validateValue: String) = signByClick(validateValue)

    @Deprecated("Use signByImage instead", ReplaceWith("signByImage(objectId, captchaValidate)"))
    suspend fun signByImageWithCaptcha(objectId: String, validateValue: String) =
        signByImage(objectId, validateValue)


    suspend fun ifPhotoRequiredLogin(): Pair<Boolean, ChaoxingSignOutEntity> {
        val json = getSignInfo()
        return Pair(json.getInteger("ifphoto") == 1, getSignoffEntity(json))
    }

    override val notSignedPageMarkers: List<String> =
        listOf("请先拍照", "zactives-btn")
}
