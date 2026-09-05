/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.Orange
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.ChaoxingPredictableException

@Stable
data class ChaoxingSignStatus(
    private val hapticFeedback: HapticFeedback,
    val isSuccess: MutableState<Boolean?> = mutableStateOf(null),
    val error: MutableState<String> = mutableStateOf(""),
    val isLoading: MutableState<Boolean> = mutableStateOf(false),
    val isObsoleteSession: MutableState<Boolean> = mutableStateOf(false),
    val isCaptchaResolvedByModel: MutableState<Boolean> = mutableStateOf(false),
    val errorException: MutableState<Throwable?> = mutableStateOf(null)
) {
    fun markCaptchaResolvedByModel() {
        isCaptchaResolvedByModel.value = true
    }

    fun loading() {
        isLoading.value = true
    }

    fun success() {
        isSuccess.value = true
        isLoading.value = false
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    fun successForLate() {
        isLoading.value = false
        error.value = "疑似迟到"
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
    }

    fun failed(e: Throwable) {
        isSuccess.value = false
        isLoading.value = false
        errorException.value = e
        error.value = when (e) {
            is ChaoxingSigner.AlreadySignedException -> "您已签到过了"
            is ChaoxingPredictableException -> e.message ?: "签到失败"
            else -> {
                e.message ?: "预期外错误签到失败"
            }
        }
    }

    fun markSessionObsoleted() {
        isObsoleteSession.value = true
    }

    val isBypassCheckingRequired: Boolean
        get() = errorException.value is ChaoxingSigner.SignActivityNoPermissionException ||
                errorException.value is ChaoxingSigner.PredictedAlreadySignedException

    @Composable
    fun ResultCard(onRetry: (() -> Unit)? = null) {
        when (isSuccess.value) {
            true -> {
                Icon(painterResource(R.drawable.ic_check), "签到成功")
            }

            false -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onRetry != null)
                        IconButton(onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onRetry()
                        }) {
                            Icon(painterResource(R.drawable.ic_refresh_rounded), null)
                        }
                    Text(
                        error.value, color = when (error.value) {
                            "您已签到过了" -> {
                                LocalContentColor.current
                            }

                            "疑似迟到" -> {
                                Orange
                            }

                            else -> {
                                Color(0xFFF43E06)
                            }
                        }
                    )
                }
            }

            null -> {
                if (isLoading.value)
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }

    }
}
typealias ImportOtherUserResult = Triple<ChaoxingImportOtherUserResultStatus, String, ChaoxingOtherUserSession>

enum class ChaoxingImportOtherUserResultStatus {
    SUCCESS,
    EXISTED_BUT_UPDATE_PASSWORD,
    EXISTED_BUT_UPDATE_FACE_IMAGES
}

fun ImportOtherUserResult.getResultTips(): String =
    when (this.first) {
        ChaoxingImportOtherUserResultStatus.SUCCESS -> "$second(手机号:${third.phoneNumber}) 用户成功导入"
        ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_PASSWORD -> "已更新 $second(手机号:${third.phoneNumber}) 密码"
        ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_FACE_IMAGES -> "已添加 $second(手机号:${third.phoneNumber}) 的人脸照片信息"
    }