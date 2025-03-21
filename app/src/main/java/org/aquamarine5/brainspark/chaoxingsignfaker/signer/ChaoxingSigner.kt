package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient

abstract class ChaoxingSigner(
    val activeId:Long,
    val classId:Int,
    val courseId:Int,
    val extContent:String
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

    val client = ChaoxingHttpClient.instance!!

    abstract suspend fun sign()
    abstract suspend fun checkAlreadySign(response: Response):Boolean
    open suspend fun getSignInfo(): JSONObject = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_SIGN_INFO.toHttpUrl().newBuilder()
                    .addQueryParameter("activeId", activeId.toString())
                    .build()
            ).build()
        ).execute().use {
            return@withContext JSONObject.parseObject(it.body?.string())
        }
    }

    open suspend fun preSign():Boolean = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().post(
                FormBody.Builder().addEncoded("ext",extContent).build()
            ).url(
                URL_PERSIGN.toHttpUrl().newBuilder()
                    .addQueryParameter("courseId", courseId.toString())
                    .addQueryParameter("classId", classId.toString())
                    .addQueryParameter("activePrimaryId", activeId.toString())
                    .addQueryParameter("uid", client.userEntity.uid.toString())
                    .build()
            ).build()
        ).execute().use {
            postAnalysis()
            return@withContext checkAlreadySign(it)
        }
    }

    open suspend fun postAnalysis() = withContext(Dispatchers.IO) {
        client.newCall(
            Request.Builder().get().url(
                URL_ANALYSIS.toHttpUrl().newBuilder()
                    .addQueryParameter("aid",activeId.toString()).build()
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
        ).execute().close()
    }
}