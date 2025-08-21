/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.HttpCookie
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

object ChaoxingOtherUserHelper {
    const val TIMEOUT_NEXT_SIGN = 200L

    class NotAvailableQRCodeException(message: String) : Exception(message)

    class AlreadyExistedOtherUserException(message: String) : Exception(message)

    private fun getQRCodeSize(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        val shorterSide = minOf(width, height)
        return (shorterSide * 0.73).toInt()
    }

    fun getQRCodeDpSize(context: Context): Dp {
        return (getQRCodeSize(context) / context.resources.displayMetrics.density).toInt().dp
    }

    fun checkSharedEntity(dataStore: ChaoxingSignFakerDataStore) =
        !dataStore.loginSession.password.isNullOrEmpty() && !dataStore.loginSession.phoneNumber.isNullOrEmpty()

    private fun getSharedUserEntity(dataStore: ChaoxingSignFakerDataStore): ChaoxingOtherUserSharedEntity {
        return ChaoxingOtherUserSharedEntity(
            dataStore.loginSession.phoneNumber!!,
            dataStore.loginSession.password!!,
            ChaoxingHttpClient.instance!!.userEntity.name
        )
    }

    suspend fun getSharedUrl(
        context: Context,
        insertSharedEntity: ChaoxingOtherUserSharedEntity? = null
    ): String =
        withContext(Dispatchers.IO) {
            val sharedEntity =
                insertSharedEntity ?: getSharedUserEntity(context.chaoxingDataStore.data.first())
            "http://cdn.aquamarine5.fun/?phone=${sharedEntity.phoneNumber}&pwd=${sharedEntity.encryptedPassword}&name=${
                Uri.encode(
                    sharedEntity.userName
                )
            }"
        }

    suspend fun generateQRCode(
        context: Context,
        insertSharedEntity: ChaoxingOtherUserSharedEntity? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val qrcodeSize = getQRCodeSize(context)
        val qrCode = QRCodeWriter().encode(
            getSharedUrl(context, insertSharedEntity),
            BarcodeFormat.QR_CODE,
            qrcodeSize,
            qrcodeSize,
            mapOf(
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

    suspend fun saveOtherUser(
        context: Context,
        sharedEntity: ChaoxingOtherUserSharedEntity
    ): ChaoxingOtherUserSession =
        withContext(Dispatchers.IO) {
            context.chaoxingDataStore.data.first().apply {
                if (loginSession.phoneNumber == sharedEntity.phoneNumber)
                    throw AlreadyExistedOtherUserException("自己不能添加自己！")
                if (otherUsersList.any { it.phoneNumber == sharedEntity.phoneNumber })
                    throw AlreadyExistedOtherUserException("${sharedEntity.userName}(${sharedEntity.phoneNumber}) 用户已经存在！")
            }

            val tempOkHttpClient =
                ChaoxingHttpClient.instance!!.okHttpClient.newBuilder()
                    .cookieJar(object : CookieJar {
                        private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                        private var chaoxingCookieSession: List<Cookie> = listOf()

                        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                            if (url.host.endsWith("chaoxing.com") && url.encodedPath == "/fanyalogin") {
                                chaoxingCookieSession = cookies
                            } else
                                cookieStore[url.host] = cookies
                        }

                        override fun loadForRequest(url: HttpUrl): List<Cookie> {
                            return if (url.host.endsWith("chaoxing.com")) {
                                chaoxingCookieSession
                            } else {
                                cookieStore[url.host] ?: listOf()
                            }
                        }
                    }).build()

            ChaoxingHttpClient.login(
                tempOkHttpClient,
                sharedEntity.phoneNumber,
                sharedEntity.encryptedPassword,
                context,
                isSaveToDataStore = false,
                isEncryptedPassword = true
            )

            val session = ChaoxingOtherUserSession.newBuilder()
                .setPassword(sharedEntity.encryptedPassword.replace(" ", "+"))
                .setName(sharedEntity.userName)
                .setPhoneNumber(sharedEntity.phoneNumber)
                .addAllCookies(tempOkHttpClient.cookieJar.loadForRequest(
                    HttpUrl.Builder()
                        .scheme("https")
                        .host("chaoxing.com").build()
                ).map { cookie ->
                    HttpCookie.newBuilder()
                        .setValue(cookie.value)
                        .setName(cookie.name)
                        .setHost(cookie.domain).build()
                })
                .build()
            context.chaoxingDataStore.updateData { datastore ->
                datastore.toBuilder().addOtherUsers(session).build()
            }
            return@withContext session
        }
}