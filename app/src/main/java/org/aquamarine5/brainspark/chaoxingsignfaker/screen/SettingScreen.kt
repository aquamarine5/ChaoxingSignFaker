/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.BuildConfig
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AnalyserCard
import org.aquamarine5.brainspark.chaoxingsignfaker.components.SponsorCard
import org.aquamarine5.brainspark.stackbricks.ApplicationBuildConfig
import org.aquamarine5.brainspark.stackbricks.StackbricksComponent
import org.aquamarine5.brainspark.stackbricks.StackbricksEventTrigger
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.StackbricksVersionData
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuConfiguration
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuMessageProvider
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuPackageProvider
import org.aquamarine5.brainspark.stackbricks.rememberStackbricksStatus
import java.util.concurrent.TimeUnit

@Serializable
object SettingGraphDestination

@Serializable
object SettingDestination

@Composable
fun SettingScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        val stackbricksState = rememberStackbricksStatus()
        val context = LocalContext.current
        QiniuConfiguration(
            "cdn.aquamarine5.fun",
            referer = "http://cdn.aquamarine5.fun/",
            configFilePath = "chaoxingsignfaker_stackbricks_v2_manifest.json",
            okHttpClient = ChaoxingHttpClient.instance!!.okHttpClient.newBuilder()
                .callTimeout(20, TimeUnit.MINUTES)
                .readTimeout(20, TimeUnit.MINUTES)
                .writeTimeout(20, TimeUnit.MINUTES)
                .build()
        ).let {
            val userEntity = ChaoxingHttpClient.instance!!.userEntity
            StackbricksComponent(
                StackbricksService(
                    LocalContext.current,
                    QiniuMessageProvider(it),
                    QiniuPackageProvider(it),
                    stackbricksState,
                    buildConfig = ApplicationBuildConfig(
                        versionName = BuildConfig.VERSION_NAME,
                        isAllowedToDisableCheckUpdateOnLaunch = false,
                        versionCode = null
                    ),
                ),
                trigger = object : StackbricksEventTrigger() {
                    override fun onChannelChanged(isTestChannel: Boolean) {
                        UMengHelper.onStackbricksTestChannelChangedEvent(
                            context,
                            userEntity,
                            isTestChannel
                        )
                    }

                    override fun onCheckUpdate(isTestChannel: Boolean) {
                        UMengHelper.onStackbricksCheckUpdateEvent(context, userEntity)
                    }

                    override fun onCheckUpdateOnLaunchChanged(isChecked: Boolean) {
                        UMengHelper.onStackbricksCheckOnLaunchChangedEvent(
                            context,
                            userEntity,
                            isChecked
                        )
                    }

                    override fun onDownloadPackage() {

                    }

                    override fun onInstallPackage(
                        isTestChannel: Boolean,
                        versionData: StackbricksVersionData
                    ) {
                        if (isTestChannel)
                            UMengHelper.onStackbricksInstallTestChannelEvent(
                                context,
                                userEntity,
                                versionData
                            )
                        else
                            UMengHelper.onStackbricksInstallNewestEvent(
                                context,
                                userEntity,
                                versionData
                            )
                    }

                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        SponsorCard()

        AnalyserCard()
    }
}