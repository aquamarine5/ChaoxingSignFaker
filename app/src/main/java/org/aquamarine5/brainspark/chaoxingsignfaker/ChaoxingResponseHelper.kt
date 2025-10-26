/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.sentry.Sentry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

suspend fun Response.checkResponse(context: Context): Boolean =
    if (isSuccessful) {
        false
    } else {
        withContext(Dispatchers.Main) {
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
        }
        true
    }

suspend fun Response.checkResponse(snackbarHostState: SnackbarHostState): Boolean =
    if (isSuccessful) {
        false
    } else {
        coroutineScope {
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    when (code) {
                        ChaoxingHttpClient.HTTP_RESPONSE_CODE_UNKNOWN_HOST -> "网络异常，请检查网络连接和DNS服务器"
                        ChaoxingHttpClient.HTTP_RESPONSE_CODE_UNKNOWN_ERROR -> "网络异常，请稍后再试"
                        ChaoxingHttpClient.HTTP_RESPONSE_CODE_SOCKET_TIMEOUT -> "网络异常，连接超时，请检查网络连接"
                        403 -> "网络异常，访问被拒绝"
                        404 -> "网络异常，资源未找到"
                        500 -> "网络异常，服务器内部错误"
                        else -> "网络异常，错误码：$code"
                    }
                )
            }
        }
        true
    }

fun Throwable.toastReport(
    context: Context,
    prefixTips: String? = null,
    hapticFeedback: HapticFeedback? = null
) {
    this.cause?.printStackTrace()
    this.printStackTrace()
    hapticFeedback?.performHapticFeedback(HapticFeedbackType.Reject)
    if (this !is ChaoxingPredictableException) {
        Sentry.captureException(this)
        Toast.makeText(
            context,
            "${prefixTips?.plus(" ") ?: ""}预期外错误:${this.message ?: this::class.simpleName}",
            Toast.LENGTH_LONG
        ).show()
    } else {
        Toast.makeText(
            context,
            "${prefixTips?.plus(" ") ?: ""}${this.message ?: this::class.simpleName}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun Throwable.snackbarReport(
    snackbarHostState: SnackbarHostState?,
    coroutineScope: CoroutineScope,
    prefixTips: String? = null,
    hapticFeedback: HapticFeedback,
    duration: SnackbarDuration = SnackbarDuration.Short,
    actionLabel: String? = null,
    onSnackbarResult: ((SnackbarResult) -> Unit)? = null
) {
    this.cause?.printStackTrace()
    this.printStackTrace()
    hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
    if (this !is ChaoxingPredictableException) {
        Sentry.captureException(this)
        snackbarHostState?.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            snackbarHostState?.showSnackbar(
                "${prefixTips?.plus(" ") ?: ""}预期外错误:${this@snackbarReport.message ?: this@snackbarReport::class.simpleName}",
                actionLabel,
                true,
                duration
            )?.apply {
                onSnackbarResult?.invoke(this)
            }
        }
    } else {
        snackbarHostState?.currentSnackbarData?.dismiss()
        coroutineScope.launch {
            snackbarHostState?.showSnackbar(
                "${prefixTips?.plus(" ") ?: ""}${this@snackbarReport.message ?: this@snackbarReport::class.simpleName}",
                actionLabel,
                true,
                duration
            )?.apply {
                onSnackbarResult?.invoke(this)
            }
        }
    }
}

fun Throwable.ifAlreadySigned(action: () -> Unit) {
    if (this is ChaoxingSigner.AlreadySignedException) {
        action()
    }
}

fun <T> List<T?>.checkIsLast(fromIndex: Int): Boolean {
    if (this.size - 1 == fromIndex) return true
    for (i in fromIndex + 1 until this.size) {
        if (this[i] != null) return false
    }
    return true
}