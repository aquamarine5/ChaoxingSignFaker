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
import okhttp3.RequestBody.Companion.asRequestBody
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner.Companion.CLASSTAG
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

typealias ChaoxingPhotoActivityEntity = PhotoSignDestination

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

    class ChaoxingIncorrectSignTypeException() :
        ChaoxingPredictableException("签到类型不匹配，应是图片签到")

    companion object {
        const val URL_CLOUD_TOKEN = "https://pan-yz.chaoxing.com/api/token/uservalid"
        const val URL_CLOUD_UPLOAD = "https://pan-yz.chaoxing.com/upload?_from=mobilelearn&_token="
    }

    override suspend fun checkAlreadySign(response: String): Boolean {
        return response.contains("请先拍照").not() &&
                response.contains("<div class=\"zactives-btn\" onclick=\"send()\">").not()
    }

    suspend fun getCloudToken(): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(URL_CLOUD_TOKEN).build()).execute().use {
            return@withContext JSONObject.parseObject(it.body!!.string()).getString("_token")
        }
    }

    suspend fun signByClick() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", ChaoxingHttpClient.deviceCode)
                    .build()
            ).get().build()
        ).execute().use {
            val result = it.body?.string()
            if (result != "success") {
                Log.w(CLASSTAG, result ?: "")
                throw ChaoxingPhotoSignException(result ?: "签到失败")
            }
        }
    }

    suspend fun signByImage(objectId: String) = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("objectId", objectId)
                    .addQueryParameter("activeId", photoActivityEntity.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", ChaoxingHttpClient.deviceCode)
                    .build()
            ).get().build()
        ).execute().use {
            val result = it.body?.string()
            if (result != "success") {
                Log.w(CLASSTAG, result ?: "")
                throw ChaoxingPhotoSignException(result ?: "签到失败")
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri, fileName: String): File {
        val contentResolver = context.contentResolver
        val file = File(context.cacheDir, fileName)
        return runCatching {
            contentResolver.openInputStream(uri)?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                FileOutputStream(file).use { output ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        50,
                        output
                    )
                }
            }
            return@runCatching file
        }.getOrElse { throw ChaoxingPhotoSignException("文件转换失败") }
    }

    suspend fun uploadImage(context: Context, image: Bitmap, token: String): String =
        withContext(Dispatchers.IO) {
            val filename = "${UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, filename)
            file.outputStream().use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 50, out)
            }
            client.newCall(
                Request.Builder().url(URL_CLOUD_UPLOAD + token).post(
                    MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("puid", client.userEntity.puid.toString())
                        .addFormDataPart(
                            "file",
                            filename,
                            file.asRequestBody("image/jpeg".toMediaType())
                        )
                        .build()
                ).build()
            ).execute().use {
                JSONObject.parseObject(it.body!!.string()).getString("objectId")
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
                                uri,
                                filename
                            ).asRequestBody("image/jpeg".toMediaType())
                        )
                        .build()
                ).build()
            ).execute().use {
                JSONObject.parseObject(it.body!!.string()).getString("objectId")
            }
        }

    suspend fun ifPhotoRequiredLogin(): Boolean = getSignInfo().getInteger("ifphoto") == 1

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