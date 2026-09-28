/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper.NO_SIGN_OFF_EVENT
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpRequester
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException

class ChaoxingLocationSigner(
    client: ChaoxingHttpRequester,
    private val destination: GetLocationDestination,
    baseSignInfo: JSONObject? = null
) : ChaoxingSigner(
    client,
    destination.activeId,
    destination.classId,
    destination.courseId,
    destination.extContent,
    baseSignInfo
) {
    suspend fun getLocationSignInfo(): Pair<ChaoxingLocationDetailEntity, ChaoxingSignOutEntity> {
        getSignInfo().let { jsonResult ->
            return ChaoxingLocationDetailEntity(
                jsonResult.getDouble("locationLatitude"),
                jsonResult.getDouble("locationLongitude"),
                jsonResult.getInteger("locationRange")
            ) to ChaoxingSignOutEntity(
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

    suspend fun sign(
        signLocation: ChaoxingLocationSignEntity,
        faceImageObjectId: String? = null,
        context: Context,
        captchaValidate: String? = null
    ): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN_NO_PARAMETER.newBuilder()
                        .addQueryParameter("name", client.name)
                        .addQueryParameter("address", signLocation.address)
                        .addQueryParameter("activeId", destination.activeId.toString())
                        .addCourseIdParameter()
                        .addQueryParameter("uid", client.puid.toString())
                        .addQueryParameter("clientip", "")
                        .addQueryParameter("latitude", signLocation.randomizedLatitude.toString())
                        .addQueryParameter("longitude", signLocation.randomizedLongitude.toString())
                        .addQueryParameter("fid", client.configuredFid.toString())
                        .addQueryParameter("appType", "15")
                        .addQueryParameter("ifTiJiao", "1")
                        .addValidateQueryParameter(captchaValidate)
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .addQueryParameter("vpProbability", "-1")
                        .addQueryParameter("vpStrategy", "")
                        .addEnc2Parameter(signEnc2)
                        .addLocationResultParameter(signLocation)
                        .addFaceRecognitionParameter(
                            faceImageObjectId,
                            context,
                            isCourseIdParameterAdded = true
                        )
                        .build()
                ).get().build()
            ).execute().use {
                it.checkResponseThrowException()
                return@use it.checkSignResult(signLocation)
            }
        }

    @Deprecated("Use sign instead", ReplaceWith("sign(signLocation, faceImageObjectId, context, captchaValidate)"))
    suspend fun signWithCaptcha(
        signLocation: ChaoxingLocationSignEntity,
        validateValue: String,
        faceImageObjectId: String? = null,
        context: Context
    ) = sign(signLocation, faceImageObjectId, context, validateValue)

    override val notSignedPageMarkers: List<String> = listOf("恭喜你已完成签")
}
