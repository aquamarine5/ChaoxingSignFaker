/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay


private const val WAIT_DELAY = 500L

@Composable
fun CenterCircularProgressIndicator(
    modifier: Modifier = Modifier,
    isDelay: Boolean = true
) {
    var isShow by remember { mutableStateOf(isDelay.not()) }
    if (isDelay) {
        LaunchedEffect(Unit) {
            delay(WAIT_DELAY)
            isShow = true
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isShow) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(modifier)
            )
        }
    }
}