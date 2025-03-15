package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.baidu.mapapi.SDKInitializer
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.CourseDetailDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.CourseDetailScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.CourseListDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.CourseListScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.GetLocationDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.GetLocationPage
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.LocationSignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.LocationSignScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.LoginDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.LoginPage
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.WelcomeDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.WelcomeScreen
import org.aquamarine5.brainspark.chaoxingsignfaker.ui.theme.ChaoxingSignFakerTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val INTENT_EXTRA_EXIT_FLAG = "intent_extra_exit_flag"
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
                                withContext(Dispatchers.IO) {
                                    UMengHelper.init(applicationContext)
                                    SDKInitializer.setAgreePrivacy(applicationContext, true)
                                }
                            }
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
                    composable<GetLocationDestination> {
                        GetLocationPage(it.toRoute()) { destination ->
                            navController.navigate(destination)
                        }
                    }
                    composable<CourseListDestination> {
                        CourseListScreen {
                            navController.navigate(it, navOptions {
                                popUpTo<CourseListDestination> { saveState = true }
                                restoreState = true
                            })
                        }
                    }

                    composable<LocationSignDestination> {
                        LocationSignScreen(it.toRoute())
                    }

                    composable<CourseDetailDestination> {
                        CourseDetailScreen(it.toRoute(), navToSignerDestination = { destination->
                            navController.navigate(destination)
                        }) {
                            navController.navigate(CourseListDestination)
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

    override fun onStop() {
        MobclickAgent.onKillProcess(this)
        super.onStop()
    }

    override fun onDestroy() {
        MobclickAgent.onKillProcess(this)
        super.onDestroy()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChaoxingSignFakerTheme {
        Greeting("Android")
    }
}