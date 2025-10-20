/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import android.content.pm.PackageInfo
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData
import java.security.MessageDigest

object UMengHelper {
    private const val API_KEY = "67d42c1c48ac1b4f87e7edae"
    private const val API_CHANNEL = BuildConfig.UMENG_CHANNEL

    private const val EVENT_TAG_ACCOUNT_LOGIN = "account_login"
    private const val EVENT_TAG_SIGN_LOCATION = "sign_location"
    private const val EVENT_TAG_SIGN_PASSWORD = "sign_password"
    private const val EVENT_TAG_SIGN_QR_CODE = "sign_qr_code"
    private const val EVENT_TAG_SIGN_PHOTO = "sign_photo"
    private const val EVENT_TAG_SIGN_CLICK = "sign_click"
    private const val EVENT_TAG_ADD_OTHER_USER = "account_add_other_user"
    private const val EVENT_TAG_GOTO_SPONSOR_WECHAT = "sponsor_wechat_goto"
    private const val EVENT_TAG_ILLEGAL_CHANNEL = "illegal_channel"

    @Deprecated("")
    private const val EVENT_TAG_CAPTCHA_SUCCESS = "captcha_success"

    private const val EVENT_TAG_STACKBRICKS_CHECK_UPDATE = "stackbricks_check_update"
    private const val EVENT_TAG_STACKBRICKS_INSTALL_NEWEST = "stackbricks_install_newest"
    private const val EVENT_TAG_STACKBRICKS_INSTALL_TEST_CHANNEL =
        "stackbricks_install_test_channel"
    private const val EVENT_TAG_STACKBRICKS_CHECK_ON_LAUNCH_CHANGED =
        "stackbricks_check_on_launch_status_changed"
    private const val EVENT_TAG_STACKBRICKS_TEST_CHANNEL_CHANGED =
        "stackbricks_test_channel_status_changed"

    @OptIn(ExperimentalStdlibApi::class)
    fun md5(str: String): String {
        return MessageDigest.getInstance("MD5").digest(str.toByteArray()).toHexString()
    }

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

    private fun onEvent(context: Context, eventId: String, data: Map<String, Any>) {
        MobclickAgent.onEventObject(context, eventId, data)
    }

    fun onLoginEvent(context: Context, phoneNumber: String) {
        onEvent(context, EVENT_TAG_ACCOUNT_LOGIN, mapOf("phone" to phoneNumber))
    }

    fun onIllegalChannelEvent(context: Context, applicationInfo: PackageInfo) {
        onEvent(
            context,
            EVENT_TAG_ILLEGAL_CHANNEL,
            mapOf(
                "name" to applicationInfo.applicationInfo!!.name,
                "label" to context.packageManager.getApplicationLabel(applicationInfo.applicationInfo!!),
                "version" to applicationInfo.versionName.toString(),
                "user" to ChaoxingHttpClient.instance!!.userEntity.name
            )
        )
    }

    suspend fun onSignCodeEvent(
        context: Context,
        name: String, isOtherUser: Boolean = false
    ) {
        onEvent(
            context, EVENT_TAG_SIGN_PASSWORD, mapOf(
                "user" to name
            )
        )
        ChaoxingAnalyser.onPasswordSignEvent(context)
        if (isOtherUser) ChaoxingAnalyser.onOtherUserSignEvent(context)
    }

    suspend fun onSignLocationEvent(
        context: Context,
        postLocationEntity: ChaoxingLocationSignEntity,
        name: String, isOtherUser: Boolean = false
    ) {
        onEvent(
            context, EVENT_TAG_SIGN_LOCATION, mapOf(
                "address" to postLocationEntity.address,
                "user" to name,
            )
        )
        ChaoxingAnalyser.onLocationSignEvent(context)
        if (isOtherUser) ChaoxingAnalyser.onOtherUserSignEvent(context)
    }

    suspend fun onSignQRCodeEvent(
        context: Context,
        name: String,
        isOtherUser: Boolean = false
    ) {
        onEvent(context, EVENT_TAG_SIGN_QR_CODE, mapOf("user" to name))
        ChaoxingAnalyser.onQRCodeSignEvent(context)
        if (isOtherUser) ChaoxingAnalyser.onOtherUserSignEvent(context)
    }

    suspend fun onSignClickEvent(
        context: Context,
        name: String,
        isOtherUser: Boolean = false
    ) {
        onEvent(context, EVENT_TAG_SIGN_CLICK, mapOf("user" to name))
        ChaoxingAnalyser.onClickSignEvent(context)
        if (isOtherUser) ChaoxingAnalyser.onOtherUserSignEvent(context)
    }

    suspend fun onSignPhotoEvent(
        context: Context,
        name: String,
        isOtherUser: Boolean = false
    ) {
        onEvent(context, EVENT_TAG_SIGN_PHOTO, mapOf("user" to name))
        ChaoxingAnalyser.onPhotoSignEvent(context)
        if (isOtherUser) ChaoxingAnalyser.onOtherUserSignEvent(context)
    }

    fun onAccountOtherUserAddEvent(context: Context, userEntity: ChaoxingOtherUserSession) {
        onEvent(
            context,
            EVENT_TAG_ADD_OTHER_USER,
            mapOf("phone" to userEntity.phoneNumber, "user" to userEntity.name)
        )
    }

    fun onGotoSponsorWechatEvent(context: Context, userEntity: ChaoxingUserEntity) {
        onEvent(context, EVENT_TAG_GOTO_SPONSOR_WECHAT, mapOf("user" to userEntity.name))
    }

    fun onStackbricksCheckUpdateEvent(context: Context, userEntity: ChaoxingUserEntity) {
        onEvent(context, EVENT_TAG_STACKBRICKS_CHECK_UPDATE, mapOf("user" to userEntity.name))
    }

    fun onStackbricksInstallNewestEvent(
        context: Context,
        userEntity: ChaoxingUserEntity,
        versionData: StackbricksVersionData
    ) {
        onEvent(
            context,
            EVENT_TAG_STACKBRICKS_INSTALL_NEWEST,
            mapOf("user" to userEntity.name, "version" to versionData.versionName)
        )
    }

    fun onStackbricksInstallTestChannelEvent(
        context: Context,
        userEntity: ChaoxingUserEntity,
        versionData: StackbricksVersionData
    ) {
        onEvent(
            context,
            EVENT_TAG_STACKBRICKS_INSTALL_TEST_CHANNEL,
            mapOf("user" to userEntity.name, "version" to versionData.versionName)
        )
    }

    fun onStackbricksCheckOnLaunchChangedEvent(
        context: Context,
        userEntity: ChaoxingUserEntity,
        status: Boolean
    ) {
        onEvent(
            context,
            EVENT_TAG_STACKBRICKS_CHECK_ON_LAUNCH_CHANGED,
            mapOf("user" to userEntity.name, "status" to status)
        )
    }

    fun onStackbricksTestChannelChangedEvent(
        context: Context,
        userEntity: ChaoxingUserEntity,
        status: Boolean
    ) {
        onEvent(
            context,
            EVENT_TAG_STACKBRICKS_TEST_CHANNEL_CHANGED,
            mapOf("user" to userEntity.name, "status" to status)
        )
    }
}