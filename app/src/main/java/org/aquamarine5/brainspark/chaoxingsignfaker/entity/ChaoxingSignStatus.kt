/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class ChaoxingSignStatus(
    val isSuccess: MutableState<Boolean?> = mutableStateOf(null),
    val error:MutableState<String> = mutableStateOf(""),
){
    fun getText():String{
        return when(isSuccess.value){
            true->"签到成功"
            false->"失败！${error.value}"
            null->""
        }
    }
}
