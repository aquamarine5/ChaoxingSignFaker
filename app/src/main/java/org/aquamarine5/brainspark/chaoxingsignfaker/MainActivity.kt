/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseDetailDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseDetailScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseListDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseListScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LocationSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LoginDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LoginPage
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.OtherUserDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.OtherUserScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.WelcomeDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.WelcomeScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.ChaoxingSignFakerTheme
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    companion object {
        const val INTENT_EXTRA_EXIT_FLAG = "intent_extra_exit_flag"

        const val CLASS_TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UMengHelper.preInit(this)
        enableEdgeToEdge()
        setContent {
            ChaoxingSignFakerTheme {
                val navController = rememberNavController()
                NavHost(navController,
                    runBlocking {
                        applicationContext.chaoxingDataStore.data.first().let {
                            if (it.agreeTerms) {
                                UMengHelper.init(applicationContext)
                                LocationClient.setAgreePrivacy(true)
                                SDKInitializer.setAgreePrivacy(applicationContext, true)
                            }
                            ChaoxingHttpClient.deviceCode =
                                if (it.deviceCode == null)
                                    ChaoxingHttpClient.generateDeviceCode(applicationContext)
                                else it.deviceCode
                            when {
                                !it.agreeTerms -> WelcomeDestination
                                !it.hasLoginSession() -> LoginDestination
                                else -> {
                                    ChaoxingHttpClient.loadFromDataStore(it)
                                    CourseListDestination
                                }
                            }
                        }
                    }
                ) {
                    composable<QRCodeSignDestination> {
                        QRCodeSignScreen(it.toRoute()) {
                            navController.navigateUp()
                        }
                    }

                    composable<OtherUserDestination> {
                        OtherUserScreen {
                            navController.navigateUp()
                        }
                    }

                    composable<WelcomeDestination> {
                        WelcomeScreen {
                            navController.navigate(LoginDestination) {
                                popUpTo<WelcomeDestination>()
                            }
                        }
                    }

                    composable<LoginDestination> {
                        LoginPage {
                            navController.navigate(CourseListDestination) {
                                popUpTo<LoginDestination> { inclusive = true }
                            }
                        }
                    }

                    composable<GetLocationDestination>(
                        typeMap = mapOf(
                            typeOf<ChaoxingSignActivityEntity>() to ChaoxingSignActivityEntity.SignActivityNavType
                        )
                    ) {
                        LocationSignScreen(it.toRoute()) {
                            navController.navigateUp()
                        }
                    }

                    composable<CourseListDestination> {
                        CourseListScreen(navToOtherUserDestination = {
                            navController.navigate(OtherUserDestination())
                        }) {
                            navController.navigate(it, navOptions {
                                popUpTo<CourseListDestination> { saveState = true }
                                restoreState = true
                            })
                        }
                    }

                    composable<CourseDetailDestination> {
                        CourseDetailScreen(it.toRoute(), navToSignerDestination = { destination ->
                            navController.navigate(destination)
                        }) {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.getBooleanExtra(INTENT_EXTRA_EXIT_FLAG, false)) {
            finish()
        }
        super.onNewIntent(intent)
    }

    override fun onResume() {
        MobclickAgent.onResume(this)
        super.onResume()
    }

    override fun onPause() {
        MobclickAgent.onPause(this)
        super.onPause()
    }

    override fun onStop() {
        MobclickAgent.onKillProcess(this)
        super.onStop()
    }

    override fun onDestroy() {
        MobclickAgent.onKillProcess(this)
        super.onDestroy()
    }
}
