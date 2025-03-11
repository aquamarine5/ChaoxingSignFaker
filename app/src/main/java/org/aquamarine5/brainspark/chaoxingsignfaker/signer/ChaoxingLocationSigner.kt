package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

class ChaoxingLocationSigner(client: ChaoxingHttpClient,
                             activityEntity: ChaoxingSignActivityEntity
) : ChaoxingSigner(client, activityEntity) {

    companion object{
        const val URL_SIGN="https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&validate=&vpProbability=-1&vpStrategy="
    }
    override fun sign() {
        client.newCall(Request.Builder().url(
            URL_SIGN.toHttpUrl().newBuilder().build()
        ))
    }

    override fun beforeSign() {
        TODO("Not yet implemented")
    }
}