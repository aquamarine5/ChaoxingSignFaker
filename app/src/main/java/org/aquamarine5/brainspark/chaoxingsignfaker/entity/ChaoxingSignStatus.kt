/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.sentry.Sentry
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.R

data class ChaoxingSignStatus(
    val isSuccess: MutableState<Boolean?> = mutableStateOf(null),
    val error: MutableState<String> = mutableStateOf(""),
    val isLoading: MutableState<Boolean> = mutableStateOf(false)
) {

    fun loading() {
        var obj by isLoading
        obj = true
    }

    fun success() {
        var obj1 by isSuccess
        var obj2 by isLoading
        obj1 = true
        obj2 = false
    }

    fun failed(e: Throwable) {
        var obj1 by isSuccess
        var obj2 by error
        var obj3 by isLoading
        obj1 = false
        obj3 = false
        obj2 = when (e) {
            is ChaoxingPredictableException -> e.message ?: "签到失败"
            else -> {
                Sentry.captureException(e)
                e.message ?: "签到失败"
            }
        }
    }

    @Composable
    fun ResultCard() {

            when (isSuccess.value) {
                true -> {
                    Icon(painterResource(R.drawable.ic_check), "签到成功")
                }

                false -> {
                    Text(error.value, color = Color(0xFFF43E06))
                }

                null -> {
                    if (isLoading.value)
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

    }
}
