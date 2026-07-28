/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import androidx.compose.runtime.Composable

@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.PROPERTY
)
annotation class OnlyAppDevelopedMode()

@OnlyAppDevelopedMode
fun disableComposableCode(block: @Composable () -> Unit) {
}

@OnlyAppDevelopedMode
fun disableCode(block: suspend () -> Unit) {
}
