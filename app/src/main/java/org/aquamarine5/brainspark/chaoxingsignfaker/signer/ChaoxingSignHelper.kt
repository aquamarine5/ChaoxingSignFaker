package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity
import org.aquamarine5.brainspark.chaoxingsignfaker.signer.ChaoxingSigner.Companion.URL_SIGN_INFO

object ChaoxingSignHelper {
    suspend fun getSignInfo(activityEntity:ChaoxingSignActivityEntity): JSONObject = withContext(Dispatchers.IO) {
        ChaoxingHttpClient.instance!!.newCall(
            Request.Builder().get().url(
                URL_SIGN_INFO.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activityEntity.id.toString())
                    .build()
            ).build()
        ).execute().use {
            return@withContext JSONObject.parseObject(it.body?.string()).getJSONObject("data")
        }
    }
}