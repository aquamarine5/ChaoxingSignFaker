/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLoginSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.HttpCookie
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaoxingHttpClient private constructor(
    val okHttpClient: OkHttpClient,
    val userEntity: ChaoxingUserEntity
) {
    class ChaoxingLoginException(message: String) : ChaoxingPredictableException(message)

    class ChaoxingGetUserInfoException(message: String, throwable: Throwable) :
        ChaoxingPredictableException(message, throwable)

    fun newCall(request: Request): Call = okHttpClient.newCall(request)

    companion object {
        private const val CHAOXING_USER_AGENT =
            "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9006 Build/8aba9e4.0) (schild:ce31140dfcdc2fcd113ccdd86f89a9aa) (device:SM-N9006) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_3_6.5.1_android_phone_10837_265 (@Kalimdor)_68f184fd763546c1a04ab3a09b3deebb"
        private const val TRANSFER_KEY = "u2oh6Vu^HWe4_AES"
        private const val URL_USER_INFO = "https://sso.chaoxing.com/apis/login/userLogin4Uname.do"
        private const val URL_LOGIN = "https://passport2.chaoxing.com/fanyalogin"

        var instance: ChaoxingHttpClient? = null

        var deviceCode: String? = null

        suspend fun generateDeviceCode(context: Context): String {
            val rawData = MessageDigest.getInstance("SHA-256").digest(
                (UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString()
                    .replace("-", "")).toByteArray()
            )
            return Base64.getEncoder().encodeToString(rawData + rawData).apply {
                context.chaoxingDataStore.updateData {
                    it.toBuilder().setDeviceCode(this).build()
                }
            }
        }

        suspend fun loadFromOtherUserSession(session: ChaoxingOtherUserSession): ChaoxingHttpClient {
            val okHttpClient = instance!!.okHttpClient.newBuilder().cookieJar(object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                private var chaoxingCookieSession: List<Cookie> = listOf()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (url.host.endsWith("chaoxing.com") && (url.encodedPath == "/fanyalogin")) {
                        chaoxingCookieSession = cookies.toMutableList()
                    } else if (url.encodedPath == "/apis/login/userLogin4Uname.do") {
                        val cookiesMap = cookies.associateBy { cookie -> cookie.name }
                        val keepCookies =
                            chaoxingCookieSession.filter { !cookiesMap.containsKey(it.name) }
                        chaoxingCookieSession = (keepCookies + cookiesMap.values)
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
                        .header("User-Agent", CHAOXING_USER_AGENT).build()
                )
            }.build().apply {
                cookieJar.saveFromResponse(
                    HttpUrl.Builder().scheme("https").host("chaoxing.com")
                        .encodedPath("/fanyalogin").build(),
                    session.cookiesList.map {
                        Cookie.Builder()
                            .value(it.value)
                            .name(it.name)
                            .domain(it.host)
                            .build()
                    }
                )
            }
            val userInfo = getInfo(okHttpClient)
            return ChaoxingHttpClient(
                okHttpClient,
                userInfo
            )
        }

        suspend fun create(
            phoneNumber: String,
            password: String,
            context: Context
        ): ChaoxingHttpClient = withContext(Dispatchers.IO) {
            val cookieJar: CookieJar = object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                private var chaoxingCookieSession: List<Cookie> = listOf()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (url.host.endsWith("chaoxing.com") && (url.encodedPath == "/fanyalogin")) {
                        chaoxingCookieSession = cookies.toMutableList()
                    } else if (url.encodedPath == "/apis/login/userLogin4Uname.do") {
                        val cookiesMap = cookies.associateBy { cookie -> cookie.name }
                        val keepCookies =
                            chaoxingCookieSession.filter { !cookiesMap.containsKey(it.name) }
                        chaoxingCookieSession = (keepCookies + cookiesMap.values)
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
            }
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", CHAOXING_USER_AGENT).build()
                    )
                }
                .build()
            login(client, phoneNumber, password, context)
            val userInfo = getInfo(client).apply {
                UMengHelper.profileSignIn(this, phoneNumber)
            }
            return@withContext ChaoxingHttpClient(
                client,
                userInfo
            ).apply {
                instance = this
            }
        }

        suspend fun loadFromDataStore(dataStore: ChaoxingSignFakerDataStore): ChaoxingHttpClient {
            val okHttpClient = OkHttpClient.Builder().cookieJar(object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                private var chaoxingCookieSession: List<Cookie> = listOf()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (url.host.endsWith("chaoxing.com") && (url.encodedPath == "/fanyalogin")) {
                        chaoxingCookieSession = cookies.toMutableList()
                    } else if (url.encodedPath == "/apis/login/userLogin4Uname.do") {
                        val cookiesMap = cookies.associateBy { cookie -> cookie.name }
                        val keepCookies =
                            chaoxingCookieSession.filter { !cookiesMap.containsKey(it.name) }
                        chaoxingCookieSession = (keepCookies + cookiesMap.values)
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
                        .header("User-Agent", CHAOXING_USER_AGENT).build()
                )
            }.build().apply {
                cookieJar.saveFromResponse(
                    HttpUrl.Builder().scheme("https").host("chaoxing.com")
                        .encodedPath("/fanyalogin").build(),
                    dataStore.loginSession.cookiesList
                        .map { cookie ->
                            Cookie.Builder()
                                .name(cookie.name)
                                .value(cookie.value)
                                .domain(cookie.host)
                                .build()
                        }
                )
            }
            val userInfo = getInfo(okHttpClient)
            return ChaoxingHttpClient(
                okHttpClient,
                userInfo
            ).apply {
                instance = this
            }
        }

        private suspend fun getInfo(client: OkHttpClient): ChaoxingUserEntity =
            withContext(Dispatchers.IO) {
                runCatching {
                    client.newCall(Request.Builder().get().url(URL_USER_INFO).build()).execute()
                        .use { response ->
                            val jsonResult =
                                JSONObject.parseObject(response.body?.string()).getJSONObject("msg")
                            return@withContext ChaoxingUserEntity(
                                jsonResult.getInteger("uid"),
                                jsonResult.getInteger("fid"),
                                jsonResult.getString("name"),
                                jsonResult.getString("schoolname"),
                                jsonResult.getString("uname"),
                                jsonResult.getString("pic").replace("http://", "https://"),
                                jsonResult.getInteger("puid")
                            )
                        }
                }.getOrElse {
                    throw ChaoxingGetUserInfoException("获取用户信息失败", it)
                }
            }

        suspend fun checkSharedEntity(
            phoneNumber: String,
            password: String,
            context: Context
        ): ChaoxingOtherUserSharedEntity =
            withContext(Dispatchers.IO) {
                val uname = encryptByAES(phoneNumber)
                val encryptedPassword = encryptByAES(password)
                val request = Request.Builder()
                    .url(URL_LOGIN)
                    .post(FormBody.Builder().apply {
                        addEncoded("fid", "-1")
                        addEncoded("uname", uname.replace("+", "%2B"))
                        addEncoded(
                            "password",
                            encryptedPassword.replace("+", "%2B").replace(" ", "%2B")
                        )
                        addEncoded("refer", "https%3A%2F%2Fi.chaoxing.com")
                        addEncoded("t", "true")
                        addEncoded("forbidotherlogin", "0")
                        addEncoded("validate", "")
                        addEncoded("doubleFactorLogin", "0")
                        addEncoded("independentId", "0")
                        addEncoded("independentNameId", "0")
                    }.build())
                    .build()

                val tempOkHttpClient =
                    instance!!.okHttpClient.newBuilder()
                        .cookieJar(object : CookieJar {
                            private val cookieStore: MutableMap<String, List<Cookie>> =
                                mutableMapOf()
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
                tempOkHttpClient.newCall(request).execute().use {
                    val jsonResult = JSONObject.parseObject(it.body?.string())
                    if (!jsonResult.getBoolean("status")) {
                        throw ChaoxingLoginException(if (jsonResult.containsKey("msg2")) {
                            jsonResult.getString("msg2").ifEmpty {
                                "登录错误"
                            }
                        } else {
                            "登录错误"
                        })
                    }
                    tempOkHttpClient.cookieJar.saveFromResponse(
                        request.url,
                        Cookie.parseAll(request.url, it.headers)
                    )
                    return@withContext ChaoxingOtherUserSharedEntity(
                        phoneNumber,
                        encryptedPassword,
                        getInfo(tempOkHttpClient).name
                    )
                }
            }

        suspend fun login(
            client: OkHttpClient,
            phoneNumber: String,
            password: String,
            context: Context,
            isSaveToDataStore: Boolean = true,
            isEncryptedPassword: Boolean = false
        ): Unit =
            withContext(Dispatchers.IO) {
                val uname = encryptByAES(phoneNumber)
                val encryptedPassword =
                    if (isEncryptedPassword) password else encryptByAES(password)
                val request = Request.Builder()
                    .url(URL_LOGIN)
                    .post(FormBody.Builder().apply {
                        addEncoded("fid", "-1")
                        addEncoded("uname", uname.replace("+", "%2B"))
                        addEncoded(
                            "password",
                            encryptedPassword.replace("+", "%2B").replace("%20", "%2B")
                                .replace(" ", "%2B")
                        )
                        addEncoded("refer", "https%3A%2F%2Fi.chaoxing.com")
                        addEncoded("t", "true")
                        addEncoded("forbidotherlogin", "0")
                        addEncoded("validate", "")
                        addEncoded("doubleFactorLogin", "0")
                        addEncoded("independentId", "0")
                        addEncoded("independentNameId", "0")
                    }.build())
                    .build()

                client.newCall(request).execute().use {
                    val jsonResult = JSONObject.parseObject(it.body?.string())
                    if (!jsonResult.getBoolean("status")) {
                        throw ChaoxingLoginException(if (jsonResult.containsKey("msg2")) {
                            jsonResult.getString("msg2").ifEmpty {
                                "登录错误"
                            }
                        } else {
                            "登录错误"
                        })
                    }

                    client.cookieJar.saveFromResponse(
                        request.url,
                        Cookie.parseAll(request.url, it.headers)
                    )

                }
                if (isSaveToDataStore) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setLoginSession(
                            ChaoxingLoginSession.newBuilder().addAllCookies(
                                client.cookieJar.loadForRequest(
                                    HttpUrl.Builder()
                                        .scheme("https")
                                        .host("chaoxing.com").build()
                                ).map { cookie ->
                                    HttpCookie.newBuilder()
                                        .setValue(cookie.value)
                                        .setName(cookie.name)
                                        .setHost(cookie.domain).build()
                                }
                            )
                                .setPassword(encryptedPassword)
                                .setPhoneNumber(phoneNumber)
                                .build()
                        ).build()
                    }
                }
            }

        private fun encryptByAES(message: String, key: String = TRANSFER_KEY): String {
            val iv = IvParameterSpec(key.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        }
    }

    suspend fun reLogin(context: Context): Boolean {
        runCatching {
            context.chaoxingDataStore.data.first().let {
                if (it.loginSession.phoneNumber.isNullOrEmpty() || it.loginSession.password.isNullOrEmpty()) {
                    return false
                }
                login(
                    okHttpClient,
                    it.loginSession.phoneNumber,
                    it.loginSession.password,
                    context,
                    isEncryptedPassword = true
                )
            }
            return true
        }.getOrElse {
            return false
        }
    }
}