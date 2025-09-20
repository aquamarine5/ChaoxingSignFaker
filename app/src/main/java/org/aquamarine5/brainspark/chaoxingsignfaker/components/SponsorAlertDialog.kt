/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.alibaba.fastjson2.JSONObject
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.LocalSnackbarHostState
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.displaySnackbar
import java.io.File


private const val SPONSOR_IMAGE_FILENAME_BASE = "ChaoxingSignFaker_sponsor"

@OptIn(ExperimentalPermissionsApi::class)
@SuppressLint("WrongConstant")
@Composable
fun SponsorAlertDialog(showDialog: MutableState<Boolean>) {
    val context = LocalActivity.current!!.applicationContext
    val sponsorList = remember { mutableStateListOf<List<String>>() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            ChaoxingHttpClient.instance?.okHttpClient?.newCall(
                Request.Builder()
                    .get()
                    .url("http://cdn.aquamarine5.fun/chaoxingsignfaker_sponsor.json")
                    .build()
            )?.execute().use {
                val json = JSONObject.parseObject(it?.body?.string())
                val list = json.getJSONArray("sponsorList")
                if (list.isNotEmpty()) {
                    sponsorList.clear()
                    for (i in 0 until list.size) {
                        val item = list.getJSONArray(i)
                        if (item.size == 2) {
                            sponsorList.add(listOf(item.getString(0), item.getString(1)))
                        }
                    }
                }
            }
        }
    }
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val permissionCheck =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) rememberPermissionState(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) else null
    var isShowDialog by showDialog
    if (isShowDialog) {
        AlertDialog(onDismissRequest = {
            isShowDialog = false
        }, confirmButton = {
            Row {
                OutlinedButton(onClick = {
                    isShowDialog = false
                }) {
                    Text("下次一定")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(onClick = {
                    if (permissionCheck?.status?.isGranted == false) {
                        permissionCheck.launchPermissionRequest()
                    } else
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val imageValues = ContentValues().apply {
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                    val date = System.currentTimeMillis() / 1000
                                    put(MediaStore.Images.Media.DATE_ADDED, date)
                                    put(MediaStore.Images.Media.DATE_MODIFIED, date)
                                }
                                val filename =
                                    "${SPONSOR_IMAGE_FILENAME_BASE}_${System.currentTimeMillis()}.png"

                                var file: File? = null
                                val collection =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        imageValues.apply {
                                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                            put(
                                                MediaStore.Images.Media.RELATIVE_PATH,
                                                Environment.DIRECTORY_PICTURES
                                            )
                                            put(MediaStore.Images.Media.IS_PENDING, 1)
                                        }
                                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                                    } else {
                                        val dir =
                                            Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_PICTURES
                                            )
                                        if (!dir.exists() && !dir.mkdirs()) {
                                            return@withContext
                                        }
                                        file = File(dir, filename)
                                        imageValues.apply {
                                            put(MediaStore.Images.Media.DATA, file.absolutePath)
                                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                        }
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                    }
                                val resultUri =
                                    context.contentResolver.insert(collection, imageValues)
                                if (resultUri == null) {
                                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                                } else {
                                    context.contentResolver.openOutputStream(resultUri)
                                        ?.use { outputStream ->
                                            ContextCompat.getDrawable(
                                                context,
                                                R.drawable.img_sponsor
                                            )!!
                                                .toBitmap().compress(
                                                    android.graphics.Bitmap.CompressFormat.PNG,
                                                    100,
                                                    outputStream
                                                )
                                        }
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        file?.let {
                                            imageValues.put(
                                                MediaStore.Images.Media.SIZE,
                                                it.length()
                                            )
                                        }
                                        context.contentResolver.update(
                                            resultUri,
                                            imageValues,
                                            null,
                                            null
                                        )
                                        context.sendBroadcast(
                                            Intent(
                                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                                resultUri
                                            )
                                        )
                                    } else {
                                        imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                        context.contentResolver.update(
                                            resultUri,
                                            imageValues,
                                            null,
                                            null
                                        )
                                    }
                                }
                            }
                            snackbarHost.displaySnackbar("图片已保存到相册", coroutineScope)
                        }.invokeOnCompletion {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "weixin://dl/scan".toUri()
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            UMengHelper.onGotoSponsorWechatEvent(
                                context,
                                ChaoxingHttpClient.instance!!.userEntity
                            )
                        }
                }) {
                    Text("现在就去")
                }
            }
        }, text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Image(painterResource(R.drawable.img_sponsor), contentDescription = "sponsor")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "捐赠列表：",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "如需在列表内显示完整名称，请添加备注，微信支付不会显示完整名称。捐赠列表并非实时更新。",
                    fontStyle = FontStyle.Italic,
                    lineHeight = 14.sp,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildAnnotatedString {
                        sponsorList.forEachIndexed { index, it ->
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(it[0])
                            }
                            append(" 赞赏了 ")
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold, fontFamily = FontFamily(
                                        Font(R.font.gilroy)
                                    )
                                )
                            ) {
                                append(it[1])
                            }
                            append(" 元")
                            if (index != sponsorList.size - 1)
                                append("\n")
                        }
                    }, modifier = Modifier
                        .border(
                            1.dp, MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        })
    }
}