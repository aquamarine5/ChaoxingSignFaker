/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FaceRecognitionComponent(
    signUserName: List<Pair<String, String>>,
    onCancel: () -> Unit,
    onFinish: (Map<String, Bitmap>) -> Unit
) {
    var faceImageCapturedIndex by remember { mutableIntStateOf(0) }
    BackHandler {
        onCancel()
    }

    CameraComponent(signUserName.size, isDefaultBackCamera = false, onNextPhoto = {
        faceImageCapturedIndex++
    }, content = {
        Row(
            modifier = Modifier
                .animateContentSize()
                .background(
                    Color(0x88888888),
                    RoundedCornerShape(14.dp)
                )
                .border(
                    BorderStroke(
                        2.dp, Color(0xFF444444)
                    ), RoundedCornerShape(14.dp)
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("拍摄给 ${signUserName[faceImageCapturedIndex].second} 人脸识别的图片")
        }
    }) {
        onFinish(it.mapIndexed { index, bitmap -> signUserName[index].first to bitmap }
            .associate { it })
    }
}