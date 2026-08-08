/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import com.alibaba.fastjson2.JSONObject
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingFaceRecognitionConfigure
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingFaceRecognitionImage
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.StoredData
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.storedData
import java.io.File
import java.security.MessageDigest
import java.util.TreeMap

object ChaoxingFaceHelper {
    val storedFaceRecognitionImages: StoredData<Context, Map<String, List<ChaoxingFaceRecognitionImage>>> =
        storedData { context ->
            context.chaoxingDataStore.data.first().faceRecognitionConfiguresMap
                .mapValues { it.value.imagesList }
        }

    const val URL_SHARED_IMAGE = "https://p.cldisk.com/star4/%s/origin.jpg"
    private val URL_CHECK_FACE_RESULT =
        "https://mobilelearn.chaoxing.com/pptSign/check-face-result?DB_STRATEGY=PRIMARY_KEY&STRATEGY_PARA=activeId".toHttpUrl()

    const val MAX_FACE_IMAGES = 3

    suspend fun checkFaceResultAndGetEnc(
        client: ChaoxingHttpClient,
        objectId: String,
        activeId: Long
    ): String =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_CHECK_FACE_RESULT.newBuilder()
                        .addQueryParameter("activeId", activeId.toString())
                        .addQueryParameter(
                            "faceResult",
                            buildFaceResult(client, objectId).toJSONString()
                        )
                        .build()
                ).get().build()
            ).execute().use { response ->
                response.checkResponseThrowException()
                val jsonObject = JSONObject.parseObject(response.body.string())
                return@withContext jsonObject.getString("enc") ?: throw ChaoxingParseDataException(
                    "获取faceEnc失败",
                    data = jsonObject.toJSONString()
                )
            }
        }

    private fun buildFaceResult(client: ChaoxingHttpClient, objectId: String): JSONObject {
        val fields = mapOf(
            "currentFaceId" to objectId,
            "LiveDetectionStatus" to "1",
            "collectStatus" to "1"
        )
        val cxtime = System.currentTimeMillis().toString()
        return JSONObject()
            .fluentPut("currentFaceId", objectId)
            .fluentPut("LiveDetectionStatus", 1)
            .fluentPut("collectStatus", 1)
            .fluentPut("cxtime", cxtime)
            .apply {
                runCatching {
                    client.userEntity.clientId?.let { clientId ->
                        addSignToken(clientId, fields, cxtime)
                    }
                }.onFailure {
                    Sentry.captureException(it)
                }
            }
    }

    private fun JSONObject.addSignToken(
        clientId: String,
        fields: Map<String, String>,
        cxtime: String
    ) {
        val deviceInfo = ChaoxingDeviceInfoHelper.decryptClientId(clientId) ?: return
        val cxcid = deviceInfo.getString("cid") ?: return
        val sc = deviceInfo.getString("sc") ?: return
        val signedFields = TreeMap<String, String>().apply {
            putAll(fields)
            put("cxtime", cxtime)
            put("cxcid", cxcid)
        }
        val raw = buildString {
            signedFields.forEach { (key, value) ->
                append(key)
                append(value)
            }
            append(sc)
        }
        fluentPut("cxcid", cxcid)
        fluentPut("signToken", md5(raw))
    }

    private fun md5(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }


    fun getFaceImageFile(context: Context, objectId: String): File {
        require(objectId.isNotBlank()) { "人脸照片 ID 不能为空" }
        return File(context.filesDir, "face_images").apply { mkdirs() }
            .resolve("$objectId.jpg")
    }

    suspend fun saveFaceImage(
        client: ChaoxingHttpClient,
        context: Context,
        objectId: String,
        phoneNumber: String? = null,
    ): ChaoxingFaceRecognitionImage =
        saveFaceImage(client.okHttpClient, client.userEntity, context, objectId, phoneNumber)

    suspend fun saveFaceImage(
        okHttpClient: OkHttpClient,
        userEntity: ChaoxingUserEntity,
        context: Context,
        objectId: String,
        phoneNumber: String? = null,
    ): ChaoxingFaceRecognitionImage = withContext(Dispatchers.IO) {
        require(objectId.isNotBlank()) { "人脸照片 ID 不能为空" }
        val targetPhoneNumber = phoneNumber ?: userEntity.phoneNumber
        require(targetPhoneNumber.isNotBlank()) { "无法确定人脸照片所属用户" }
        val image = ChaoxingFaceRecognitionImage.newBuilder()
            .setObjectId(objectId)
            .setUseCount(0)
            .setIsFailureBefore(false)
            .build()
        val destination = getFaceImageFile(context, objectId)
        val temporary = File(destination.parentFile, "${destination.name}.download")

        runCatching {
            okHttpClient.newCall(Request.Builder().url(URL_SHARED_IMAGE.format(objectId)).build())
                .execute().use { response ->
                    response.checkResponseThrowException()
                    response.body.byteStream().use { input ->
                        temporary.outputStream().use(input::copyTo)
                    }
                }
            check(temporary.length() > 0L) { "下载的人脸照片为空" }
            check(temporary.renameTo(destination)) { "保存人脸照片失败" }
            context.chaoxingDataStore.updateData { dataStore ->
                val configure = dataStore.faceRecognitionConfiguresMap[targetPhoneNumber]
                    ?: ChaoxingFaceRecognitionConfigure.getDefaultInstance()
                check(configure.imagesCount < MAX_FACE_IMAGES) { "最多只能保存$MAX_FACE_IMAGES 张人脸照片" }
                check(configure.imagesList.none { it.objectId == objectId }) { "该人脸照片已保存" }
                dataStore.toBuilder()
                    .putFaceRecognitionConfigures(
                        targetPhoneNumber,
                        configure.toBuilder().addImages(image).build(),
                    )
                    .build()
            }
            storedFaceRecognitionImages.setValue(
                storedFaceRecognitionImages.getValue(context).toMutableMap().apply {
                    put(targetPhoneNumber, this[targetPhoneNumber].orEmpty() + image)
                }
            )
            image
        }.onFailure {
            temporary.delete()
            destination.delete()
        }.getOrThrow()
    }

    suspend fun saveFaceImage(
        client: ChaoxingHttpClient,
        context: Context,
        bitmap: Bitmap,
        phoneNumber: String? = null,
    ): ChaoxingFaceRecognitionImage = withContext(Dispatchers.IO) {
        val targetPhoneNumber = phoneNumber ?: client.userEntity.phoneNumber
        require(targetPhoneNumber.isNotBlank()) { "无法确定人脸照片所属用户" }
        val objectId = ChaoxingCloudDriveHelper.uploadImage(client, bitmap)
        val image = ChaoxingFaceRecognitionImage.newBuilder()
            .setObjectId(objectId)
            .setUseCount(0)
            .setIsFailureBefore(false)
            .build()
        val destination = getFaceImageFile(context, objectId)
        val temporary = File(destination.parentFile, "${destination.name}.tmp")

        runCatching {
            temporary.outputStream().use { output ->
                check(
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        90,
                        output
                    )
                ) { "保存人脸照片失败" }
            }
            check(temporary.renameTo(destination)) { "保存人脸照片失败" }
            context.chaoxingDataStore.updateData { dataStore ->
                val configure = dataStore.faceRecognitionConfiguresMap[targetPhoneNumber]
                    ?: ChaoxingFaceRecognitionConfigure.getDefaultInstance()
                check(configure.imagesCount < MAX_FACE_IMAGES) { "最多只能保存$MAX_FACE_IMAGES 张人脸照片" }
                check(configure.imagesList.none { it.objectId == objectId }) { "该人脸照片已保存" }
                dataStore.toBuilder()
                    .putFaceRecognitionConfigures(
                        targetPhoneNumber,
                        configure.toBuilder().addImages(image).build(),
                    )
                    .build()
            }
            storedFaceRecognitionImages.setValue(
                storedFaceRecognitionImages.getValue(context).toMutableMap().apply {
                    put(targetPhoneNumber, this[targetPhoneNumber].orEmpty() + image)
                }
            )
            image
        }.onFailure {
            temporary.delete()
            destination.delete()
        }.getOrThrow()
    }

    suspend fun deleteFaceImage(
        context: Context,
        phoneNumber: String,
        objectId: String,
    ) {
        context.chaoxingDataStore.updateData { dataStore ->
            val configure = dataStore.faceRecognitionConfiguresMap[phoneNumber]
                ?: return@updateData dataStore
            dataStore.toBuilder()
                .putFaceRecognitionConfigures(
                    phoneNumber,
                    configure.toBuilder()
                        .clearImages()
                        .addAllImages(configure.imagesList.filterNot { it.objectId == objectId })
                        .build(),
                )
                .build()
        }
        getFaceImageFile(context, objectId).delete()
        storedFaceRecognitionImages.peekValue()?.let { imagesByPhoneNumber ->
            storedFaceRecognitionImages.setValue(
                imagesByPhoneNumber.toMutableMap().apply {
                    this[phoneNumber] = this[phoneNumber].orEmpty()
                        .filterNot { it.objectId == objectId }
                },
            )
        }
    }

    suspend fun afterUsingFaceImage(
        context: Context,
        phoneNumber: String,
        objectId: String,
        isFailureBefore: Boolean,
    ) {
        context.chaoxingDataStore.updateData { dataStore ->
            val configure = dataStore.faceRecognitionConfiguresMap[phoneNumber]
                ?: return@updateData dataStore
            val images = configure.imagesList.map { image ->
                if (image.objectId == objectId) {
                    image.toBuilder()
                        .setUseCount(image.useCount + 1)
                        .setIsFailureBefore(image.isFailureBefore || isFailureBefore)
                        .build()
                } else image
            }
            dataStore.toBuilder()
                .putFaceRecognitionConfigures(
                    phoneNumber,
                    configure.toBuilder().clearImages().addAllImages(images).build(),
                )
                .build()
        }
        storedFaceRecognitionImages.peekValue()?.let { imagesByPhoneNumber ->
            storedFaceRecognitionImages.setValue(
                imagesByPhoneNumber.toMutableMap().apply {
                    this[phoneNumber] = this[phoneNumber].orEmpty().map { image ->
                        if (image.objectId == objectId) {
                            image.toBuilder()
                                .setUseCount(image.useCount + 1)
                                .setIsFailureBefore(image.isFailureBefore || isFailureBefore)
                                .build()
                        } else image
                    }
                },
            )
        }
    }
}
