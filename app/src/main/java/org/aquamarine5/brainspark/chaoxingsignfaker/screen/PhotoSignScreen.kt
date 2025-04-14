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
import androidx.compose.foundation.background
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import io.sentry.Sentry
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.aquamarine5.brainspark.chaoxingsignfaker.ChaoxingPredictableException
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.components.AlreadySignedNotice
import org.aquamarine5.brainspark.chaoxingsignfaker.components.CenterCircularProgressIndicator
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingPhotoSigner

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
fun PhotoSignScreen(destination: PhotoSignDestination, navBack: () -> Unit) {
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
                                                        ChaoxingHttpClient.instance!!.userEntity
                                                    )
                                                }
                                        }
                                    }
                                }.onFailure {
                                    if ((it is ChaoxingPredictableException).not()) {
                                        Sentry.captureException(it)
                                    }
                                    Toast.makeText(context, "签到失败", Toast.LENGTH_SHORT)
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
                            if (isImage == true) {
                                Button(onClick = {
                                    isShowPhotoPicker = true
                                }) {
                                    Text("选择图片")
                                }
                            } else if (isImage == false) {
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
                                Button(onClick = {
                                    coroutineScope.launch {
                                        runCatching {
                                            signer.signByClick()
                                        }.onFailure {
                                            if ((it is ChaoxingPredictableException).not()) {
                                                Sentry.captureException(it)
                                            }
                                            Toast.makeText(context, "签到失败", Toast.LENGTH_SHORT)
                                                .show()
                                        }.onSuccess {
                                            isSignSuccess = true
                                            UMengHelper.onSignClickEvent(
                                                context,
                                                ChaoxingHttpClient.instance!!.userEntity
                                            )
                                        }
                                    }
                                }) {
                                    Text("点击签到")
                                }
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

            true -> {
                AlreadySignedNotice(null, onDismiss = {
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