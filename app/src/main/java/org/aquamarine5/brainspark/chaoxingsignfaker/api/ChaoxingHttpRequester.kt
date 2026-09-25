/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import okhttp3.Call
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingUserAgent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

open class ChaoxingHttpRequester internal constructor(
    val okHttpClient: OkHttpClient,
    val phoneNumber: String,
    val name: String,
    val puid: Int,
    val deviceCode: String,
    initialConfiguredFid: Int,
    internal val otherUserSession: ChaoxingOtherUserSession? = null,
) {
    var configuredFid by mutableIntStateOf(initialConfiguredFid)
        protected set

    var storageCloudToken: String? = null

    fun newCall(request: Request): Call = okHttpClient.newCall(request)

    suspend fun toChaoxingHttpClient(context: Context): ChaoxingHttpClient {
        val userInfo = ChaoxingHttpClient.getInfoWithIdentity(
            okHttpClient,
            context,
            phoneNumber,
            otherUserSession,
        )
        val effectiveConfiguredFid = configuredFid.takeIf { fid ->
            userInfo.userEntity.fidList.any { school -> school.first == fid }
        } ?: userInfo.userEntity.fidList.first().first
        return ChaoxingHttpClient(
            userEntity = userInfo.userEntity,
            name = userInfo.name,
            puid = userInfo.puid,
            deviceCode = deviceCode,
            initialConfiguredFid = effectiveConfiguredFid,
            okHttpClient = okHttpClient,
        )
    }

    companion object {
        private suspend fun ChaoxingOtherUserSession.resolveDeviceCode(context: Context): String =
            deviceCode.takeIf { it.isNotEmpty() } ?: ChaoxingDeviceInfoHelper.randomizedDeviceCode()
                .also { code ->
                    context.chaoxingDataStore.updateData { dataStore ->
                        dataStore.toBuilder().apply {
                            otherUsersList.indexOfFirst { it.phoneNumber == this@resolveDeviceCode.phoneNumber }
                                .takeIf { it >= 0 }?.let { index ->
                                    setOtherUsers(
                                        index,
                                        getOtherUsers(index).toBuilder()
                                            .setDeviceCode(code)
                                            .setIsNotRandomizedDeviceCode(false)
                                            .build()
                                    )
                                }
                        }.build()
                    }
                }

        suspend fun loadFromOtherSession(
            session: ChaoxingOtherUserSession,
            context: Context,
        ): ChaoxingHttpRequester {
            val baseClient = ChaoxingHttpClient.instance?.okHttpClient ?: OkHttpClient()
            val okHttpClient = baseClient.newBuilder().cookieJar(object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                private var chaoxingCookieSession: List<Cookie> = listOf()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (url.host.endsWith("chaoxing.com") && url.encodedPath == "/fanyalogin") {
                        chaoxingCookieSession = cookies.toMutableList()
                    } else if (url.encodedPath == "/apis/login/userLogin4Uname.do") {
                        val cookiesMap = cookies.associateBy { cookie -> cookie.name }
                        val keepCookies = chaoxingCookieSession.filter {
                            !cookiesMap.containsKey(it.name)
                        }
                        chaoxingCookieSession = keepCookies + cookiesMap.values
                    } else {
                        cookieStore[url.host] = cookies
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> =
                    if (url.host.endsWith("chaoxing.com")) {
                        chaoxingCookieSession
                    } else {
                        cookieStore[url.host] ?: listOf()
                    }
            }).addInterceptor { chain ->
                chain.proceed(
                    chain.request().run {
                        if (headers.none { it.first == "User-Agent" }) {
                            newBuilder().header("User-Agent", chaoxingUserAgent).build()
                        } else {
                            this
                        }
                    }
                )
            }.retryOnConnectionFailure(true).build().apply {
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
            return ChaoxingHttpRequester(
                okHttpClient,
                session.phoneNumber,
                session.name,
                session.puid.takeIf { session.hasPuid() } ?: 0,
                session.resolveDeviceCode(context),
                session.configuredFid.takeIf { session.hasConfiguredFid() } ?: 0,
                session,
            )
        }

        suspend fun loadFromDataStore(
            dataStore: ChaoxingSignFakerDataStore,
            context: Context,
        ): ChaoxingHttpRequester {
            val okHttpClient = OkHttpClient.Builder().cookieJar(object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()
                private var chaoxingCookieSession: List<Cookie> = listOf()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    if (url.host.endsWith("chaoxing.com") && url.encodedPath == "/fanyalogin") {
                        chaoxingCookieSession = cookies.toMutableList()
                    } else if (url.encodedPath == "/apis/login/userLogin4Uname.do") {
                        val cookiesMap = cookies.associateBy { cookie -> cookie.name }
                        val keepCookies = chaoxingCookieSession.filter {
                            !cookiesMap.containsKey(it.name)
                        }
                        chaoxingCookieSession = keepCookies + cookiesMap.values
                    } else {
                        cookieStore[url.host] = cookies
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> =
                    if (url.host.endsWith("chaoxing.com")) {
                        chaoxingCookieSession
                    } else {
                        cookieStore[url.host] ?: listOf()
                    }
            }).addInterceptor { chain ->
                chain.proceed(
                    chain.request().run {
                        if (headers.none { it.first == "User-Agent" }) {
                            newBuilder().header("User-Agent", chaoxingUserAgent).build()
                        } else {
                            this
                        }
                    }
                )
            }.retryOnConnectionFailure(true).build().apply {
                cookieJar.saveFromResponse(
                    HttpUrl.Builder().scheme("https").host("chaoxing.com")
                        .encodedPath("/fanyalogin").build(),
                    dataStore.loginSession.cookiesList.map { cookie ->
                        Cookie.Builder()
                            .name(cookie.name)
                            .value(cookie.value)
                            .domain(cookie.host)
                            .build()
                    }
                )
            }
            val session = dataStore.loginSession
            return ChaoxingHttpRequester(
                okHttpClient,
                session.phoneNumber,
                session.name,
                session.puid.takeIf { session.hasPuid() } ?: 0,
                session.deviceCode.takeIf { it.isNotEmpty() }
                    ?: ChaoxingDeviceInfoHelper.getLocalMachineDeviceCode(context).also { code ->
                        if (code.isNotEmpty()) {
                            context.chaoxingDataStore.updateData { currentDataStore ->
                                currentDataStore.toBuilder().setLoginSession(
                                    currentDataStore.loginSession.toBuilder()
                                        .setDeviceCode(code)
                                        .setIsNotRandomizedDeviceCode(true)
                                        .build()
                                ).build()
                            }
                        }
                    },
                session.configuredFid.takeIf { session.hasConfiguredFid() } ?: 0,
            )
        }
    }
}
