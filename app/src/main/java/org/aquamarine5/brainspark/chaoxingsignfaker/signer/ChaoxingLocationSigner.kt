package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingPostLocationEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screens.GetLocationDestination

class ChaoxingLocationSigner(
    client: ChaoxingHttpClient,
    activityEntity: ChaoxingSignActivityEntity,
    private val signLocation: ChaoxingPostLocationEntity
) : ChaoxingSigner(client, activityEntity) {

    companion object {
        const val URL_SIGN =
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&validate=&vpProbability=-1&vpStrategy="

        const val CLASSTAG="ChaoxingLocationSigner"

        suspend fun getLocationSignInfo(activityEntity: ChaoxingSignActivityEntity):GetLocationDestination{
            ChaoxingSignHelper.getSignInfo(activityEntity).let { jsonResult ->
                return ChaoxingLocationDetailEntity(
                    jsonResult.getDouble("latitude"),
                    jsonResult.getDouble("longitude"),
                    jsonResult.getString("locationRange")
                )
            }
        }
    }

    override suspend fun sign() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", signLocation.latitude.toString())
                    .addQueryParameter("longitude", signLocation.longitude.toString())
                    .addQueryParameter("address", signLocation.address)
                    .addQueryParameter("activeId", activityEntity.id.toString())
                    .addQueryParameter("uid", client.userEntity.uid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", "")
                    .build()
            ).get().build()
        ).execute().use {
            val result=it.body?.string()
            if(result!="success"){
                Log.w(CLASSTAG,result?:"")
            }
        }
    }

    override suspend fun beforeSign() {
        preSign()
    }

    suspend fun getSignLocation(): ChaoxingLocationDetailEntity =
        getSignInfo().let { jsonResult ->
            return ChaoxingLocationDetailEntity(
                jsonResult.getDouble("latitude"),
                jsonResult.getDouble("longitude"),
                jsonResult.getString("locationRange")
            )
        }
}