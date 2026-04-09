/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore

object ChaoxingFaceHelper {
    suspend fun saveFaceImage(
        client: ChaoxingHttpClient,
        context: Context,
        bitmap: Bitmap,
        phoneNumber: String? = null
    ) =
        withContext(Dispatchers.IO) {
            ChaoxingCloudDriveHelper.uploadImage(
                client,
                bitmap
            ).let { objectId ->
                context.chaoxingDataStore.updateData {
                    it.toBuilder().apply {
                        if (phoneNumber == null) {
                            setLoginSession(
                                loginSession.toBuilder().setFaceImageObjectId(objectId).build()
                            )
                        } else {
                            val index = otherUsersList.indexOfFirst { user -> user.phoneNumber == phoneNumber }
                            if (index != -1) {
                                setOtherUsers(
                                    index,
                                    getOtherUsers(index).toBuilder().setFaceImageObjectId(objectId).build()
                                )
                            }
                        }
                    }.build()
                }
            }
        }
}