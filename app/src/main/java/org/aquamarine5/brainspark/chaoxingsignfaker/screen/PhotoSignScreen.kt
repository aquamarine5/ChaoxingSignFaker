/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CameraComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.components.OtherUserSelectorComponent
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner

@Serializable
data class PhotoSignDestination(
    val activeId: Long,
    val classId: Int,
    val courseId: Int,
    val extContent: String
) {
    companion object {
        fun parseFromSignActivityEntity(activityEntity: ChaoxingSignActivityEntity): PhotoSignDestination {
            return PhotoSignDestination(
                activityEntity.id,
                activityEntity.course.classId,
                activityEntity.course.courseId,
                activityEntity.ext
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoSignScreen(
    destination: PhotoSignDestination,
    navBack: () -> Unit,
    navToOtherUserDestination: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val signer = ChaoxingPhotoSigner(ChaoxingHttpClient.instance!!, destination)
        var isImage by remember { mutableStateOf<Boolean?>(null) }
        var isAlreadySigned by remember { mutableStateOf<Boolean?>(null) }
        var isSignSuccess by remember { mutableStateOf(false) }
        var isShowPhotoPicker by remember { mutableStateOf(false) }
        var isForSelf by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            runCatching {
                isImage = signer.ifPhotoRequiredLogin()
                isAlreadySigned = signer.preSign()
            }.onFailure {
                if ((it is ChaoxingPredictableException).not()) {
                    Sentry.captureException(it)
                }
                Toast.makeText(context, "获取签到事件详情失败", Toast.LENGTH_SHORT).show()
                navBack()
            }
        }
        when (isAlreadySigned) {
            false -> {
                if (isImage == false) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .background(Color.DarkGray, RoundedCornerShape(18.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painterResource(R.drawable.ic_info),
                                contentDescription = "Info",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "这是一个普通的点击签到，不会收集任何其他的信息，所以我们推荐对于这种签到使用学习通APP而不是随地大小签。",
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.W500
                            )
                        }
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("cxstudy://")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                        }) {
                            Text("跳转到学习通")
                        }
                        val signStatus = remember { mutableListOf(ChaoxingSignStatus()) }
                        OtherUserSelectorComponent(navToOtherUser = {
                            navToOtherUserDestination()
                        }, signStatus, isForSelf) { isSelf, otherUserSessionList ->
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    if (isSelf)
                                        runCatching {
                                            signStatus[0].loading()
                                            signer.signByClick()
                                        }.onFailure {
                                            if ((it is ChaoxingPredictableException).not()) {
                                                Sentry.captureException(it)
                                            }
                                            it.printStackTrace()
                                            signStatus[0].failed(it)
                                        }.onSuccess {
                                            UMengHelper.onSignClickEvent(
                                                context,
                                                ChaoxingHttpClient.instance!!.userEntity.name
                                            )
                                            signStatus[0].success()
                                        }
                                    otherUserSessionList.forEachIndexed { index, userSession ->
                                        runCatching {
                                            signStatus[1 + index].loading()
                                            ChaoxingHttpClient.loadFromOtherUserSession(userSession)
                                                .also { client ->
                                                    ChaoxingPhotoSigner(client, destination).apply {
                                                        if (preSign()) {
                                                            signStatus[index + 1].failed(
                                                                ChaoxingSigner.AlreadySignedException()
                                                            )
                                                        } else if (ifPhotoRequiredLogin()) {
                                                            signStatus[index + 1].failed(
                                                                ChaoxingPhotoSigner.ChaoxingIncorrectSignTypeException()
                                                            )
                                                        } else {
                                                            signByClick()
                                                        }
                                                    }
                                                }
                                        }.onFailure {
                                            if ((it is ChaoxingPredictableException).not()) {
                                                Sentry.captureException(it)
                                            }
                                            it.printStackTrace()
                                            signStatus[1 + index].failed(it)
                                        }.onSuccess {
                                            UMengHelper.onSignClickEvent(
                                                context,
                                                userSession.name,
                                                isOtherUser = true
                                            )
                                            signStatus[1 + index].success()
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (isImage == true) {
                    var isSignForOther by remember { mutableStateOf<Boolean?>(null) }
                    when (isSignForOther) {
                        null -> {
                            Icon(painterResource(R.drawable.ic_image_up), null)
                            Text("这是一个图片签到。")
                            Button(onClick = {
                                isSignForOther = false
                            }) { Text("为自己签到（从图库读取图片）") }
                            Button(onClick = {
                                isSignForOther = true
                            }) { Text("为他人代签（自己拍摄多张图片上传）") }
                        }

                        true -> {
                            val signStatus = remember { mutableListOf(ChaoxingSignStatus()) }
                            var isCamera by remember { mutableStateOf(false) }
                            var isSelfForSign by remember { mutableStateOf(false) }
                            val otherUserSessionForSignList =
                                remember { mutableListOf<ChaoxingOtherUserSession>() }

                            OtherUserSelectorComponent(
                                navToOtherUser = {
                                    navToOtherUserDestination()
                                }, signStatus, isForSelf
                            ) { isSelf, otherUserSessionList ->
                                isSelfForSign = isSelf
                                otherUserSessionForSignList.addAll(otherUserSessionList)
                                isCamera = true
                            }
                            AnimatedVisibility(
                                isCamera, enter =
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
                                BackHandler(isCamera) {
                                    isCamera = false
                                }
                                val combinedUserList = if (isSelfForSign) {
                                    listOf(
                                        ChaoxingHttpClient.instance!!.userEntity.name,
                                    ) + otherUserSessionForSignList.map { it.name }
                                } else {
                                    otherUserSessionForSignList.map { it.name }
                                }
                                var index by remember { mutableIntStateOf(0) }
                                CameraComponent(
                                    pictureCount = otherUserSessionForSignList.size + if (isSelfForSign) 1 else 0,
                                    onNextPhoto = {
                                        index++
                                    },
                                    content = {
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    Color(0x88888888),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .border(
                                                    BorderStroke(2.dp, Color(0xFF444444)),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Crossfade(index) {
                                                Text("拍摄给 ${combinedUserList[it]} 签到的图片")
                                            }
                                        }
                                    }) {
                                    coroutineScope.launch {
                                        if (isSelfForSign) {
                                            runCatching {
                                                signStatus[0].loading()
                                                signer.getCloudToken().let { token ->
                                                    signer.uploadImage(context, it[0], token)
                                                        .let { objectId ->
                                                            signer.signByImage(objectId)
                                                        }
                                                }
                                            }.onFailure {
                                                if ((it is ChaoxingPredictableException).not()) {
                                                    Sentry.captureException(it)
                                                }
                                                it.printStackTrace()
                                                signStatus[0].failed(it)
                                            }.onSuccess {
                                                UMengHelper.onSignPhotoEvent(
                                                    context,
                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                )
                                                signStatus[0].success()
                                            }
                                            otherUserSessionForSignList.apply {
                                                removeAt(0)
                                            }.forEachIndexed { index, chaoxingOtherUserSession ->
                                                runCatching {
                                                    signStatus[1 + index].loading()
                                                    ChaoxingHttpClient.loadFromOtherUserSession(
                                                        chaoxingOtherUserSession
                                                    ).also { client ->
                                                        ChaoxingPhotoSigner(
                                                            client,
                                                            destination
                                                        ).apply {
                                                            signByImage(
                                                                uploadImage(
                                                                    context, it[index + 1],
                                                                    getCloudToken()
                                                                )
                                                            )
                                                        }
                                                    }
                                                }.onFailure {
                                                    if ((it is ChaoxingPredictableException).not()) {
                                                        Sentry.captureException(it)
                                                    }
                                                    it.printStackTrace()
                                                    signStatus[1 + index].failed(it)
                                                }.onSuccess {
                                                    UMengHelper.onSignPhotoEvent(
                                                        context,
                                                        ChaoxingHttpClient.instance!!.userEntity.name
                                                    )
                                                    signStatus[1 + index].success()
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }

                        false -> {
                            val photoPermissionsState = rememberMultiplePermissionsState(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                                    listOf(
                                        android.Manifest.permission.READ_MEDIA_IMAGES,
                                        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                    ) else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU)
                                    listOf(
                                        android.Manifest.permission.READ_MEDIA_IMAGES
                                    ) else
                                    listOf(
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                    )
                            )
                            if (photoPermissionsState.allPermissionsGranted) {
                                if (isShowPhotoPicker)
                                    signer.GetPhotoFromMediaStore {
                                        coroutineScope.launch {
                                            runCatching {
                                                signer.getCloudToken().let { token ->
                                                    it?.let { image ->
                                                        signer.uploadImage(context, image, token)
                                                            .let { objectId ->
                                                                signer.signByImage(objectId)
                                                                UMengHelper.onSignPhotoEvent(
                                                                    context,
                                                                    ChaoxingHttpClient.instance!!.userEntity.name
                                                                )
                                                            }
                                                    }
                                                }
                                            }.onFailure {
                                                if ((it is ChaoxingPredictableException).not()) {
                                                    Sentry.captureException(it)
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "签到失败",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            }.onSuccess {
                                                isSignSuccess = true
                                            }
                                            isShowPhotoPicker = false
                                        }
                                    }
                                if (isSignSuccess) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(painterResource(R.drawable.ic_check_px80), "")
                                        Text("签到成功")
                                        Button(onClick = {
                                            navBack()
                                        }) { Text("返回") }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Button(onClick = {
                                            isShowPhotoPicker = true
                                        }) {
                                            Text("选择图片")
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("请授予应用读取图片权限")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { photoPermissionsState.launchMultiplePermissionRequest() },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("授予")
                                    }
                                }
                            }

                        }

                    }
                }
            }

            true -> {
                AlreadySignedNotice({
                    isAlreadySigned = true
                    isForSelf = true
                }, onDismiss = {
                    isAlreadySigned = true
                }) {
                    navBack()
                }
            }

            null -> {
                CenterCircularProgressIndicator()
            }
        }
    }
}