package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

abstract class ChaoxingSigner(
    private val client: ChaoxingHttpClient,
    val activityEntity: ChaoxingSignActivityEntity
) {
    abstract fun sign()
    abstract fun preSign()
}