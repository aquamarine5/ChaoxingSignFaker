/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
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
import okhttp3.OkHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingUserAgent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.HttpCookie
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingImportOtherUserResultStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ImportOtherUserResult
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import kotlin.time.Duration.Companion.milliseconds

object ChaoxingOtherUserHelper {
    val TIMEOUT_NEXT_SIGN = 200.milliseconds

    class NotAvailableQRCodeException(message: String) : ChaoxingPredictableException(message)

    class AlreadyExistedOtherUserException(message: String) : ChaoxingPredictableException(message)

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
        insertSharedEntity: ChaoxingOtherUserSharedEntity? = null,
        selectedFaceObjectIds: List<String>? = null,
    ): String =
        withContext(Dispatchers.IO) {
            val dataStore = context.chaoxingDataStore.data.first()
            val sharedEntity = insertSharedEntity ?: getSharedUserEntity(dataStore)
            val availableFaceObjectIds =
                dataStore.faceRecognitionConfiguresMap[sharedEntity.phoneNumber]
                    ?.imagesList
                    .orEmpty()
                    .map { it.objectId }
            val faceObjectIds = (selectedFaceObjectIds
                ?.distinct()
                ?.filter { it in availableFaceObjectIds }
                ?: availableFaceObjectIds).take(ChaoxingFaceHelper.MAX_FACE_IMAGES)
            "http://cdn.aquamarine5.fun/?phone=${sharedEntity.phoneNumber}&pwd=${sharedEntity.encryptedPassword}&name=${
                Uri.encode(sharedEntity.userName)
            }&face=${faceObjectIds.joinToString(",")}"
        }

    suspend fun generateQRCode(
        context: Context,
        insertSharedEntity: ChaoxingOtherUserSharedEntity? = null,
        selectedFaceObjectIds: List<String>? = null,
    ): Bitmap = withContext(Dispatchers.Default) {
        val qrcodeSize = getQRCodeSize(context)
        val qrCode = QRCodeWriter().encode(
            getSharedUrl(context, insertSharedEntity, selectedFaceObjectIds),
            BarcodeFormat.QR_CODE,
            qrcodeSize,
            qrcodeSize,
            mapOf(
                EncodeHintType.CHARACTER_SET to "utf-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
        )
        return@withContext createBitmap(qrcodeSize, qrcodeSize, Bitmap.Config.RGB_565)
            .apply {
                for (x in 0 until qrcodeSize) {
                    for (y in 0 until qrcodeSize) {
                        set(x, y, (if (qrCode[x, y]) 0x000000 else 0xFFFFFF))
                    }
                }
            }
    }

    suspend fun repairOtherUserSession(
        context: Context,
        session: ChaoxingOtherUserSession,
        password: String
    ): ChaoxingOtherUserSession =
        withContext(Dispatchers.IO) {
            val tempOkHttpClient =
                (ChaoxingHttpClient.instance?.okHttpClient ?: OkHttpClient()).newBuilder()
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
                    }).addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", chaoxingUserAgent).build()
                        )
                    }.retryOnConnectionFailure(true).build()

            ChaoxingHttpClient.login(
                tempOkHttpClient,
                session.phoneNumber,
                password,
                context,
                isSaveToDataStore = false,
                isEncryptedPassword = false
            )
            val newSession = session.toBuilder()
                .setPassword(password.replace(" ", "+"))
                .setIsObsoleteSession(false)
                .clearCookies().addAllCookies(
                    tempOkHttpClient.cookieJar.loadForRequest(
                        HttpUrl.Builder()
                            .scheme("https")
                            .host("chaoxing.com").build()
                    ).map { cookie ->
                        HttpCookie.newBuilder()
                            .setValue(cookie.value)
                            .setName(cookie.name)
                            .setHost(cookie.domain).build()
                    }
                ).build()
            context.chaoxingDataStore.updateData { datastore ->
                val index =
                    datastore.otherUsersList.indexOfFirst { it.phoneNumber == session.phoneNumber }
                if (index == -1) return@updateData datastore
                datastore.toBuilder().removeOtherUsers(index).addOtherUsers(index, newSession)
                    .build()
            }
            return@withContext newSession
        }

    suspend fun saveOtherUser(
        context: Context,
        sharedEntity: ChaoxingOtherUserSharedEntity
    ): ImportOtherUserResult =
        withContext(Dispatchers.IO) {
            val dataStore = context.chaoxingDataStore.data.first()
            if (dataStore.loginSession.phoneNumber == sharedEntity.phoneNumber)
                throw AlreadyExistedOtherUserException("自己不能添加自己！")
            val existedSession =
                dataStore.otherUsersList.firstOrNull { it.phoneNumber == sharedEntity.phoneNumber }

            suspend fun saveFaceImages(okHttpClient: OkHttpClient, userEntity: ChaoxingUserEntity) {
                if (sharedEntity.faceObjectIds.isEmpty()) return
                val configure = context.chaoxingDataStore.data.first()
                    .faceRecognitionConfiguresMap[sharedEntity.phoneNumber]
                val existingObjectIds = configure?.imagesList.orEmpty().mapTo(mutableSetOf()) {
                    it.objectId
                }
                val availableNewImageCount =
                    (ChaoxingFaceHelper.MAX_FACE_IMAGES - (configure?.imagesCount ?: 0))
                        .coerceAtLeast(0)
                var newImageCount = 0

                sharedEntity.faceObjectIds
                    .asSequence()
                    .filter { it.isNotBlank() }
                    .distinct()
                    .filter { objectId ->
                        val imageFile = ChaoxingFaceHelper.getFaceImageFile(context, objectId)
                        val hasUsableLocalImage =
                            objectId in existingObjectIds && imageFile.isFile && imageFile.length() > 0L
                        when {
                            hasUsableLocalImage -> false
                            objectId in existingObjectIds -> true
                            newImageCount < availableNewImageCount -> {
                                newImageCount++
                                true
                            }

                            else -> false
                        }
                    }
                    .forEach { objectId ->
                        ChaoxingFaceHelper.saveFaceImage(
                            okHttpClient,
                            userEntity,
                            context,
                            objectId,
                            sharedEntity.phoneNumber,
                        )
                    }
            }

            if (existedSession != null && existedSession.password == sharedEntity.encryptedPassword) {
                if (sharedEntity.faceObjectIds.isEmpty())
                    throw AlreadyExistedOtherUserException(
                        "${sharedEntity.userName}(${sharedEntity.phoneNumber}) 用户已经存在！"
                    )
                if (sharedEntity.faceObjectIds.all { localObjectId ->
                        val existsInDataStore =
                            dataStore.faceRecognitionConfiguresMap[sharedEntity.phoneNumber]
                                ?.imagesList
                                ?.any { it.objectId == localObjectId }
                                ?: false
                        val imageFile = ChaoxingFaceHelper.getFaceImageFile(context, localObjectId)
                        existsInDataStore && imageFile.isFile && imageFile.length() > 0L
                    }) {
                    throw AlreadyExistedOtherUserException(
                        "${sharedEntity.userName}(${sharedEntity.phoneNumber}) 用户已经存在！"
                    )
                }
                val faceClient =
                    ChaoxingHttpClient.loadFromOtherUserSession(existedSession, context)
                saveFaceImages(faceClient.okHttpClient, faceClient.userEntity)
                return@withContext Triple(
                    ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_FACE_IMAGES,
                    existedSession.name,
                    existedSession,
                )
            }

            val tempOkHttpClient =
                (ChaoxingHttpClient.instance?.okHttpClient ?: OkHttpClient()).newBuilder()
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
                    }).addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", chaoxingUserAgent).build()
                        )
                    }.retryOnConnectionFailure(true).build()

            ChaoxingHttpClient.login(
                tempOkHttpClient,
                sharedEntity.phoneNumber,
                sharedEntity.encryptedPassword,
                context,
                isSaveToDataStore = false,
                isEncryptedPassword = true
            )
            val userEntity =
                ChaoxingHttpClient.getInfo(tempOkHttpClient, context, sharedEntity.phoneNumber)

            val session = ChaoxingOtherUserSession.newBuilder()
                .setPassword(sharedEntity.encryptedPassword.replace(" ", "+"))
                .setName(sharedEntity.userName.ifEmpty { userEntity.name })
                .setPhoneNumber(sharedEntity.phoneNumber)
                .addAllCookies(
                    tempOkHttpClient.cookieJar.loadForRequest(
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

            if (existedSession == null) {
                context.chaoxingDataStore.updateData { datastore ->
                    if (datastore.otherUsersList.any { it.phoneNumber == session.phoneNumber }) {
                        datastore
                    } else {
                        datastore.toBuilder().addOtherUsers(session).build()
                    }
                }
                saveFaceImages(tempOkHttpClient, userEntity)
                return@withContext Triple(
                    ChaoxingImportOtherUserResultStatus.SUCCESS,
                    session.name,
                    session,
                )
            }

            context.chaoxingDataStore.updateData { datastore ->
                val index =
                    datastore.otherUsersList.indexOfFirst { it.phoneNumber == session.phoneNumber }
                if (index == -1) return@updateData datastore
                datastore.toBuilder().removeOtherUsers(index).addOtherUsers(index, session).build()
            }
            saveFaceImages(tempOkHttpClient, userEntity)
            return@withContext Triple(
                ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_PASSWORD,
                session.name,
                session,
            )
        }

    suspend fun markSessionObsoleted(session: ChaoxingOtherUserSession, context: Context) =
        withContext(Dispatchers.IO) {
            context.chaoxingDataStore.updateData { datastore ->
                val updatedSessions = datastore.otherUsersList.map {
                    if (it.phoneNumber == session.phoneNumber) it.toBuilder()
                        .setIsObsoleteSession(true).build() else it
                }
                datastore.toBuilder().clearOtherUsers().addAllOtherUsers(updatedSessions).build()
            }
        }
}