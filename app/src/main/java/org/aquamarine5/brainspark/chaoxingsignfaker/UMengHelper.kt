/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity

object UMengHelper {
    private const val API_KEY = "67d42c1c48ac1b4f87e7edae"
    private const val API_CHANNEL = "WXPublish"

    const val EVENT_TAG_ACCOUNT_LOGIN = "account_login"
    const val EVENT_TAG_SIGN_LOCATION = "sign_location"
    fun preInit(context: Context) {
        UMConfigure.preInit(context, API_KEY, API_CHANNEL)
    }

    fun init(context: Context) {
        UMConfigure.init(context, API_KEY, API_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")
    }

    fun profileSignIn(user: ChaoxingUserEntity, phoneNumber: String) {
        MobclickAgent.onProfileSignIn(user.uid.toString())
        MobclickAgent.userProfileMobile(phoneNumber)
        MobclickAgent.userProfile("name", user.name)
    }

    fun profileSignOff() {
        MobclickAgent.onProfileSignOff()
    }

    fun onEvent(context: Context, eventId: String, data: Map<String, Any>) {
        MobclickAgent.onEventObject(context, eventId, data)
    }

    fun onLoginEvent(context: Context, phoneNumber: String) {
        onEvent(context, EVENT_TAG_ACCOUNT_LOGIN, mapOf("phone" to phoneNumber))
    }

    fun onSignLocationEvent(context: Context, postLocationEntity: ChaoxingLocationSignEntity) {
        onEvent(
            context, EVENT_TAG_SIGN_LOCATION, mapOf(
                "latitude" to postLocationEntity.latitude,
                "longitude" to postLocationEntity.longitude,
                "address" to postLocationEntity.address
            )
        )
    }
}