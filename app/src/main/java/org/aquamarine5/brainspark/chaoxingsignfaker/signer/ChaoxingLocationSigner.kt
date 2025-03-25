/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationDetailEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingLocationSignEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.screen.GetLocationDestination

class ChaoxingLocationSigner(
    client: ChaoxingHttpClient,
    private val destination: GetLocationDestination
) : ChaoxingSigner(
    client,
    destination.activeId,
    destination.classId,
    destination.courseId,
    destination.extContent
) {

    companion object {
        const val URL_SIGN =
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax?&clientip=&appType=15&ifTiJiao=1&validate=&vpProbability=-1&vpStrategy="

        const val CLASSTAG = "ChaoxingLocationSigner"
    }

    class ChaoxingLocationSignException(message: String) : Exception(message)

    suspend fun getLocationSignInfo(): ChaoxingLocationDetailEntity {
        getSignInfo().let { jsonResult ->
            return ChaoxingLocationDetailEntity(
                jsonResult.getDouble("locationLatitude"),
                jsonResult.getDouble("locationLongitude"),
                jsonResult.getInteger("locationRange")
            )
        }
    }

    suspend fun sign(signLocation: ChaoxingLocationSignEntity) = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().url(
                URL_SIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("latitude", signLocation.latitude.toString())
                    .addQueryParameter("longitude", signLocation.longitude.toString())
                    .addQueryParameter("address", signLocation.address)
                    .addQueryParameter("activeId", destination.activeId.toString())
                    .addQueryParameter("uid", client.userEntity.uid.toString())
                    .addQueryParameter("name", client.userEntity.name)
                    .addQueryParameter("fid", client.userEntity.fid.toString())
                    .addQueryParameter("deviceCode", ChaoxingHttpClient.deviceCode!!)
                    .build()
            ).get().build()
        ).execute().use {
            val result = it.body?.string()
            if (result != "success") {
                Log.w(CLASSTAG, result ?: "")
                throw ChaoxingLocationSignException(result ?: "签到失败")
            }
        }
    }

    override suspend fun checkAlreadySign(response: Response): Boolean {
        return response.body?.string()?.contains("恭喜你已完成签到")?.not() ?: false
    }
}