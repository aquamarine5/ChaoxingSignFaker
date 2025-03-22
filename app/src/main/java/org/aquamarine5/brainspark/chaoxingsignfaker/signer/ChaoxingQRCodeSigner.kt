/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingQRCodeActivityEntity

class ChaoxingQRCodeSigner(
    client: ChaoxingHttpClient,
    qrCodeActivityEntity: ChaoxingQRCodeActivityEntity
) : ChaoxingSigner(
    client,
    qrCodeActivityEntity.activeId,
    qrCodeActivityEntity.classId,
    qrCodeActivityEntity.courseId,
    qrCodeActivityEntity.extContent
) {
    suspend fun sign() {
        TODO("Not yet implemented")
    }

    override suspend fun checkAlreadySign(response: Response): Boolean {
        TODO("Not yet implemented")
    }
}