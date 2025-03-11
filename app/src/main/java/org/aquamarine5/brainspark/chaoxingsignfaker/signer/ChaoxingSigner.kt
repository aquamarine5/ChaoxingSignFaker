package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity

abstract class ChaoxingSigner(
    private val client: ChaoxingHttpClient,
    val activityEntity: ChaoxingSignActivityEntity
) {
    companion object{
        const val URL_PERSIGN="https://mobilelearn.chaoxing.com/newsign/preSign?&general=1&sys=1&ls=1&appType=15&isTeacherViewOpen=0"
    }
    abstract fun sign()
    abstract fun beforeSign()
    fun preSign(){
        client.newCall(Request.Builder().get().url(
            URL_PERSIGN.toHttpUrl().newBuilder()
                .addQueryParameter("courseId",activityEntity.course.courseId.toString())
                .addQueryParameter("classId",activityEntity.course.classId.toString())
                .addQueryParameter("activePrimaryId",activityEntity.id.toString())
                .addQueryParameter("uid",client.userEntity.uid.toString())
                .build()
        ).build()).execute().use{

        }

    }
}