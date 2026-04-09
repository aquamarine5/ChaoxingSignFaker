/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner.ChaoxingPhotoSignException
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner.Companion.URL_CLOUD_UPLOAD
import java.io.ByteArrayOutputStream
import java.util.UUID

object ChaoxingCloudDriveHelper {
    val URL_CLOUD_TOKEN = "https://pan-yz.chaoxing.com/api/token/uservalid".toHttpUrl()

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

    suspend fun getCloudToken(client: ChaoxingHttpClient): String = withContext(Dispatchers.IO) {
        client.storageCloudToken?.let { return@withContext it }
        client.newCall(Request.Builder().url(URL_CLOUD_TOKEN).build()).execute().use {
            it.checkResponseThrowException()
            return@withContext JSONObject.parseObject(it.body.string()).getString("_token").apply {
                client.storageCloudToken = this
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

    suspend fun uploadImage(client: ChaoxingHttpClient, image: Bitmap): String =
        withContext(Dispatchers.IO) {
            val filename = "${UUID.randomUUID()}.jpg"
            ByteArrayOutputStream().use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 50, out)
                client.newCall(
                    Request.Builder().url(URL_CLOUD_UPLOAD + getCloudToken(client)).post(
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

    suspend fun uploadImage(
        client: ChaoxingHttpClient,
        context: Context,
        uri: Uri
    ): String =
        withContext(Dispatchers.IO) {
            val filename = "${UUID.randomUUID()}.jpg"
            client.newCall(
                Request.Builder().url(URL_CLOUD_UPLOAD + getCloudToken(client)).post(
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


}