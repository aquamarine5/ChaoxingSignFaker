package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

class ChaoxingQRCodeSigner(client: ChaoxingHttpClient,
                           activityEntity: ChaoxingSignActivityEntity
) : ChaoxingSigner(client, activityEntity) {
    override suspend fun sign() {
        TODO("Not yet implemented")
    }

    override suspend fun beforeSign() {
        TODO("Not yet implemented")
    }
}