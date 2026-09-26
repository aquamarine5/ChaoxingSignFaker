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
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper.getSessionPuid
import org.aquamarine5.brainspark.chaoxingsignfaker.components.chaoxingUserAgent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingLoginSession
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
        val (userEntity, puid) = ChaoxingHttpClient.getInfoWithIdentity(
            okHttpClient,
            context,
            phoneNumber,
            otherUserSession,
        )
        val effectiveConfiguredFid = configuredFid.takeIf { fid ->
            userEntity.fidList.any { school -> school.first == fid }
        } ?: userEntity.fidList.first().first
        if (otherUserSession != null) {
            if (!otherUserSession.hasConfiguredFid() || otherUserSession.configuredFid != effectiveConfiguredFid) {
                context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder().apply {
                        otherUsersList.indexOfFirst { it.phoneNumber == phoneNumber }
                            .takeIf { it >= 0 }?.let { index ->
                                setOtherUsers(
                                    index,
                                    getOtherUsers(index).toBuilder()
                                        .setConfiguredFid(effectiveConfiguredFid)
                                        .build()
                                )
                            }
                    }.build()
                }
            }
        } else {
            context.chaoxingDataStore.updateData { dataStore ->
                dataStore.toBuilder().apply {
                    if (!loginSession.hasConfiguredFid() || loginSession.configuredFid != effectiveConfiguredFid) {
                        setLoginSession(
                            loginSession.toBuilder()
                                .setConfiguredFid(effectiveConfiguredFid)
                                .build()
                        )
                    }
                }.build()
            }
        }
        return ChaoxingHttpClient(
            userEntity = userEntity,
            name = userEntity.name,
            puid = puid,
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

        private suspend fun resolveOtherUserIdentity(
            okHttpClient: OkHttpClient,
            session: ChaoxingOtherUserSession,
            context: Context
        ): Pair<Int, Int> {
            val cachedPuid = session.getSessionPuid(context)
            if (cachedPuid != null && session.hasConfiguredFid()) {
                return cachedPuid to session.configuredFid
            }
            val (userEntity, freshPuid) = ChaoxingHttpClient.getInfoWithIdentity(
                okHttpClient,
                context,
                session.phoneNumber,
                session
            )
            val effectiveFid = session.configuredFid.takeIf { fid ->
                session.hasConfiguredFid() && userEntity.fidList.any { it.first == fid }
            } ?: userEntity.fidList.first().first
            if (!session.hasConfiguredFid() || session.configuredFid != effectiveFid) {
                context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder().apply {
                        otherUsersList.indexOfFirst { it.phoneNumber == session.phoneNumber }
                            .takeIf { it >= 0 }?.let { index ->
                                setOtherUsers(
                                    index,
                                    getOtherUsers(index).toBuilder()
                                        .setConfiguredFid(effectiveFid)
                                        .setPuid(freshPuid)
                                        .build()
                                )
                            }
                    }.build()
                }
            }
            return freshPuid to effectiveFid
        }

        private suspend fun resolveLoginIdentity(
            okHttpClient: OkHttpClient,
            session: ChaoxingLoginSession,
            context: Context
        ): Pair<Int, Int> {
            if (session.hasPuid() && session.hasConfiguredFid()) {
                return session.puid to session.configuredFid
            }
            val (userEntity, freshPuid) = ChaoxingHttpClient.getInfoWithIdentity(
                okHttpClient,
                context,
                session.phoneNumber,
                null
            )
            val effectiveFid = session.configuredFid.takeIf { fid ->
                session.hasConfiguredFid() && userEntity.fidList.any { it.first == fid }
            } ?: userEntity.fidList.first().first
            if (!session.hasConfiguredFid() || session.configuredFid != effectiveFid) {
                context.chaoxingDataStore.updateData { dataStore ->
                    dataStore.toBuilder().apply {
                        setLoginSession(
                            loginSession.toBuilder()
                                .setConfiguredFid(effectiveFid)
                                .build()
                        )
                    }.build()
                }
            }
            return freshPuid to effectiveFid
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
            val (resolvedPuid, resolvedFid) = resolveOtherUserIdentity(
                okHttpClient,
                session,
                context
            )
            return ChaoxingHttpRequester(
                okHttpClient,
                session.phoneNumber,
                session.name,
                resolvedPuid,
                session.resolveDeviceCode(context),
                resolvedFid,
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
            val (resolvedPuid, resolvedFid) = resolveLoginIdentity(okHttpClient, session, context)
            return ChaoxingHttpRequester(
                okHttpClient,
                session.phoneNumber,
                session.name,
                resolvedPuid,
                session.deviceCode.takeIf { it.isNotEmpty() }
                    ?: ChaoxingDeviceInfoHelper.getCachedLocalMachineDeviceCode(context),
                resolvedFid,
            )
        }
    }
}
