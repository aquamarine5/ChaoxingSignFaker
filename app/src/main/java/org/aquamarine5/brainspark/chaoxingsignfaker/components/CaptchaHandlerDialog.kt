/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.MutableLiveData
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCaptchaDataEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

@Composable
fun CaptchaHandlerDialog(
    signer: ChaoxingSigner,
    liveData: MutableLiveData<Result<String>?>,
    onDismiss: () -> Unit,
) {
    var data by remember { mutableStateOf<ChaoxingCaptchaDataEntity?>(null) }
    var sliderPosition by remember(data) { mutableFloatStateOf(10f) }
    val maxSliderPosition = 320f - 10f
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        signer.getCaptchaImage {
            data = it
        }
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
                            .size(320.dp, 160.dp)
                            .background(Color.Gray)
                    ) {
                        AsyncImage(
                            model = data!!.shadeImageUrl,
                            contentDescription = "背景图",
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0f)
                                .size(320.dp, 160.dp),
                        )

                        AsyncImage(
                            model = data!!.cutoutImageUrl,
                            contentDescription = "滑块",
                            modifier = Modifier
                                .offset { IntOffset(sliderPosition.toInt(), 0) }
                                .zIndex(1f)
                                .size(56.dp, 160.dp)
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
                                    signer.checkCaptchaResult(sliderPosition, data!!)
                                        .let { result ->
                                            if (result == null) {
                                                Toast.makeText(
                                                    context,
                                                    "验证失败，请重试",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                signer.getCaptchaImage {
                                                    data = it
                                                }
                                            } else {
                                                liveData.postValue(Result.success(result))
                                                onDismiss()
                                            }
                                        }
                                }.onFailure {
                                    liveData.postValue(Result.failure(it))
                                    onDismiss()
                                }
                            }
                        },
                        valueRange = 10f..maxSliderPosition,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                }
            ) {
                Text("确认")
            }
        }
    )
}