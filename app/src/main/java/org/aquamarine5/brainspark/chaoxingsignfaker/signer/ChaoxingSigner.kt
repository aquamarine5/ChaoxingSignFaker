/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCaptchaDataEntity
import java.util.UUID

abstract class ChaoxingSigner(
    val client: ChaoxingHttpClient,
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        const val URL_PERSIGN =
            "https://mobilelearn.chaoxing.com/newsign/preSign?&general=1&sys=1&ls=1&appType=15&isTeacherViewOpen=0"
        const val URL_SIGN_INFO =
            "https://mobilelearn.chaoxing.com/v2/apis/active/getPPTActiveInfo"
        const val URL_ANALYSIS =
            "https://mobilelearn.chaoxing.com/pptSign/analysis?vs=1&DB_STRATEGY=RANDOM"
        const val URL_AFTER_ANALYSIS2 =
            "https://mobilelearn.chaoxing.com/pptSign/analysis2?DB_STRATEGY=RANDOM"
        const val URL_CAPTCHA_CONF =
            "https://captcha.chaoxing.com/captcha/get/conf?callback=cx_captcha_function"
        const val URL_CAPTCHA_RESULT =
            "https://captcha.chaoxing.com/captcha/check/verification/result?callback=cx_captcha_function"
        const val URL_CAPTCHA_IMAGE = "https://captcha.chaoxing.com/captcha/get/verification/image"
        const val URL_SIGN =
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&vpProbability=-1&vpStrategy="
    }

    class SignAlreadyEndedException : ChaoxingPredictableException("迟到或签到已结束")

    class SignActivityNoPermissionException : ChaoxingPredictableException("此用户不在班级")

    class AlreadySignedException : ChaoxingPredictableException("已经签到过了")

    class CaptchaTimeoutException : ChaoxingPredictableException("获取验证码信息超时")

    class CaptchaException : ChaoxingPredictableException("验证码获取失败")

    class CaptchaCheckException(message: String) :
        ChaoxingPredictableException("$message, 验证码校验失败")

    abstract suspend fun checkAlreadySign(response: String): Boolean

    open suspend fun getSignInfo(): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_SIGN_INFO.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            return@withContext JSONObject.parseObject(it.body.string()).getJSONObject("data")
        }
    }

    open suspend fun preSign(): Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().post(
                FormBody.Builder().addEncoded("ext", extContent).build()
            ).url(
                URL_PERSIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("courseId", courseId.toString())
                    .addQueryParameter("classId", classId.toString())
                    .addQueryParameter("activePrimaryId", activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            val body = it.body.string()
            if (it.code == 302 || body.contains("校验失败，未查询到活动数据")) {
                throw SignActivityNoPermissionException()
            }
            postAnalysis()
            return@withContext checkAlreadySign(body)
        }
    }

    open suspend fun postAnalysis() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_ANALYSIS.toHttpUrl().newBuilder()
                    .addQueryParameter("aid", activeId.toString()).build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            postAfterAnalysis(
                """code='\+'([a-f0-9]+)'""".toRegex()
                    .find(it.body.string())?.groupValues?.get(1)
                    ?: throw Exception("Cannot find code")
            )
        }
    }

    open suspend fun postAfterAnalysis(code: String) = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_AFTER_ANALYSIS2.toHttpUrl().newBuilder()
                    .addQueryParameter("code", code)
                    .build()
            ).build()
        ).execute().close()
    }

    open fun getCaptchaId(): String = "Qt9FIw9o4pwRjOyqM6yizZBh682qN2TU"

    open suspend fun checkCaptchaResult(
        xPosition: Float,
        dataEntity: ChaoxingCaptchaDataEntity
    ): String? = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_CAPTCHA_RESULT.toHttpUrl().newBuilder()
                    .addQueryParameter("captchaId", dataEntity.captchaId)
                    .addQueryParameter("type", dataEntity.type)
                    .addQueryParameter("token", dataEntity.token)
                    .addQueryParameter("textClickArr", "[{\"x\":${xPosition.toInt()}}]")
                    .addQueryParameter("coordinate", "[]")
                    .addQueryParameter("runEnv", "10")
                    .addQueryParameter("version", dataEntity.version)
                    .addQueryParameter("t", "a")
                    .addQueryParameter("iv", dataEntity.iv)
                    .addQueryParameter("_", System.currentTimeMillis().toString())
                    .build()
            ).header(
                "Referer", URL_PERSIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("courseId", courseId.toString())
                    .addQueryParameter("classId", classId.toString())
                    .addQueryParameter("activePrimaryId", activeId.toString())
                    .addQueryParameter("uid", client.userEntity.puid.toString())
                    .build().toString()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            val jsonResult = JSONObject.parseObject(
                it.body.string().replace("cx_captcha_function(", "").replace(")", "")
            )
            if (jsonResult.getInteger("error") == 1) {
                throw CaptchaCheckException(jsonResult.getString("msg"))
            }
            if (jsonResult.getBoolean("result")) {
                return@use JSONObject.parseObject(
                    jsonResult.getString("extraData").replace("\\\"", "\"")
                ).getString("validate")
            } else {
                return@use null
            }
        }
    }

    open suspend fun getCaptchaConf(): Long = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_CAPTCHA_CONF.toHttpUrl().newBuilder()
                    .addQueryParameter("captchaId", getCaptchaId())
                    .addQueryParameter("_", System.currentTimeMillis().toString()).build()
            ).build()
        ).execute().use {
            return@use JSONObject.parseObject(
                it.body.string().replace("cx_captcha_function(", "").replace(")", "")
            ).getLong("t")
        }
    }

    @Deprecated("Use getCaptchaImageV2 instead", ReplaceWith("getCaptchaImageV2()"))
    open suspend fun getCaptchaImage(onSuccess: (ChaoxingCaptchaDataEntity) -> Unit) {
        getCaptchaData(client.context) {
            client.newCall(
                Request.Builder().get().url(it).header(
                    "Referer", URL_PERSIGN.toHttpUrl().newBuilder()
                        .addQueryParameter("courseId", courseId.toString())
                        .addQueryParameter("classId", classId.toString())
                        .addQueryParameter("activePrimaryId", activeId.toString())
                        .addQueryParameter("uid", client.userEntity.puid.toString())
                        .build().toString()
                ).build()
            ).execute().use { response ->
                val jsonResult = JSONObject.parseObject(
                    response.body.string().replace("cx_captcha_function(", "").replace(")", "")
                )
                val params = it.toHttpUrl()
                onSuccess(
                    ChaoxingCaptchaDataEntity(
                        params.queryParameter("captchaId") ?: getCaptchaId(),
                        "slide",
                        "1.1.20",
                        jsonResult.getString("token"),
                        params.queryParameter("captchaKey") ?: throw CaptchaException(),
                        params.queryParameter("iv") ?: throw CaptchaException(),
                        jsonResult.getJSONObject("imageVerificationVo").getString("shadeImage")
                            ?: throw CaptchaException(),
                        jsonResult.getJSONObject("imageVerificationVo").getString("cutoutImage")
                            ?: throw CaptchaException()
                    )
                )
            }
        }
    }

    @Deprecated("Use getCaptchaImageV2 instead", ReplaceWith("getCaptchaImageV2()"))
    @SuppressLint("SetJavaScriptEnabled")
    open suspend fun getCaptchaData(
        context: Context,
        onSuccess: (String) -> Unit
    ) {
        getCaptchaConf()
        var webview: WebView? = WebView(context).apply {
            settings.javaScriptEnabled = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            removeAllCookies(null)
            client.okHttpClient.cookieJar.loadForRequest("https://chaoxing.com/get_cookies".toHttpUrl())
                .forEach {
                    setCookie(
                        "chaoxing.com",
                        "${it.name}=${it.value}; Path=/; Domain=chaoxing.com;"
                    )
                }
            flush()
        }

        coroutineScope {
            val job =
                launch {
                    webview?.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (request?.url?.toString()
                                    ?.contains("captcha.chaoxing.com/captcha/get/verification/image") == true
                            ) {
                                Log.d("ChaoxingSigner", "Captcha URL: ${request.url}")
                                onSuccess(request.url?.toString()!!)
                                cancel()
                                return null
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    webview?.loadUrl(
                        URL_PERSIGN.toHttpUrl().newBuilder()
                            .addQueryParameter("courseId", courseId.toString())
                            .addQueryParameter("classId", classId.toString())
                            .addQueryParameter("activePrimaryId", activeId.toString())
                            .addQueryParameter("uid", client.userEntity.puid.toString())
                            .build().toString()
                    )
                }
            job.invokeOnCompletion {
                if (job.isCancelled)
                    webview?.destroy()
                webview = null
            }
            delay(10000)
            if (job.isActive) {
                job.cancel()
                webview?.destroy()
                webview = null
                throw CaptchaTimeoutException()
            }
        }
    }

    open suspend fun getCaptchaImageV2(): ChaoxingCaptchaDataEntity = withContext(Dispatchers.IO) {
        val t = getCaptchaConf()
        val type = "slide"
        val captchaKey = UMengHelper.md5("$t${UUID.randomUUID()}")
        val iv =
            UMengHelper.md5("${getCaptchaId()}$type${System.currentTimeMillis()}${UUID.randomUUID()}")
        val token = UMengHelper.md5("$t${getCaptchaId()}$type$captchaKey") + ":${t + 300000L}"
        client.newCall(
            Request.Builder().get().url(
                URL_CAPTCHA_IMAGE.toHttpUrl().newBuilder()
                    .addQueryParameter("callback", "cx_captcha_function")
                    .addQueryParameter("captchaId", getCaptchaId())
                    .addQueryParameter("type", "slide")
                    .addQueryParameter("version", "1.1.20")
                    .addQueryParameter("captchaKey", captchaKey)
                    .addQueryParameter("token", token)
                    .addQueryParameter(
                        "referer", URL_PERSIGN.toHttpUrl().newBuilder()
                            .addQueryParameter("courseId", courseId.toString())
                            .addQueryParameter("classId", classId.toString())
                            .addQueryParameter("activePrimaryId", activeId.toString())
                            .addQueryParameter("uid", client.userEntity.puid.toString())
                            .build().toString()
                    )
                    .addQueryParameter("iv", iv)
                    .addQueryParameter("_", System.currentTimeMillis().toString())
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            val jsonResult = JSONObject.parseObject(
                it.body.string().replace("cx_captcha_function(", "").replace(")", "")
            )

            return@use ChaoxingCaptchaDataEntity(
                getCaptchaId(),
                "slide",
                "1.1.20",
                jsonResult.getString("token"),
                captchaKey,
                iv,
                jsonResult.getJSONObject("imageVerificationVo").getString("shadeImage")
                    ?: throw CaptchaException(),
                jsonResult.getJSONObject("imageVerificationVo").getString("cutoutImage")
                    ?: throw CaptchaException()
            )
        }
    }
}
