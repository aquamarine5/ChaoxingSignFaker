/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCaptchaDataEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

@Composable
fun CaptchaHandlerDialog(
    signer: ChaoxingSigner,
    onResult: suspend (Result<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var data by remember { mutableStateOf<ChaoxingCaptchaDataEntity?>(null) }
    val shadeImageUrl by remember(data) { mutableStateOf(data?.shadeImageUrl) }
    val cutoutImageUrl by remember(data) { mutableStateOf(data?.cutoutImageUrl) }
    var sliderPosition by remember(data) { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(320f) }
    val snackbar = LocalSnackbarHostState.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density by remember(containerWidth) { mutableFloatStateOf(containerWidth / 320) }
    val sliderMaxValue = remember(containerWidth) { containerWidth - 56f * density }
    LaunchedEffect(signer) {
        data = signer.getCaptchaImageV2()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("请完成滑动验证") },
        text = {
            if (data != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f)
                            .onSizeChanged {
                                containerWidth = it.width.toFloat()
                            }
                            .background(Color.Gray)
                    ) {
                        AsyncImage(
                            model = shadeImageUrl,
                            contentDescription = "背景图",
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(0f)
                        )

                        AsyncImage(
                            model = cutoutImageUrl,
                            contentDescription = "滑块",
                            modifier = Modifier
                                .offset { IntOffset((sliderPosition).toInt(), 0) }
                                .aspectRatio(56f / 160f)
                                .fillMaxHeight()
                                .zIndex(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                        },
                        onValueChangeFinished = {
                            coroutineScope.launch {
                                runCatching {
                                    // (-8 ~ 272) + 28
                                    // 0 ~280
                                    val normalizedPosition =
                                        (sliderPosition / sliderMaxValue) * 280f - 8

                                    signer.checkCaptchaResult(normalizedPosition, data!!)
                                        .let { result ->
                                            if (result == null) {
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.Reject
                                                )
                                                sliderPosition = 0f
                                                Toast.makeText(
                                                    context,
                                                    "验证失败，请重试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                data = signer.getCaptchaImageV2()
                                            } else {
                                                onResult(Result.success(result))
                                                onDismiss()
                                            }
                                        }
                                }.onFailure {
                                    it.snackbarReport(
                                        snackbar,
                                        coroutineScope,
                                        "验证码校验失败",
                                        hapticFeedback
                                    )
                                    onResult(Result.failure(it))
                                    onDismiss()
                                }
                            }
                        },
                        valueRange = 0f..sliderMaxValue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()

                    var shouldRetry by remember(data) { mutableStateOf(false) }
                    LaunchedEffect(data) {
                        delay(5000)
                        shouldRetry = true
                    }
                    AnimatedVisibility(shouldRetry, enter = fadeIn() + slideInVertically()) {
                        Button(onClick = {
                            coroutineScope.launch {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                                data = signer.getCaptchaImageV2()
                            }
                        }) {
                            Text("重试")
                        }
                    }
                }
            }
        },
        dismissButton = {
            Button(onClick = {
                coroutineScope.launch {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                    data = signer.getCaptchaImageV2()
                }
            }) {
                Text("刷新验证码")
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    onDismiss()
                }
            ) {
                Text("取消")
            }
        }
    )
}