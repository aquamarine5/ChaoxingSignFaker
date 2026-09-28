/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import android.util.Log
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
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingQRCodeParseResult
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
        fun parseQRCode(qrcode: Barcode): ChaoxingQRCodeParseResult {
            val rawValue = qrcode.rawValue ?: qrcode.url?.url
            ?: throw QRCodeParseException(qrcode.rawValue ?: "null")
            if (rawValue.startsWith("SIGNIN:")) {
                val content = rawValue.removePrefix("SIGNIN:").substringBefore("-")
                val enc = content.substringAfter("&enc=", "").substringBefore("&")
                if (enc.isBlank()) throw QRCodeParseException(rawValue)
                val signDetail = content.substringBefore("&enc=")
                return ChaoxingQRCodeParseResult(
                    signDetail.substringBefore("&").substringAfter("=", "").toLongOrNull(),
                    signDetail.substringAfterLast("=", "").takeIf { it.isNotBlank() },
                    enc
                )
            }
            val url = rawValue.toHttpUrlOrNull() ?: throw QRCodeParseException(rawValue)
            val enc = url.queryParameter("enc")?.takeIf { it.isNotBlank() }
            ?: throw QRCodeParseException(rawValue)
            return ChaoxingQRCodeParseResult(
                url.queryParameter("id")?.toLongOrNull()
                    ?: url.queryParameter("aid")?.toLongOrNull(),
                url.queryParameter("c")?.takeIf { it.isNotBlank() },
                enc
            )
        }
    }

    class QRCodeParseException(val rawValue: String, throwable: Throwable? = null) :
        ChaoxingParseDataException("二维码解析失败", throwable, rawValue)

    class QRCodeExpiredException(throwable: Throwable? = null) :
        ChaoxingParseDataException("二维码已过期", throwable)

    suspend fun isQRCodeExpired(parseResult: ChaoxingQRCodeParseResult): Boolean? {
        val code = parseResult.code ?: return null
        val activePrimaryId = parseResult.aid ?: activeId
        return withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(
                    Request.Builder().get().url(
                        URL_SIGN_DETAIL.newBuilder()
                            .addQueryParameter("activePrimaryId", activePrimaryId.toString())
                            .addQueryParameter("type", "1")
                            .addQueryParameter("msg", code)
                            .build()
                    ).build()
                ).execute().use {
                    it.checkResponseThrowException()
                    val body = it.body.string()
                    Log.d("ChaoxingQRCodeSigner", "signDetail: $body")
                    val result = JSONObject.parseObject(body)
                    result.getIntValue("isOver") == 1 || result.getString("signCode") != code
                }
            }.getOrNull()
        }
    }

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
        parseResult: ChaoxingQRCodeParseResult,
        position: ChaoxingLocationSignEntity?,
        faceImageObjectId: String? = null,
        context: Context,
        captchaValidate: String? = null
    ): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN_NO_PARAMETER.newBuilder()
                        .addQueryParameter("enc", parseResult.enc)
                        .addQueryParameter("name", client.name)
                        .addQueryParameter(
                            "activeId", (parseResult.aid ?: activeId).toString()
                        )
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

    @Deprecated(
        "Use sign instead",
        ReplaceWith("sign(parseResult, position, faceImageObjectId, context, captchaValidate)")
    )
    suspend fun signWithCaptcha(
        parseResult: ChaoxingQRCodeParseResult,
        position: ChaoxingLocationSignEntity?,
        captchaValidate: String,
        faceImageObjectId: String? = null,
        context: Context
    ) = sign(parseResult, position, faceImageObjectId, context, captchaValidate)

    override val notSignedPageMarkers: List<String> = listOf("扫一扫")
}
