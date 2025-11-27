/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper.NO_SIGN_OFF_EVENT
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.ChaoxingPhotoActivityEntity
import java.io.ByteArrayOutputStream
import java.util.UUID


class ChaoxingPhotoSigner(
    client: ChaoxingHttpClient,
    private val photoActivityEntity: ChaoxingPhotoActivityEntity
) : ChaoxingSigner(
    client,
    photoActivityEntity.activeId,
    photoActivityEntity.classId,
    photoActivityEntity.courseId,
    photoActivityEntity.extContent
) {

    class ChaoxingPhotoSignException(message: String) : ChaoxingPredictableException(message)

    class ChaoxingIncorrectSignTypeException :
        ChaoxingPredictableException("签到类型不匹配，应是图片签到")

    companion object {
        const val CLASSTAG = "ChaoxingPhotoSigner"
        const val URL_CLOUD_TOKEN = "https://pan-yz.chaoxing.com/api/token/uservalid"
        const val URL_CLOUD_UPLOAD = "https://pan-yz.chaoxing.com/upload?_from=mobilelearn&_token="
    }

    override suspend fun checkAlreadySign(response: String): Boolean {
        return response.contains("请先拍照").not() &&
                response.contains("<div class=\"zactives-btn\" onclick=\"send()\">").not()
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

    suspend fun getCloudToken(): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(URL_CLOUD_TOKEN).build()).execute().use {
            it.checkResponseThrowException()
            return@withContext JSONObject.parseObject(it.body.string()).getString("_token")
        }
    }

    suspend fun signByClick(): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", client.deviceCode)
                    .build()
            ).get().build()
        ).execute().use {
            it.checkResponseThrowException()
            val result = it.body.string()
            if (result == "您已签到过了") {
                throw AlreadySignedException()
            }
            if (result == "success2")
                throw SignAlreadyEndedException()
            if (result == "validate") {
                return@use true
            }
            if (result != "success") {
                Log.w(CLASSTAG, result)
                throw ChaoxingPhotoSignException(result)
            } else return@use false
        }
    }

    suspend fun signByImage(objectId: String): Boolean =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN.toHttpUrl().newBuilder()
                        .addQueryParameter("objectId", objectId)
                        .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                        .addQueryParameter("uid", client.userEntity.puid.toString())
                        .addQueryParameter("name", client.userEntity.name)
                        .addQueryParameter("fid", client.userEntity.fid.toString())
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .build()
                ).get().build()
            ).execute().use {
                it.checkResponseThrowException()
                val result = it.body.string()
                if (result == "您已签到过了") {
                    throw AlreadySignedException()
                }
                if (result == "success2")
                    throw SignAlreadyEndedException()
                if (result == "validate") {
                    return@use true
                }
                if (result != "success") {
                    Log.w(CLASSTAG, result)
                    throw ChaoxingPhotoSignException(result)
                } else return@use false
            }
        }

    suspend fun signByClickWithCaptcha(validateValue: String) = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", client.deviceCode)
                    .addQueryParameter("validate", validateValue)
                    .build()
            ).get().build()
        ).execute().use {
            it.checkResponseThrowException()
            val result = it.body.string()
            if (result == "success2")
                throw SignAlreadyEndedException()
            if (result == "您已签到过了") {
                throw AlreadySignedException()
            }
            if (result != "success") {
                Log.w(CLASSTAG, result)
                throw ChaoxingPhotoSignException(result)
            }
        }
    }

    suspend fun signByImageWithCaptcha(objectId: String, validateValue: String) =
        withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder().url(
                    URL_SIGN.toHttpUrl().newBuilder()
                        .addQueryParameter("objectId", objectId)
                        .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                        .addQueryParameter("uid", client.userEntity.puid.toString())
                        .addQueryParameter("name", client.userEntity.name)
                        .addQueryParameter("fid", client.userEntity.fid.toString())
                        .addQueryParameter("deviceCode", client.deviceCode)
                        .addQueryParameter("validate", validateValue)
                        .build()
                ).get().build()
            ).execute().use {
                it.checkResponseThrowException()
                val result = it.body.string()
                if (result == "success2")
                    throw SignAlreadyEndedException()
                if (result == "您已签到过了") {
                    throw AlreadySignedException()
                }
                if (result != "success") {
                    Log.w(CLASSTAG, result)
                    throw ChaoxingPhotoSignException(result)
                }
            }
        }

    private fun uriToFile(context: Context, uri: Uri): RequestBody {
        val contentResolver = context.contentResolver
        return runCatching {
            contentResolver.openInputStream(uri).use result@{
                val bitmap = BitmapFactory.decodeStream(it)
                ByteArrayOutputStream().use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        50, out
                    )
                    return@use out.toByteArray().toRequestBody("image/jpeg".toMediaType())
                }
            }
        }.getOrElse { throw ChaoxingPhotoSignException("文件转换失败") }
    }

    suspend fun uploadImage(image: Bitmap, token: String): String =
        withContext(Dispatchers.IO) {
            val filename = "${UUID.randomUUID()}.jpg"
            ByteArrayOutputStream().use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 50, out)
                client.newCall(
                    Request.Builder().url(URL_CLOUD_UPLOAD + token).post(
                        MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("puid", client.userEntity.puid.toString())
                            .addFormDataPart(
                                "file",
                                filename,
                                out.toByteArray().toRequestBody("image/jpeg".toMediaType())
                            )
                            .build()
                    ).build()
                ).execute().use {
                    it.checkResponseThrowException()
                    JSONObject.parseObject(it.body.string()).getString("objectId")
                }
            }
        }

    suspend fun uploadImage(context: Context, uri: Uri, token: String): String =
        withContext(Dispatchers.IO) {
            val filename = "${UUID.randomUUID()}.jpg"
            client.newCall(
                Request.Builder().url(URL_CLOUD_UPLOAD + token).post(
                    MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("puid", client.userEntity.puid.toString())
                        .addFormDataPart(
                            "file",
                            filename,
                            uriToFile(
                                context,
                                uri
                            )
                        )
                        .build()
                ).build()
            ).execute().use {
                it.checkResponseThrowException()
                JSONObject.parseObject(it.body.string()).getString("objectId")
            }
        }

    suspend fun ifPhotoRequiredLogin(): Pair<Boolean, ChaoxingSignOutEntity> {
        val json = getSignInfo()
        return Pair(json.getInteger("ifphoto") == 1, getSignoffEntity(json))
    }

    @Composable
    fun GetPhotoFromMediaStore(onResult: (Uri?) -> Unit) {
        val gallery = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                onResult(uri)
            }
        )
        LaunchedEffect(Unit) {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}