/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

fun HttpUrl.toOriginSignHost(): HttpUrl =
    if (host == ChaoxingSignRoute.HOST_DATA_XXT)
        newBuilder().host(ChaoxingSignRoute.HOST_ORIGIN).build()
    else this

object ChaoxingSignRoute : Interceptor {
    const val HOST_ORIGIN = "mobilelearn.chaoxing.com"
    const val HOST_DATA_XXT = "data-xxt.aichaoxing.com"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val routedRequest = when (request.url.host) {
            HOST_DATA_XXT -> request.newBuilder().header("Host", HOST_ORIGIN).build()
            HOST_ORIGIN -> request.newBuilder()
                .url(request.url.newBuilder().host(HOST_DATA_XXT).build())
                .header("Host", HOST_ORIGIN)
                .build()
            else -> return chain.proceed(request)
        }
        val fallbackRequest = request.newBuilder()
            .url(request.url.toOriginSignHost())
            .removeHeader("Host")
            .build()
        val routedResult = runCatching { chain.proceed(routedRequest) }
        val failure = routedResult.exceptionOrNull()
        if (failure != null) {
            if (failure !is IOException || chain.call().isCanceled()) throw failure
            return chain.proceed(fallbackRequest)
        }
        val response = routedResult.getOrThrow()
        if (response.code == 404) {
            response.close()
            return chain.proceed(fallbackRequest)
        }
        return response
    }
}
