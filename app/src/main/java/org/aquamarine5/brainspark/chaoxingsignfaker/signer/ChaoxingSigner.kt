package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

abstract class ChaoxingSigner(
    val client: ChaoxingHttpClient,
    val activityEntity: ChaoxingSignActivityEntity
) {
    companion object {
        const val URL_PERSIGN =
            "https://mobilelearn.chaoxing.com/newsign/preSign?&general=1&sys=1&ls=1&appType=15&isTeacherViewOpen=0"
        const val URL_SIGN_INFO =
            "https://mobilelearn.chaoxing.com/v2/apis/active/getPPTActiveInfo"
        const val URL_ANALYSIS =
            "https://mobilelearn.chaoxing.com/pptSign/analysis?vs=1&DB_STRATEGY=RANDOM"
        const val URL_AFTER_ANALYSIS2 =
            "https://mobilelearn.chaoxing.com/pptSign/analysis2?DB_STRATEGY=RANDOM"
    }

    abstract suspend fun sign()
    abstract suspend fun beforeSign()
    open suspend fun getSignInfo(): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_SIGN_INFO.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activityEntity.id.toString())
                    .build()
            ).build()
        ).execute().use {
            return@withContext JSONObject.parseObject(it.body?.string())
        }
    }

    open suspend fun preSign() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().post(
                FormBody.Builder().addEncoded("ext",activityEntity.ext.toString()).build()
            ).url(
                URL_PERSIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("courseId", activityEntity.course.courseId.toString())
                    .addQueryParameter("classId", activityEntity.course.classId.toString())
                    .addQueryParameter("activePrimaryId", activityEntity.id.toString())
                    .addQueryParameter("uid", client.userEntity.uid.toString())
                    .build()
            ).build()
        ).execute().use {

        }
        postAnalysis()
    }

    open suspend fun postAnalysis() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_ANALYSIS.toHttpUrl().newBuilder()
                    .addQueryParameter("aid", activityEntity.id.toString()).build()
            ).build()
        ).execute().use {
            postAfterAnalysis(
                """code='\+'([a-f0-9]+)'""".toRegex()
                    .find(it.body?.string() ?: throw Exception("xx"))?.groupValues?.get(1)
                    ?: throw Exception("Cannot find code")
            )
        }
    }

    open suspend fun postAfterAnalysis(code: String) = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_AFTER_ANALYSIS2.toHttpUrl().newBuilder()
                    .addQueryParameter("code", code)
                    .build()
            ).build()
        ).execute().use {

        }
    }
}