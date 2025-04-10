/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingAnalyser
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun AnalyserCard() {
    LocalContext.current.let { context ->
//        var analyser = rememberSaveable(saver = ChaoxingAnalyser.MutableStateAnalyser.Saver) {
//            ChaoxingAnalyser.MutableStateAnalyser()
//        }
        var analyser by remember{ mutableStateOf<ChaoxingAnalyser.MutableStateAnalyser?>(null)}
        LaunchedEffect(Unit) {
            if (analyser==null)
                analyser = ChaoxingAnalyser.createStateAnalyser(context)
        }
        val fontGilroy = SpanStyle(
            fontFamily = FontFamily(Font(R.font.gilroy)),
            fontSize = 13.sp
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF22A2C3))
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp, 8.dp)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_chart_column), contentDescription = "analyser")
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedVisibility(
                    analyser!=null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Text("使用次数统计", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        analyser?.apply {
                            listOf(
                                "位置签到" to locationSignCount,
                                "二维码签到" to qrcodeSignCount,
                                "拍照签到" to photoSignCount,
                                "代签次数" to otherUserSignCount
                            ).forEach {
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(fontSize = 14.sp)) { append("${it.first}: ") }
                                        withStyle(fontGilroy) {
                                            append("${it.second.value}")
                                        }
                                    }, modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}