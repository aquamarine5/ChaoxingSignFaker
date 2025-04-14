/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.util.Log
import com.alibaba.fastjson2.JSONObject
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingQRCodeDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.ChaoxingLocationSignException

class ChaoxingQRCodeSigner(
    client: ChaoxingHttpClient,
    qrCodeActivityEntity: QRCodeSignDestination
) : ChaoxingSigner(
    client,
    qrCodeActivityEntity.activeId,
    qrCodeActivityEntity.classId,
    qrCodeActivityEntity.courseId,
    qrCodeActivityEntity.extContent
) {
    companion object {
        const val CLASSTAG = "ChaoxingQRCodeSigner"
    }

    suspend fun getQRCodeSignInfo(): ChaoxingQRCodeDetailEntity {
        return getSignInfo().run {
            ChaoxingQRCodeDetailEntity(
                getInteger("ifopenAddress") == 1,
                getInteger("ifrefreshewm") == 1
            )
        }
    }

    suspend fun sign(enc: String, position: ChaoxingLocationSignEntity?) =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN.toHttpUrl().newBuilder()
                        .addQueryParameter("enc", enc)
                        .addQueryParameter("latitude", "-1")
                        .addQueryParameter("longitude", "-1")
                        .addQueryParameter("activeId", activeId.toString())
                        .addQueryParameter("uid", client.userEntity.puid.toString())
                        .addQueryParameter("name", client.userEntity.name)
                        .addQueryParameter("fid", client.userEntity.fid.toString())
                        .addQueryParameter("deviceCode", ChaoxingHttpClient.deviceCode!!)
                        .apply {
                            if (position != null) {
                                addQueryParameter(
                                    "location", JSONObject()
                                        .fluentPut("result", 1)
                                        .fluentPut("latitude", "%.6f".format(position.latitude))
                                        .fluentPut("longitude", "%.6f".format(position.longitude))
                                        .fluentPut("address", position.address)
                                        .fluentPut(
                                            "mockData",
                                            "{\"strategy\":0,\"probability\":-1}"
                                        )
                                        .toString()
                                )
                            }
                        }.build()
                ).build()
            ).execute().use {
                val result = it.body?.string()
                if (result != "success") {
                    Log.w(CLASSTAG, result ?: "")
                    throw ChaoxingLocationSignException(result ?: "签到失败")
                }
            }
        }

    fun parseQRCode(qrcode: Barcode): String =
        qrcode.url!!.url!!.toHttpUrl().queryParameter("enc")!!

    override suspend fun checkAlreadySign(response: String): Boolean =
        response.contains("扫一扫").not()

}