/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper.NO_SIGN_OFF_EVENT
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpRequester
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingQRCodeDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException

class ChaoxingQRCodeSigner(
    client: ChaoxingHttpRequester,
    qrCodeActivityEntity: QRCodeSignDestination,
    baseSignInfo: JSONObject? = null
) : ChaoxingSigner(
    client,
    qrCodeActivityEntity.activeId,
    qrCodeActivityEntity.classId,
    qrCodeActivityEntity.courseId,
    qrCodeActivityEntity.extContent,
    baseSignInfo
) {
    companion object {
        fun parseQRCode(qrcode: Barcode): String {
            val rawValue = qrcode.rawValue ?: qrcode.url?.url
            ?: throw QRCodeParseException(qrcode.rawValue ?: "null")
            if (rawValue.startsWith("SIGNIN:")) {
                val content = rawValue.removePrefix("SIGNIN:").substringBefore("-")
                if (content.contains("&enc=")) {
                    val enc = content.substringAfter("&enc=").substringBefore("&")
                    if (enc.isNotBlank()) return enc
                }
                throw QRCodeParseException(rawValue)
            }
            return rawValue.toHttpUrlOrNull()?.queryParameter("enc")?.takeIf { it.isNotBlank() }
                ?: throw QRCodeParseException(rawValue)
        }
    }

    class QRCodeParseException(val rawValue: String, throwable: Throwable? = null) :
        ChaoxingParseDataException("二维码解析失败", throwable, rawValue)

    class QRCodeExpiredException(throwable: Throwable? = null) :
        ChaoxingParseDataException("二维码已过期", throwable)

    suspend fun getQRCodeSignInfo(): Pair<ChaoxingQRCodeDetailEntity, ChaoxingSignOutEntity> {
        return getSignInfo().run {
            ChaoxingQRCodeDetailEntity(
                getInteger("ifopenAddress") == 1,
                getInteger("ifrefreshewm") == 1
            ) to ChaoxingSignOutEntity(
                getLong("signInId"),
                getLong("signOutId"),
                getLong("signOutPublishTimeStamp").let { time ->
                    if (time == NO_SIGN_OFF_EVENT) null else time
                },
                classId,
                courseId
            )
        }
    }

    suspend fun sign(
        enc: String,
        position: ChaoxingLocationSignEntity?,
        faceImageObjectId: String? = null,
        context: Context,
        captchaValidate: String? = null
    ): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN_NO_PARAMETER.newBuilder()
                        .addQueryParameter("enc", enc)
                        .addQueryParameter("name", client.name)
                        .addQueryParameter("activeId", activeId.toString())
                        .addQueryParameter("uid", client.puid.toString())
                        .addQueryParameter("clientip", "")
                        .addLocationParameter(position, false)
                        .addQueryParameter("latitude", "-1")
                        .addQueryParameter("longitude", "-1")
                        .addQueryParameter("fid", client.configuredFid.toString())
                        .addQueryParameter("appType", "15")
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .addQueryParameter("vpProbability", "-1")
                        .addQueryParameter("vpStrategy", "")
                        .addCourseIdParameter()
                        .addEnc2Parameter(signEnc2)
                        .addValidateQueryParameter(captchaValidate)
                        .addLocationResultParameter(position)
                        .addFaceRecognitionParameter(
                            faceImageObjectId,
                            context,
                            isCourseIdParameterAdded = true
                        )
                        .build()
                ).build()
            ).execute().use {
                it.checkResponseThrowException()
                return@use it.checkSignResult(position)
            }
        }

    @Deprecated("Use sign instead", ReplaceWith("sign(enc, position, faceImageObjectId, context, captchaValidate)"))
    suspend fun signWithCaptcha(
        enc: String,
        position: ChaoxingLocationSignEntity?,
        captchaValidate: String,
        faceImageObjectId: String? = null,
        context: Context
    ) = sign(enc, position, faceImageObjectId, context, captchaValidate)

    override val notSignedPageMarkers: List<String> = listOf("扫一扫")
}
