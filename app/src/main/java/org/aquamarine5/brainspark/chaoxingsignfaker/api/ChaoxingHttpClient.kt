/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingUserAgent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLoginSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.HttpCookie
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingEasemobIMConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingParseDataException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkPredictable
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.checkResponseThrowException
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.displaySnackbar
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.sentryReport
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaoxingHttpClient internal constructor(
    val userEntity: ChaoxingUserEntity,
    name: String,
    puid: Int,
    deviceCode: String,
    initialConfiguredFid: Int,
    okHttpClient: OkHttpClient,
) : ChaoxingHttpRequester(
    okHttpClient = okHttpClient,
    phoneNumber = userEntity.phoneNumber,
    name = name,
    puid = puid,
    deviceCode = deviceCode,
    initialConfiguredFid = initialConfiguredFid,
) {
    suspend fun updateConfiguredFid(context: Context, fid: Int) {
        requirePredictable(userEntity.fidList.any { it.first == fid }) { "请选择账号所属的学校单位" }
        context.chaoxingDataStore.updateData { dataStore ->
            dataStore.toBuilder().apply {
                if (loginSession.phoneNumber == phoneNumber) {
                    setLoginSession(loginSession.toBuilder().setConfiguredFid(fid))
                } else {
                    val index = otherUsersList.indexOfFirst {
                        it.phoneNumber == phoneNumber
                    }
                    checkPredictable(index >= 0) { "未找到当前账号的登录会话" }
                    setOtherUsers(index, getOtherUsers(index).toBuilder().setConfiguredFid(fid))
                }
            }.build()
        }
        configuredFid = fid
        instance?.takeIf { it.phoneNumber == phoneNumber }?.configuredFid = fid
        cloneInstance?.takeIf { it.phoneNumber == phoneNumber }?.configuredFid = fid
        ChaoxingHttpRequesterPool.initialize(context.chaoxingDataStore.data.first().otherUsersList)
    }


    class ChaoxingLoginException(message: String, throwable: Throwable? = null) :
        ChaoxingParseDataException(message, throwable)

    class ChaoxingGetUserInfoException(
        message: String,
        throwable: Throwable? = null,
        val isOtherUser: Boolean,
        data: String? = null
    ) :
        ChaoxingParseDataException(message, throwable, data)

    class ChaoxingNetworkException(message: String? = null, throwable: Throwable? = null) :
        ChaoxingParseDataException(message ?: "网络错误", throwable)

    private var storageIMConfig: ChaoxingEasemobIMConfig? = null

    suspend fun getIMConfig(): ChaoxingEasemobIMConfig {
        if (storageIMConfig != null) return storageIMConfig!!
        return ChaoxingIMHelper.getEasemobConfig(this).also {
            storageIMConfig = it
        }
    }

    companion object {
        private const val TRANSFER_KEY = "u2oh6Vu^HWe4_AES"
        private val URL_USER_INFO =
            "https://sso.chaoxing.com/apis/login/userLogin4Uname.do".toHttpUrl()
        private val URL_LOGIN = "https://passport2.chaoxing.com/fanyalogin".toHttpUrl()

        var instance: ChaoxingHttpClient? = null

        private var cloneInstanceState = mutableStateOf<ChaoxingHttpClient?>(null)

        var cloneInstance: ChaoxingHttpClient?
            get() = cloneInstanceState.value
            set(value) {
                cloneInstanceState.value = value
            }

        fun getClientInstanceOrClone(isCloneSession: Boolean = true) =
            if (isCloneSession && cloneInstance != null) cloneInstance else instance

        @Deprecated("Should use ChaoxingHttpClient().deviceCode not ChaoxingHttpClient.Companion.deviceCode")
        var deviceCode: String? = null

        @Suppress("Deprecation")
        @Deprecated("Don't use saveDeviceCode() and ChaoxingHttpClient.Companion.deviceCode")
        suspend fun saveDeviceCode(context: Context): String {
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

        fun exitCloning(coroutineScope: CoroutineScope, snackbarHostState: SnackbarHostState) {
            cloneInstance = null
            coroutineScope.launch {
                snackbarHostState.displaySnackbar("已退出克隆登录", coroutineScope)
            }
        }

        @Deprecated("Use ChaoxingDeviceInfoHelper.randomizedDeviceCode() instead")
        fun generateDeviceCode(): String = ChaoxingDeviceInfoHelper.randomizedDeviceCode()

        private fun checkPasswordIllegalThrowException(password: String) {
            if (password.isEmpty())
                throw ChaoxingLoginException("密码不能为空")
            if (password.length !in 8..16)
                throw ChaoxingLoginException("密码位数应该在8-16位")
        }

        suspend fun loadFromOtherUserSession(
            session: ChaoxingOtherUserSession,
            context: Context
        ): ChaoxingHttpClient =
            loadFromOtherSession(session, context)
                .toChaoxingHttpClient(context).also { client ->
                    instance = client
                    ChaoxingHttpRequesterPool.put(client)
                }

        suspend fun create(
            phoneNumber: String,
            password: String,
            context: Context
        ): ChaoxingHttpClient = withContext(Dispatchers.IO) {
            checkPasswordIllegalThrowException(password)
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
                .retryOnConnectionFailure(true)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().run {
                            if (headers.none { it.first == "User-Agent" }) {
                                newBuilder()
                                    .header("User-Agent", chaoxingUserAgent)
                                    .build()
                            } else this
                        }
                    )
                }
                .build()
            login(client, phoneNumber, password, context)
            val session = context.chaoxingDataStore.data.first().loginSession
            val (userEntity, puid) = getInfoWithIdentity(client, context, phoneNumber)
            UMengHelper.profileSignIn(puid, userEntity.name, phoneNumber)
            val effectiveConfiguredFid = session.configuredFid.takeIf { configuredFid ->
                session.hasConfiguredFid() && userEntity.fidList.any { it.first == configuredFid }
            } ?: userEntity.fidList.first().first
            if (!session.hasConfiguredFid() || session.configuredFid != effectiveConfiguredFid) {
                context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder().apply {
                        setLoginSession(
                            loginSession.toBuilder()
                                .setConfiguredFid(effectiveConfiguredFid)
                                .build()
                        )
                    }.build()
                }
            }
            return@withContext ChaoxingHttpClient(
                userEntity = userEntity,
                name = userEntity.name,
                puid = puid,
                deviceCode = session.deviceCode.takeIf { it.isNotEmpty() }
                    ?: ChaoxingDeviceInfoHelper.getCachedLocalMachineDeviceCode(context),
                initialConfiguredFid = effectiveConfiguredFid,
                okHttpClient = client,
            ).apply {
                instance = this
                ChaoxingHttpRequesterPool.put(this)
            }
        }

        suspend fun loadFromDataStore(
            dataStore: ChaoxingSignFakerDataStore,
            context: Context
        ): ChaoxingHttpClient =
            ChaoxingHttpRequester.loadFromDataStore(dataStore, context)
                .toChaoxingHttpClient(context).also { client ->
                    instance = client
                    ChaoxingHttpRequesterPool.put(client)
                }

        suspend fun getInfo(
            client: OkHttpClient,
            context: Context,
            loginSession: ChaoxingLoginSession
        ): ChaoxingUserEntity =
            getInfoWithIdentity(client, context, loginSession.phoneNumber, null).first

        suspend fun getInfo(
            client: OkHttpClient,
            context: Context,
            otherUserSession: ChaoxingOtherUserSession
        ): ChaoxingUserEntity =
            getInfoWithIdentity(
                client,
                context,
                otherUserSession.phoneNumber,
                otherUserSession
            ).first

        suspend fun getInfo(
            client: OkHttpClient,
            context: Context,
            phoneNumber: String,
            otherUserSession: ChaoxingOtherUserSession? = null,
        ): ChaoxingUserEntity =
            getInfoWithIdentity(client, context, phoneNumber, otherUserSession).first

        internal suspend fun getInfoWithIdentity(
            client: OkHttpClient,
            context: Context,
            phoneNumber: String,
            otherUserSession: ChaoxingOtherUserSession? = null,
            isRetryAfterRelogin: Boolean = false,
        ): Pair<ChaoxingUserEntity, Int> =
            withContext(Dispatchers.IO) {
                var userInfoResponse: String? = null
                runCatching {
                    client.newCall(
                        Request.Builder()
                            .url(URL_USER_INFO)
                            .apply {
                                runCatching {
                                    ChaoxingDeviceInfoHelper.buildEncryptedDeviceInfo(context)
                                }.onSuccess {
                                    post(
                                        FormBody.Builder().apply {
                                            add(
                                                "data",
                                                it
                                            )
                                        }.build()
                                    )
                                }.onFailure {
                                    if (it is CancellationException) throw it
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "人脸识别相关数据获取失败，请尝试在本机安装学习通软件",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    if (it !is PackageManager.NameNotFoundException)
                                        it.sentryReport()
                                    get()
                                }
                            }
                            .build()
                    ).execute()
                        .use { response ->
                            response.checkResponseThrowException()
                            userInfoResponse = response.body.string()
                            val jsonResult =
                                JSONObject.parseObject(userInfoResponse).getJSONObject("msg")
                            val name = jsonResult.getString("name")
                            val puid = jsonResult.getInteger("puid")
                            val userEntity = ChaoxingUserEntity(
                                jsonResult.getInteger("uid"),
                                jsonResult.getInteger("fid"),
                                name,
                                listOf(),
                                jsonResult.getString("uname"),
                                jsonResult.getString("pic").replace("http://", "https://"),
                                puid,
                                phoneNumber,
                                jsonResult.getJSONObject("accountInfo")
                                    .getJSONObject("imAccount")
                                    .getString("password"),
                                jsonResult.getString("clientId")?.takeIf { it.isNotEmpty() },
                                parseFidList(jsonResult)
                            )
                            context.chaoxingDataStore.updateData { dataStore ->
                                dataStore.toBuilder().apply {
                                    if (loginSession.phoneNumber == phoneNumber) {
                                        if (loginSession.name != name ||
                                            !loginSession.hasPuid() || loginSession.puid != puid
                                        ) {
                                            setLoginSession(
                                                loginSession.toBuilder()
                                                    .setName(name)
                                                    .setPuid(puid)
                                                    .build()
                                            )
                                        }
                                    } else {
                                        otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
                                            .takeIf { it >= 0 }?.let { index ->
                                                val session = getOtherUsers(index)
                                                if (session.name != name ||
                                                    !session.hasPuid() || session.puid != puid
                                                ) {
                                                    setOtherUsers(
                                                        index,
                                                        session.toBuilder()
                                                            .setName(name)
                                                            .setPuid(puid)
                                                            .build()
                                                    )
                                                }
                                            }
                                    }
                                }.build()
                            }
                            return@withContext userEntity to puid
                        }
                }.getOrElse { throwable ->
                    if (throwable is CancellationException) throw throwable
                    if (otherUserSession != null) {
                        if (isRetryAfterRelogin) {
                            throw ChaoxingGetUserInfoException(
                                "获取代签用户信息失败",
                                throwable,
                                true,
                                userInfoResponse
                            )
                        }
                        runCatching {
                            reLoginFromOtherSession(client, context, otherUserSession)
                        }.onSuccess {
                            return@withContext getInfoWithIdentity(
                                client,
                                context,
                                otherUserSession.phoneNumber,
                                otherUserSession,
                                isRetryAfterRelogin = true
                            )
                        }.onFailure { reloginFailure ->
                            if (reloginFailure is CancellationException) throw reloginFailure
                            throw ChaoxingGetUserInfoException(
                                "获取代签用户信息失败",
                                throwable,
                                true,
                                userInfoResponse
                            )
                        }
                    }
                    throw ChaoxingGetUserInfoException(
                        "获取用户信息失败",
                        throwable,
                        false,
                        userInfoResponse
                    )
                }
            }

        private fun parseFidList(jsonResult: JSONObject): List<Pair<Int, String>> {
            val schools = linkedMapOf<Int, String>()
            val defaultFid = jsonResult.getIntValue("fid")
            schools[defaultFid] = jsonResult.getString("schoolname")
                ?.takeIf { it.isNotBlank() } ?: "未知学校"
            jsonResult.getJSONArray("unitConfigInfos")?.let { configs ->
                for (index in configs.indices) {
                    val config = configs.getJSONObject(index) ?: continue
                    val fid = config.getInteger("fid") ?: continue
                    val name = config.getString("schoolname")?.takeIf { it.isNotBlank() }
                    if (fid !in schools || schools[fid] == "未知学校") {
                        schools[fid] = name ?: "未知学校"
                    }
                }
            }
            return schools.map { it.key to it.value }
        }

        private suspend fun reLoginFromOtherSession(
            client: OkHttpClient,
            context: Context,
            otherUserSession: ChaoxingOtherUserSession
        ) {
            login(
                client,
                otherUserSession.phoneNumber,
                otherUserSession.password,
                context,
                isSaveToDataStore = false,
                isEncryptedPassword = true
            )
            context.chaoxingDataStore.updateData { dataStore ->
                dataStore.toBuilder().apply {
                    otherUsersList.indexOfFirst {
                        it.phoneNumber == otherUserSession.phoneNumber
                    }.takeIf { it >= 0 }?.let { index ->
                        setOtherUsers(
                            index, getOtherUsers(index).toBuilder()
                                .clearCookies()
                                .addAllCookies(
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
                                .build())
                    }
                }.build()
            }
        }

        suspend fun checkSharedEntity(
            phoneNumber: String,
            password: String,
            context: Context
        ): ChaoxingOtherUserSharedEntity =
            withContext(Dispatchers.IO) {
                checkPasswordIllegalThrowException(password)
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
                        }).retryOnConnectionFailure(true)
                        .build()
                tempOkHttpClient.newCall(request).execute().use {
                    it.checkResponseThrowException()
                    val jsonResult = JSONObject.parseObject(it.body.string())
                    if (!jsonResult.getBoolean("status")) {
                        throw ChaoxingLoginException(
                            if (jsonResult.containsKey("msg2")) {
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
                        getInfo(tempOkHttpClient, context, phoneNumber).name
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
                if (!isEncryptedPassword) checkPasswordIllegalThrowException(password)
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
                    it.checkResponseThrowException()
                    val jsonResult = JSONObject.parseObject(it.body.string())
                    if (!jsonResult.getBoolean("status")) {
                        throw ChaoxingLoginException(
                            jsonResult.getString("msg2")?.takeIf { it.isNotEmpty() } ?: "登录错误")
                    }

                    client.cookieJar.saveFromResponse(
                        request.url,
                        Cookie.parseAll(request.url, it.headers)
                    )

                }
                if (isSaveToDataStore) {
                    context.chaoxingDataStore.updateData {
                        it.toBuilder().setLoginSession(
                            (if (it.loginSession.phoneNumber == phoneNumber)
                                it.loginSession.toBuilder() else ChaoxingLoginSession.newBuilder())
                                .clearCookies().addAllCookies(
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
            it.sentryReport()
            return false
        }
    }
}
