/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Bundle
import android.os.Debug
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import com.umeng.analytics.MobclickAgent
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.NavigationBarItemData
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseDetailDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseDetailScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseListDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.CourseListScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LocationSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LoginDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.LoginPage
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.OtherUserDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.OtherUserGraphDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.OtherUserScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.QRCodeSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.SettingDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.SettingGraphDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.SettingScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.SignGraphDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.WelcomeDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.WelcomeScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.ChaoxingSignFakerTheme
import org.aquamarine5.brainspark.stackbricks.StackbricksPolicy
import org.aquamarine5.brainspark.stackbricks.StackbricksService
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuConfiguration
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuMessageProvider
import org.aquamarine5.brainspark.stackbricks.providers.qiniu.QiniuPackageProvider
import org.aquamarine5.brainspark.stackbricks.rememberStackbricksStatus
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    companion object {
        const val INTENT_EXTRA_EXIT_FLAG = "intent_extra_exit_flag"

        const val CLASS_TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SentryAndroid.init(this) {
            if (Debug.isDebuggerConnected()) {
                it.isEnabled = false
            }
            val versionName = packageManager.getPackageInfo(
                packageName,
                GET_META_DATA
            ).versionName!!
            if (versionName.contains("rc"))
                it.environment = "rc"
            else if (versionName.contains("beta"))
                it.environment = "beta"
            else if (versionName.contains("alpha")) {
                it.environment = "alpha"
                it.isAnrEnabled = false
            } else
                it.environment = "stable"
        }
        UMengHelper.preInit(this)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var isNewVersionAvailable by remember {
                mutableStateOf(false)
            }
            ChaoxingSignFakerTheme {
                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        val isNoBottomNavigationBar by remember(currentDestination) {
                            mutableStateOf(
                                listOf(
                                    WelcomeDestination::class,
                                    LoginDestination::class
                                ).any { currentDestination?.hasRoute(it) ?: false }.not()
                            )
                        }
                        AnimatedVisibility(
                            isNoBottomNavigationBar,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            BottomNavigation(
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                                elevation = 14.dp
                            ) {
                                val bottomBarItem = listOf(
                                    NavigationBarItemData(
                                        SignGraphDestination,
                                        "签到",
                                        painterResource(R.drawable.ic_clipboard_pen_line)
                                    ),
                                    NavigationBarItemData(
                                        OtherUserGraphDestination,
                                        "代签",
                                        painterResource(R.drawable.ic_users_round)
                                    ),
                                    NavigationBarItemData(
                                        SettingGraphDestination,
                                        "设置",
                                        painterResource(R.drawable.ic_settings)
                                    )
                                )
                                bottomBarItem.forEach { item ->
                                    val isSelected =
                                        currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } == true
                                    BottomNavigationItem(
                                        isSelected,
                                        onClick = {
                                            navController.navigate(item.destination) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            val iconColor by animateColorAsState(
                                                if (isSelected) LocalContentColor.current else
                                                    LocalContentColor.current.copy(ContentAlpha.medium),
                                                tween(300)
                                            )
                                            CompositionLocalProvider(LocalContentColor provides iconColor) {
                                                Column {
                                                    Spacer(modifier = Modifier.size(1.5.dp))
                                                    Box{
                                                        Icon(
                                                            item.icon,
                                                            contentDescription = item.name,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                        if(item.name=="设置"&&isNewVersionAvailable){
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .background(
                                                                        color = Color(0xFFF23E23),
                                                                        shape = CircleShape
                                                                    )
                                                                    .align(Alignment.TopEnd)
                                                                    .offset(x = 2.dp, y = (-2).dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        label = {
                                            Column {
                                                Spacer(modifier = Modifier.size(1.5.dp))
                                                Text(item.name, fontSize = 12.sp)
                                            }
                                        },
                                        alwaysShowLabel = false
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val stackbricksService= QiniuConfiguration(
                        "cdn.aquamarine5.fun",
                        referer = "http://cdn.aquamarine5.fun/",
                        configFilePath = "chaoxingsignfaker_stackbricks_v2_manifest.json",
                        okHttpClient = OkHttpClient().newBuilder()
                            .callTimeout(20, TimeUnit.MINUTES)
                            .readTimeout(20, TimeUnit.MINUTES)
                            .writeTimeout(20, TimeUnit.MINUTES)
                            .build()
                    ).let {
                        StackbricksService(
                            LocalContext.current,
                            QiniuMessageProvider(it),
                            QiniuPackageProvider(it),
                            rememberStackbricksStatus(),
                            stackbricksPolicy = StackbricksPolicy(
                                versionName = BuildConfig.VERSION_NAME,
                                isAllowedToDisableCheckUpdateOnLaunch = false,
                                isForceInstallValueCallback = false,
                                versionCode = null
                            ),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        var destination by remember { mutableStateOf<Any?>(null) }
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO){
                                val datastore = applicationContext.chaoxingDataStore.data.first()
                                if (datastore.agreeTerms) {
                                    UMengHelper.init(applicationContext)
                                    LocationClient.setAgreePrivacy(true)
                                    SDKInitializer.setAgreePrivacy(applicationContext, true)
                                }
                                destination =
                                    when {
                                        !datastore.agreeTerms -> WelcomeDestination
                                        !datastore.hasLoginSession() -> LoginDestination
                                        else -> {
                                            runCatching {
                                                ChaoxingHttpClient.loadFromDataStore(datastore)
                                                return@runCatching SignGraphDestination
                                            }.getOrElse {
                                                withContext(Dispatchers.Main){
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "初始化客户端失败，可能是网络问题或登录过期。",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                LoginDestination
                                            }
                                        }
                                    }
                                ChaoxingHttpClient.deviceCode =
                                    datastore.deviceCode ?: ChaoxingHttpClient.generateDeviceCode(
                                        applicationContext
                                    )
                            }
                        }
                        if (destination == null) {
                            CenterCircularProgressIndicator(isDelay = false)
                        } else
                            NavHost(
                                navController,
                                destination!!,
                                enterTransition = {
                                    fadeIn(
                                        animationSpec = tween(300)
                                    )
                                },
                                exitTransition = {
                                    fadeOut(
                                        animationSpec = tween(300)
                                    )
                                },
                            ) {
                                navigation<SignGraphDestination>(startDestination = CourseListDestination) {
                                    composable<CourseListDestination> {
                                        CourseListScreen(stackbricksService,navToDetailDestination = {
                                            navController.navigate(it)
                                        }, onNewVersionAvailable = {
                                            isNewVersionAvailable=true
                                        }, navToSettingDestination = {
                                            navController.navigate(SettingDestination) {
                                                popUpTo<CourseListDestination> { inclusive = true }
                                            }
                                        }) {
                                            navController.navigate(LoginDestination) {
                                                popUpTo<CourseListDestination> { inclusive = true }
                                            }
                                        }
                                    }

                                    composable<QRCodeSignDestination> {
                                        QRCodeSignScreen(it.toRoute(), navToOtherUser = {
                                            navController.navigate(OtherUserDestination)
                                        }) {
                                            navController.navigateUp()
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

                                    composable<CourseDetailDestination> {
                                        CourseDetailScreen(
                                            it.toRoute(),
                                            navToSignerDestination = { destination ->
                                                navController.navigate(destination)
                                            }) {
                                            navController.navigateUp()
                                        }
                                    }

                                    composable<PhotoSignDestination> {
                                        PhotoSignScreen(it.toRoute()) {
                                            navController.navigateUp()
                                        }
                                    }
                                }

                                navigation<OtherUserGraphDestination>(startDestination = OtherUserDestination) {
                                    composable<OtherUserDestination> {
                                        OtherUserScreen {
                                            navController.navigateUp()
                                        }
                                    }
                                }

                                navigation<SettingGraphDestination>(startDestination = SettingDestination) {
                                    composable<SettingDestination> {
                                        SettingScreen(stackbricksService)
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
