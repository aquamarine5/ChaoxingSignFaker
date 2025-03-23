/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import com.google.mlkit.vision.barcode.common.Barcode
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper

data class ChaoxingOtherUserSharedEntity(
    val phoneNumber: String,
    val encryptedPassword: String,
    val userName: String
) {
    companion object {
        fun parseFromQRCode(qrcode: Barcode): ChaoxingOtherUserSharedEntity {
            if (qrcode.url == null)
                throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("QRCode is not a URL")
            return runCatching {
                val url = qrcode.url!!.url!!.toHttpUrl()
                val phoneNumber = url.queryParameter("phone")!!
                val password = url.queryParameter("pwd")!!
                val userName = url.queryParameter("name")!!
                ChaoxingOtherUserSharedEntity(
                    phoneNumber,
                    password,
                    userName
                )
            }.getOrElse {
                throw ChaoxingOtherUserHelper.NotAvailableQRCodeException("QRCode is not a valid Chaoxing QRCode")
            }
        }
    }
}
