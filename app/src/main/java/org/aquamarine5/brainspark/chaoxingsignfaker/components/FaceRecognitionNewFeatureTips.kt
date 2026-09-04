/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore

@Composable
fun FaceRecognitionNewFeatureTips(isDisplayNewFeature: MutableState<Boolean>) {
    Column {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF12AA9C)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp, 6.dp)
                .zIndex(1f)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp, 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_scan_face),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    "感谢 @miloce 提供人脸识别技术支持。\n拍摄或上传人脸识别照片时，请选择竖屏正脸睁眼照片。",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.W500,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        val context = LocalContext.current
        NewFeatureTipsCard(
            isDisplayNewFeature,
            tipsContent = {
                Text(
                    "现在随地大小签可以保存用户的人脸照片，可以不需要手动上传了。",
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            },
            modifier = Modifier.padding(0.dp, 1.dp)
        ) {
            context.chaoxingDataStore.updateData {
                it.toBuilder().setLearntTooltips(
                    it.learntTooltips.toBuilder().setSaveFaceRecognitionImagesToLocal(true)
                        .build()
                ).build()
            }
        }
    }
}