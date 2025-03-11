package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignLocationEntity

class ChaoxingLocationSigner(
    client: ChaoxingHttpClient,
    activityEntity: ChaoxingSignActivityEntity,
    private val signLocation: ChaoxingSignLocationEntity
) : ChaoxingSigner(client, activityEntity) {

    companion object {
        const val URL_SIGN =
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&validate=&vpProbability=-1&vpStrategy="
    }

    override suspend fun sign() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", signLocation.latitude.toString())
                    .addQueryParameter("longitude", signLocation.longitude.toString())
                    .addQueryParameter("address", signLocation.address)
                    .addQueryParameter("activeId",activityEntity.id.toString())
                    .addQueryParameter("uid",client.userEntity.uid.toString())
                    .addQueryParameter("name",client.userEntity.name)
                    .addQueryParameter("fid",client.userEntity.fid.toString())

                    .build()
            ).get().build()
        ).execute().use {

        }
    }

    override suspend fun beforeSign() {
        TODO("Not yet implemented")
    }
}