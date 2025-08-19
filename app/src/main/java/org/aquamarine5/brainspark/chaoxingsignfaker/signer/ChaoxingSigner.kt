/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.annotation.SuppressLint
import android.content.Context
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
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.checkResponse
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCaptchaDataEntity

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
        const val URL_SIGN =
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&validate=&vpProbability=-1&vpStrategy="
    }

    class SignActivityNoPermissionException : ChaoxingPredictableException("无权限访问")

    class AlreadySignedException : ChaoxingPredictableException("已经签到过了")

    class CaptchaTimeoutException : ChaoxingPredictableException("获取验证码信息超时")

    class CaptchaException : ChaoxingPredictableException("验证码获取失败")

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
            return@withContext JSONObject.parseObject(it.body?.string()).getJSONObject("data")
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
            val body = it.body?.string()
            if (it.code == 302 || body?.contains("校验失败，未查询到活动数据") == true) {
                throw SignActivityNoPermissionException()
            }
            postAnalysis()
            return@withContext checkAlreadySign(body ?: "")
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
                    .find(it.body?.string() ?: throw Exception("网络错误"))?.groupValues?.get(1)
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
                    .addQueryParameter("version", dataEntity.version)
                    .addQueryParameter("t", "a")
                    .addQueryParameter("runEnv", "10")
                    .addQueryParameter("token", dataEntity.token)
                    .addQueryParameter("textClickArr", "[{\"x\":${xPosition.toInt()}}]")
                    .addQueryParameter("iv", dataEntity.iv)
                    .addQueryParameter("_", System.currentTimeMillis().toString())
                    .addQueryParameter("coordinate", "[]")
                    .build()
            ).build()
        ).execute().use {
            if (it.checkResponse(client.context)) {
                throw ChaoxingHttpClient.ChaoxingNetworkException()
            }
            val jsonResult = JSONObject.parseObject(
                it.body?.string()?.replace("cx_captcha_function(", "")?.replace(")", "")
            )
            if (jsonResult.getBoolean("result")) {
                return@use JSONObject.parseObject(
                    jsonResult.getString("extraData").replace("\\\"", "\"")
                ).getString("validate")
            } else {
                return@use null
            }
        }
    }

    open suspend fun getCaptchaConf() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_CAPTCHA_CONF.toHttpUrl().newBuilder()
                    .addQueryParameter("captchaId", getCaptchaId())
                    .addQueryParameter("_", System.currentTimeMillis().toString()).build()
            ).build()
        ).execute().close()
    }

    open suspend fun getCaptchaImage(onSuccess: (ChaoxingCaptchaDataEntity) -> Unit) {
        getCaptchaData(client.context) {
            client.newCall(
                Request.Builder().get().url(it).build()
            ).execute().use { response ->
                val jsonResult = JSONObject.parseObject(
                    response.body?.string()?.replace("cx_captcha_function(", "")?.replace(")", "")
                )
                val params = it.toHttpUrl()
                onSuccess(
                    ChaoxingCaptchaDataEntity(
                        params.queryParameter("captchaId") ?: getCaptchaId(),
                        "slide",
                        "1.1.20",
                        jsonResult.getString("token"),
                        params.queryParameter("captchaKey") ?: throw CaptchaException(),
                        jsonResult.getString("iv") ?: throw CaptchaException(),
                        jsonResult.getJSONObject("imageVerificationVo").getString("shadeImage")
                            ?: throw CaptchaException(),
                        jsonResult.getJSONObject("imageVerificationVo").getString("cutoutImage")
                            ?: throw CaptchaException()
                    )
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    open suspend fun getCaptchaData(
        context: Context,
        onSuccess: (String) -> Unit
    ) {
        val webview = WebView(context).apply {
            settings.javaScriptEnabled = true
        }
        coroutineScope {
            val job =
                launch {
                    webview.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            if (request?.url?.toString()
                                    ?.startsWith("https://captcha.chaoxing.com/captcha/get/verification/image") == true
                            ) {
                                onSuccess(request.url?.toString()!!)
                                view?.destroy()
                                cancel()
                                return null
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    webview.loadUrl(
                        URL_PERSIGN.toHttpUrl().newBuilder()
                            .addQueryParameter("courseId", courseId.toString())
                            .addQueryParameter("classId", classId.toString())
                            .addQueryParameter("activePrimaryId", activeId.toString())
                            .addQueryParameter("uid", client.userEntity.puid.toString())
                            .build().toString()
                    )
                }
            delay(10000)
            if (job.isActive) {
                job.cancel()
                webview.destroy()
                throw CaptchaTimeoutException()
            }
        }
    }
}
