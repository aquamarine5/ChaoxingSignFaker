/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.PhotoSignDestination

typealias ChaoxingPhotoActivityEntity = PhotoSignDestination

class ChaoxingPhotoSigner(
    client: ChaoxingHttpClient,
    photoActivityEntity: ChaoxingPhotoActivityEntity
) : ChaoxingSigner(
    client,
    photoActivityEntity.activeId,
    photoActivityEntity.classId,
    photoActivityEntity.courseId,
    photoActivityEntity.extContent
) {
    override suspend fun checkAlreadySign(response: Response): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun ifPhotoRequiredLogin(): Boolean = getSignInfo().getInteger("ifphoto") == 1

    @Composable
    fun GetPhotoFromMediaStore(onResult: (Uri) -> Unit) {
        val gallery = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri ->
                uri?.let {
                    onResult(it)
                }
            }
        )
        LaunchedEffect(Unit) {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}