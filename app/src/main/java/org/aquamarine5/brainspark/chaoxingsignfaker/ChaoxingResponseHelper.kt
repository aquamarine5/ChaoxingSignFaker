/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import android.widget.Toast
import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

fun Response.checkResponse(context: Context): Boolean =
    if (isSuccessful) {
        false
    } else {
        Toast.makeText(
            context, when (code) {
                ChaoxingHttpClient.HTTP_RESPONSE_CODE_UNKNOWN_HOST -> "网络异常，请检查网络连接和DNS服务器"
                ChaoxingHttpClient.HTTP_RESPONSE_CODE_UNKNOWN_ERROR -> "网络异常，请稍后再试"
                ChaoxingHttpClient.HTTP_RESPONSE_CODE_SOCKET_TIMEOUT -> "网络异常，连接超时，请检查网络连接"
                403 -> "网络异常，访问被拒绝"
                404 -> "网络异常，资源未找到"
                500 -> "网络异常，服务器内部错误"
                else -> "网络异常，错误码：$code"
            }, Toast.LENGTH_LONG
        ).show()
        true
    }