/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GestureSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PasswordSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination

data class ExternalSignRequest(
    val classId: Int?,
    val courseId: Long?,
    val fid: Int?,
    val activeId: Long?,
    val signType: String?,
    val courseName: String?
)

fun Intent.parseExternalSignRequest(): ExternalSignRequest? =
    if (action != ExternalSignEntry.ACTION_OPEN_SIGN) null
    else ExternalSignRequest(
        classId = if (hasExtra("classId")) getIntExtra("classId", 0) else null,
        courseId = if (hasExtra("courseId")) getLongExtra("courseId", 0L) else null,
        fid = if (hasExtra("fid")) getIntExtra("fid", 0) else null,
        activeId = if (hasExtra("activeId")) getLongExtra("activeId", 0L) else null,
        signType = getStringExtra("signType")?.takeIf { it.isNotBlank() },
        courseName = getStringExtra("courseName")?.takeIf { it.isNotBlank() }
    )

object ExternalSignEntry {
    const val ACTION_OPEN_SIGN =
        "org.aquamarine5.brainspark.chaoxingsignfaker.action.OPEN_SIGN"

    fun destinationOf(
        activeId: Long,
        classId: Int,
        courseId: Long,
        signType: String
    ): SignDestination? =
        when (signType.lowercase()) {
            "gesture" -> GestureSignDestination(
                activeId, classId, courseId, "", null, null, false, false
            )

            "location" -> GetLocationDestination(
                activeId, classId, courseId, "", null, null, false, false
            )

            "password" -> PasswordSignDestination(
                activeId, classId, courseId, "", null, null, false, false
            )

            "photo" -> PhotoSignDestination(
                activeId, classId, courseId, "", null, null, false, false
            )

            "qrcode" -> QRCodeSignDestination(
                activeId, classId, courseId, "", null, null, false, false
            )

            else -> null
        }
}
