/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val Orange = Color(0xFFFCC307)

val Gold = Color(0xFFD4AF37)
val GoldDark = Color(0xFFFFD700)

val Silver = Color(0xFF8C8C8C)
val SilverDark = Color(0xFFE0E0E0)

val Bronze = Color(0xFFB3672B)
val BronzeDark = Color(0xFFE0A165)

@Composable
fun getGoldColor() = if (isSystemInDarkTheme()) GoldDark else Gold

@Composable
fun getSilverColor() = if (isSystemInDarkTheme()) SilverDark else Silver

@Composable
fun getBronzeColor() = if (isSystemInDarkTheme()) BronzeDark else Bronze

@Composable
fun getLowAttentionGrayColor() = if (isSystemInDarkTheme()) Color.DarkGray else Color.LightGray

@Composable
fun getHighAttentionGrayColor() = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray