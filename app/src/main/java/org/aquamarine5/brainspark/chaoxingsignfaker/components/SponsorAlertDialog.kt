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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.aquamarine5.brainspark.chaoxingsignfaker.R
import org.aquamarine5.brainspark.chaoxingsignfaker.UMengHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import java.io.File


private const val SPONSOR_IMAGE_FILENAME_BASE = "ChaoxingSignFaker_sponsor"

@SuppressLint("WrongConstant")
@Composable
fun SponsorAlertDialog(showDialog: MutableState<Boolean>) {
    val context = LocalContext.current
    val sponsorList = listOf(
        listOf("催什么崔", "8.88"),
        listOf("不愿透露姓名的耿先生", "8.88"),
        listOf("不愿透露姓名的景先生", "6.66"),
        listOf("*.", "6.00"),
        listOf("死后世界战线", "5.88"),
        listOf("不愿透露姓名的张先生", "2.88"),
    )
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
                    val imageValues = ContentValues().apply {
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        val date = System.currentTimeMillis() / 1000
                        put(MediaStore.Images.Media.DATE_ADDED, date)
                        put(MediaStore.Images.Media.DATE_MODIFIED, date)
                    }
                    val filename =
                        "${SPONSOR_IMAGE_FILENAME_BASE}_${System.currentTimeMillis()}.png"

                    var file: File? = null
                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        if (!dir.exists() && !dir.mkdirs()) {
                            return@Button
                        }
                        file = File(dir, filename)
                        imageValues.apply {
                            put(MediaStore.Images.Media.DATA, file.absolutePath)
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        }
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val resultUri = context.contentResolver.insert(collection, imageValues)
                    if (resultUri == null) {
                        Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                    } else {
                        context.contentResolver.openOutputStream(resultUri)?.use { outputStream ->
                            ContextCompat.getDrawable(context, R.drawable.image_sponsor)!!
                                .toBitmap().compress(
                                android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream
                            )
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            file?.let {
                                imageValues.put(MediaStore.Images.Media.SIZE, it.length())
                            }
                            context.contentResolver.update(resultUri, imageValues, null, null)
                            context.sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    resultUri
                                )
                            )
                        } else {
                            imageValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            context.contentResolver.update(resultUri, imageValues, null, null)
                        }
                    }
                    Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
                    context.packageManager.getLaunchIntentForPackage("com.tencent.mm")?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                    UMengHelper.onGotoSponsorWechatEvent(context,ChaoxingHttpClient.instance!!.userEntity)
                }) {
                    Text("保存图片并转到微信")
                }
            }
        }, text = {
            Column {
                Image(painterResource(R.drawable.image_sponsor), contentDescription = "sponsor")
                Spacer(modifier = Modifier.height(4.dp))
                Text("捐赠列表：", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(buildAnnotatedString {
                    sponsorList.forEach {
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
                        append("\n")
                    }
                })
            }
        })
    }
}