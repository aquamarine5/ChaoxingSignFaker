package org.aquamarine5.brainspark.chaoxingsignfaker.api

import com.alibaba.fastjson2.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingUserEntity
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaoxingHttpClient private constructor(
    val okHttpClient: OkHttpClient,
    val userEntity: ChaoxingUserEntity
) {

    fun newCall(request: Request): Call = okHttpClient.newCall(request)

    companion object {
        private const val CHAOXING_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 15; V2359A Build/AP3A.240905.015.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/134.0.6998.39 Mobile Safari/537.36 (schild:d68cdcee51c93693feca61f7141a4cd8) (device:V2359A) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_3_6.4.8_android_phone_10834_264 (@Kalimdor)_fd20364147f64d6ab387a682425f7495"
        private const val TRANSFER_KEY = "u2oh6Vu^HWe4_AES"
        private const val URL_USER_INFO = "https://sso.chaoxing.com/apis/login/userLogin4Uname.do"
        private const val URL_LOGIN = "https://passport2.chaoxing.com/fanyalogin"

        suspend fun create(phoneNumber: Int, password: String): ChaoxingHttpClient {
            val cookieJar: CookieJar = object : CookieJar {
                val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: listOf()
                }
            }
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", CHAOXING_USER_AGENT).build()
                    )
                }
                .build()
            return ChaoxingHttpClient(
                client, getInfo(client)
            )
        }

        private suspend fun getInfo(client: OkHttpClient): ChaoxingUserEntity =
            withContext(Dispatchers.IO) {
                client.newCall(Request.Builder().get().url(URL_USER_INFO).build()).execute()
                    .use { response ->
                        val jsonResult =
                            JSONObject.parseObject(response.body?.string()).getJSONObject("msg")
                        return@withContext ChaoxingUserEntity(
                            jsonResult.getInteger("uid"),
                            jsonResult.getInteger("fid"),
                            jsonResult.getString("name"),
                            jsonResult.getString("schoolname"),
                            jsonResult.getString("uname")
                        )
                    }
            }

        private suspend fun login(
            client: OkHttpClient,
            phoneNumber: Int,
            password: String
        ) =
            withContext(Dispatchers.IO) {
                val uname = encryptByAES(phoneNumber.toString())
                val encryptedPassword = encryptByAES(password)
                val request = Request.Builder()
                    .url(URL_LOGIN)
                    .post(FormBody.Builder().apply {
                        addEncoded("fid", "-1")
                        addEncoded("uname", uname)
                        addEncoded("password", encryptedPassword)
                        addEncoded("refer", "https%3A%2F%2Fi.chaoxing.com")
                        addEncoded("t", "true")
                        addEncoded("forbidotherlogin", "0")
                        addEncoded("validate", "")
                        addEncoded("doubleFactorLogin", "0")
                        addEncoded("independentId", "0")
                        addEncoded("independentNameId", "0")
                    }.build())
                    .build()
                client.newCall(request).execute().use {
                }
            }

        private fun encryptByAES(message: String, key: String = TRANSFER_KEY): String {
            val iv = IvParameterSpec(key.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val encrypted = cipher.doFinal(message.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encrypted)
        }
    }
}