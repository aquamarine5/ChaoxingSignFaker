package org.aquamarine5.brainspark.chaoxingsignfaker.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ChaoxingHttpClient private constructor(val okHttpClient: OkHttpClient) {

    fun newCall(request: Request): Call = okHttpClient.newCall(request)

    companion object {
        private const val TRANSFER_KEY = "u2oh6Vu^HWe4_AES"
        private const val URL_LOGIN = "https://passport2.chaoxing.com/fanyalogin"
        suspend fun create(phoneNumber: Int, password: String): ChaoxingHttpClient {
            val cookieJar: CookieJar = object : CookieJar {
                private val cookieStore: MutableMap<String, List<Cookie>> = mutableMapOf()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: listOf()
                }
            }
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build()
            return client.apply {
                login(this, phoneNumber, password)
            }.let { ChaoxingHttpClient(it) }
        }

        private suspend fun login(client: OkHttpClient, phoneNumber: Int, password: String): OkHttpClient =
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
                    // Handle response if needed
                }
                return@withContext client
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