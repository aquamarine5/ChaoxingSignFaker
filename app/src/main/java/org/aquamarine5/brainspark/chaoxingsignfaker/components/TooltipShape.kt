/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class TooltipShape(private val cornerRadius: Dp, private val tipSize: Dp,private val tipXPadding:Dp=0.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val tipSizePx = with(density) { tipSize.toPx() }
        val tipX = size.width - cornerRadiusPx - tipSizePx- with(density){tipXPadding.toPx()}

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height - tipSizePx
                    ),
                    cornerRadius = CornerRadius(cornerRadiusPx)
                )
            )
            moveTo(tipX, size.height - tipSizePx)
            lineTo(tipX + tipSizePx / 2, size.height)
            lineTo(tipX + tipSizePx, size.height - tipSizePx)
            close()
        }
        return Outline.Generic(path)
    }
}