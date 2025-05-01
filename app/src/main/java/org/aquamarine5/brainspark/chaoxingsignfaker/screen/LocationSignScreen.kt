/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import io.sentry.Sentry
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.GetLocationComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingLocationSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

@Serializable
data class GetLocationDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): GetLocationDestination {
            return GetLocationDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@Composable
fun LocationSignScreen(
    destination: GetLocationDestination,
    navToCourseDetailDestination: () -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
    var isSignForOther by remember { mutableStateOf(false) }
    var signInfo by remember { mutableStateOf<ChaoxingLocationDetailEntity?>(null) }
    val signer = ChaoxingLocationSigner(ChaoxingHttpClient.instance!!, destination)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        runCatching {
            signInfo = signer.getLocationSignInfo()
            isAlreadySigned = signer.preSign()
        }.onFailure {
            if ((it is ChaoxingPredictableException).not()) {
                Sentry.captureException(it)
            }
            Toast.makeText(context, "获取签到事件详情失败", Toast.LENGTH_SHORT).show()
            navToCourseDetailDestination()
        }
    }
    when (isAlreadySigned) {
        true -> {
            AlreadySignedNotice(onSignForOtherUser = {
                isAlreadySigned=false
                isSignForOther = true
            }, onDismiss = {
                isAlreadySigned = false
            }) { navToCourseDetailDestination() }
        }

        false -> {
            var isGetLocation by remember { mutableStateOf(false) }
            val signStatus = remember { mutableStateListOf(ChaoxingSignStatus()) }
            var isSelfForSign by remember { mutableStateOf(false) }
            val otherUserSessionForSignList = remember { mutableListOf<ChaoxingOtherUserSession>() }

            OtherUserSelectorComponent(
                navToOtherUser = { navToOtherUserDestination() },
                signStatus = signStatus,
                isCurrentAlreadySigned = isSignForOther,
            ) { isSelf, otherUserSessionList ->
                isSelfForSign = isSelf
                otherUserSessionForSignList.addAll(otherUserSessionList)
                isGetLocation = true
            }

            AnimatedVisibility(
                isGetLocation, enter =
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(
                    animationSpec = tween(300)
                ), exit =
                scaleOut(targetScale = 0.8f, animationSpec = tween(300)) + fadeOut(
                    animationSpec = tween(300)
                ), modifier = Modifier.zIndex(1f)
            ) {
                BackHandler(isGetLocation) {
                    isGetLocation=false
                }
                GetLocationComponent(signInfo, confirmButtonText = {
                    Text("签到")
                }) { result ->
                    isGetLocation=false
                    coroutineScope.launch {
                        runCatching {
                            signStatus[0].loading()
                            signer.sign(result)
                        }.onSuccess {
                            signStatus[0].success()
                            UMengHelper.onSignLocationEvent(
                                context,
                                result,
                                ChaoxingHttpClient.instance!!.userEntity
                            )
                        }.onFailure {
                            signStatus[0].failed(it)
                            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                            if ((it is ChaoxingPredictableException).not()) {
                                Sentry.captureException(it)
                            }
                        }
                        otherUserSessionForSignList.forEachIndexed { index, userSession ->
                            runCatching {
                                signStatus[index + 1].loading()
                                ChaoxingHttpClient.loadFromOtherUserSession(userSession).also { client->
                                    ChaoxingLocationSigner(client,destination).apply {
                                        if(preSign()){
                                            signStatus[index + 1].failed(ChaoxingSigner.AlreadySignedException())
                                        }else{
                                            sign(result)
                                        }
                                    }
                                }
                            }.onSuccess {
                                signStatus[index + 1].success()
                                UMengHelper.onSignLocationEvent(
                                    context,
                                    result,
                                    ChaoxingHttpClient.instance!!.userEntity,
                                    true
                                )
                            }.onFailure {
                                it.printStackTrace()
                                if ((it is ChaoxingPredictableException).not()) {
                                    Sentry.captureException(it)
                                }
                                signStatus[index + 1].failed(it)
                            }
                        }
                    }
                }
            }
        }

        null -> {
            CenterCircularProgressIndicator()
        }
    }


}