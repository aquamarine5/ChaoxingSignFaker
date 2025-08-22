/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingSignHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignOutEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignOutRedirectTips(
    signoffData: ChaoxingSignOutEntity,
    onRedirect: (Any) -> Unit
) {
    with(signoffData) {
        val status = if (signInId != null) {
            ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_OUT
        } else if (signOffPublishTime != null) {
            if (signOffId == null)
                ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_IN_UNPUBLISHED
            else ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_IN_PUBLISHED
        } else {
            ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.COMMON
        }
        val coroutineScope = rememberCoroutineScope()

        if (status != ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.COMMON)
            Card(
                onClick = {
                    coroutineScope.launch {
                        onRedirect(
                            ChaoxingSignHelper.getRedirectDestination(
                                if (status == ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_OUT) signInId!! else signOffId!!,
                                classId,
                                courseId
                            )
                        )
                    }
                },
                enabled = status != ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_IN_UNPUBLISHED,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFECC11), disabledContainerColor = Color(0xFFFECC11)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp, 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painterResource(if (status == ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_OUT) R.drawable.ic_calendar_arrow_up else R.drawable.ic_calendar_arrow_down),
                        contentDescription = "Info",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        when (status) {
                            ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_OUT -> "这是一个签退活动，请确保已经签到了本签退活动的主签到活动。\n点击跳转到主签到活动进行签到。"
                            ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_IN_PUBLISHED -> "此签到已发布签退活动。\n点击跳转到签退活动进行签退。"
                            ChaoxingActivityHelper.SIGN_REDIRECT_STATUS.SIGN_IN_UNPUBLISHED -> "此签到活动设置了签退活动，将在${
                                SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date(signOffPublishTime!!))
                            }发布，请发布后及时签退。"

                            else -> ""
                        }, color = Color.Black,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.W500
                    )
                }
            }
    }
}