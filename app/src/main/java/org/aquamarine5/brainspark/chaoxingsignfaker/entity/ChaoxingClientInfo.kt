/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingDeviceInfoHelper

data class ChaoxingClientInfo(
    val identity: String,
    val packageName: String,
    val displayName: String,
    val userAgentTemplate: String,
    private val productId: String,
    private val versionName: String,
    private val versionCode: String,
    private val apiVersion: String,
    private val isAzeroth: Boolean = false,
    val isCustom: Boolean = false,
) {
    fun resolveUserAgent(uniqueId: String, deviceModel: String): String =
        if (isCustom) {
            ChaoxingDeviceInfoHelper.overrideDeviceModel(userAgentTemplate, deviceModel)
        } else {
            ChaoxingDeviceInfoHelper.buildUserAgent(
                httpAgent = userAgentTemplate.replace("{model}", deviceModel),
                deviceModel = deviceModel,
                packageName = packageName,
                productId = productId,
                versionName = versionName,
                versionCode = versionCode,
                apiVersion = apiVersion,
                uniqueId = uniqueId,
                isAzeroth = isAzeroth
            )
        }

    companion object {
        const val CUSTOM_IDENTITY = "@@custom"

        val DEFAULT = ChaoxingClientInfo(
            identity = "@@default",
            packageName = "com.chaoxing.mobile",
            displayName = "学习通",
            userAgentTemplate = ChaoxingDeviceInfoHelper.systemHttpAgentTemplate(),
            productId = "3",
            versionName = "6.7.5",
            versionCode = "10941",
            apiVersion = "314"
        )

        val XUEZAIXIDIAN = ChaoxingClientInfo(
            identity = "@@xuezaixidian",
            packageName = "com.chaoxing.mobile.xuezaixidian",
            displayName = "学在西电",
            userAgentTemplate = ChaoxingDeviceInfoHelper.systemHttpAgentTemplate(),
            productId = "1000149",
            versionName = "6.3.7",
            versionCode = "6005",
            apiVersion = "249"
        )

        val presets: List<ChaoxingClientInfo> = listOf(DEFAULT, XUEZAIXIDIAN)

        fun fromIdentity(identity: String): ChaoxingClientInfo? {
            if (identity == CUSTOM_IDENTITY) return null
            if (identity.isBlank()) return DEFAULT
            return presets.firstOrNull { it.identity == identity }
        }

        fun resolve(userAgent: String, packageName: String): ChaoxingClientInfo =
            if (!ChaoxingDeviceInfoHelper.isValidUserAgent(userAgent)) DEFAULT
            else fromIdentity(userAgent) ?: DEFAULT.copy(
                identity = CUSTOM_IDENTITY,
                packageName = packageName.ifBlank { DEFAULT.packageName },
                displayName = "自定义 USER_AGENT",
                userAgentTemplate = userAgent,
                isCustom = true
            )
    }
}
