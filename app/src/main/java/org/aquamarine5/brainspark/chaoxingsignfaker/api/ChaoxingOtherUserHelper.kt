/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

object ChaoxingOtherUserHelper {

    private const val ALREADY_EXCEPTION_IS_SELF="@self"

    class NotAvailableQRCodeException(message: String) : Exception(message)

    class AlreadyExistedOtherUserException(message: String) : Exception(message){
        fun isSelf() = message == ALREADY_EXCEPTION_IS_SELF
    }

    private fun getQRCodeSize(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val shorterSide = minOf(width, height)
        return (shorterSide * 0.6).toInt()
    }

    fun getQRCodeDpSize(context: Context): Dp {
        return (getQRCodeSize(context) / context.resources.displayMetrics.density + 0.5f).toInt().dp
    }

    fun checkSharedEntity(dataStore: ChaoxingSignFakerDataStore) =
        dataStore.loginSession.password != null && dataStore.loginSession.phoneNumber != null

    private fun getSharedUserEntity(dataStore: ChaoxingSignFakerDataStore): ChaoxingOtherUserSharedEntity {
        return ChaoxingOtherUserSharedEntity(
            dataStore.loginSession.phoneNumber!!,
            dataStore.loginSession.password!!,
            ChaoxingHttpClient.instance!!.userEntity.name
        )
    }

    suspend fun generateQRCode(context: Context): Bitmap = withContext(Dispatchers.IO) {
        val qrcodeSize = getQRCodeSize(context)
        val sharedEntity = getSharedUserEntity(context.chaoxingDataStore.data.first())
        val content =
            "http://cdn.aquamarine5.fun/?phone=${sharedEntity.phoneNumber}&pwd=${sharedEntity.encryptedPassword}&name=${sharedEntity.userName}"
        val qrCode = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, qrcodeSize, qrcodeSize, mapOf(
                EncodeHintType.CHARACTER_SET to "utf-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
        )
        return@withContext Bitmap.createBitmap(qrcodeSize, qrcodeSize, Bitmap.Config.RGB_565)
            .apply {
                for (x in 0 until qrcodeSize) {
                    for (y in 0 until qrcodeSize) {
                        setPixel(x, y, (if (qrCode[x, y]) 0xFF000000 else 0xFFFFFFFF).toInt())
                    }
                }
            }
    }

    suspend fun saveOtherUser(context: Context, sharedEntity: ChaoxingOtherUserSharedEntity) {
        context.chaoxingDataStore.data.first().apply {
            if(loginSession.phoneNumber == sharedEntity.phoneNumber)
                throw AlreadyExistedOtherUserException(ALREADY_EXCEPTION_IS_SELF)
            if(otherUsersList.any { it.phoneNumber == sharedEntity.phoneNumber })
                throw AlreadyExistedOtherUserException(sharedEntity.phoneNumber)
        }

        context.chaoxingDataStore.updateData { datastore->
            TODO()
        }


    }
}