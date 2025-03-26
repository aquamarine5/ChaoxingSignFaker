/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.sentry.Sentry
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

data class ChaoxingSignStatus(
    val isSuccess: MutableState<Boolean?> = mutableStateOf(null),
    val error: MutableState<String> = mutableStateOf(""),
) {
    fun getText(): String {
        return when (isSuccess.value) {
            true -> "签到成功"
            false -> "失败！${error.value}"
            null -> ""
        }
    }

    fun success() {
        var obj by isSuccess
        obj = true
    }

    fun failed(e: Throwable) {
        var obj1 by isSuccess
        var obj2 by error
        obj1 = false
        obj2 = when (e) {
            is ChaoxingSigner.SignActivityNoPermissionException -> "无权限访问"
            is ChaoxingSigner.AlreadySignedException -> "已签到"
            else -> {
                Sentry.captureException(e)
                e.message ?: "签到失败"
            }
        }
    }


}
