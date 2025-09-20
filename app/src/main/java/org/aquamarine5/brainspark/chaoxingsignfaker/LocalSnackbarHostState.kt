/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("") }

fun SnackbarHostState?.displaySnackbar(
    message: String,
    coroutineScope: CoroutineScope
) {
    this?.currentSnackbarData?.dismiss()
    coroutineScope.launch {
        this@displaySnackbar?.showSnackbar(message)
    }
}