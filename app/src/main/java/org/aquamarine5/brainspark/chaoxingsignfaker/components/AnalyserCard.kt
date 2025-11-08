/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingAnalyser
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun AnalyserCard() {
    LocalContext.current.let { context ->
        val analyser = rememberSaveable(saver = ChaoxingAnalyser.MutableStateAnalyser.Saver) {
            ChaoxingAnalyser.createStateAnalyser()
        }
        LaunchedEffect(analyser.isLoaded) {
            if (analyser.isLoaded.value.not())
                ChaoxingAnalyser.setupStateAnalyser(context)
        }
        val fontGilroy = remember {
            FontFamily(Font(R.font.gilroy))
        }
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
                CompositionLocalProvider(LocalContentColor provides if (isSystemInDarkTheme()) Color.Black else Color.White) {
                    Icon(
                        painterResource(R.drawable.ic_chart_column),
                        contentDescription = "analyser"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedVisibility(
                        analyser.isLoaded.value,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Text("使用次数统计", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            analyser.apply {
                                FlowRow(
                                    modifier=Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    maxItemsInEachRow = 4
                                ) {
                                    listOf(
                                        photoSignCount to painterResource(R.drawable.ic_camera),
                                        locationSignCount to painterResource(R.drawable.ic_map_pin),
                                        qrcodeSignCount to painterResource(R.drawable.ic_scan_qr_code),
                                        clickSignCount to painterResource(R.drawable.ic_square_mouse_pointer),
                                        gestureSignCount to painterResource(R.drawable.ic_pattern_locking),
                                        passwordSignCount to painterResource(R.drawable.ic_binary),
                                        otherUserSignCount to painterResource(R.drawable.ic_users_round)
                                    ).forEach {
                                        Row(modifier=Modifier.padding(0.dp,0.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(it.second, null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier=Modifier.width(3.dp))
                                            Text(it.first.value.toString(), fontFamily = fontGilroy, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}