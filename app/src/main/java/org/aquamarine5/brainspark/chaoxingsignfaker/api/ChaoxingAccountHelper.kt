/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.requirePredictable
import java.io.File
import kotlin.time.Duration.Companion.days

object ChaoxingAccountHelper {
    fun getAvatarUrl(uid: Int): String {
        return "https://photo.chaoxing.com/p/${uid}_120?flag=1&psize=120_120c&ext=jpg&t=${System.currentTimeMillis()}"
    }

    private val AVATAR_CACHE_TIMEOUT = 1.days.inWholeMilliseconds

    suspend fun getCachedAvatar(context: Context, key: String, url: String): Any =
        withContext(Dispatchers.IO) {
            runCatching {
                val avatarFile = File(context.cacheDir.resolve("avatar_cache"), "$key.jpg")
                if (avatarFile.exists() &&
                    avatarFile.length() > 0 &&
                    System.currentTimeMillis() - avatarFile.lastModified() < AVATAR_CACHE_TIMEOUT
                ) {
                    return@withContext avatarFile
                }
                avatarFile.parentFile?.mkdirs()
                val client = ChaoxingHttpClient.instance?.okHttpClient ?: OkHttpClient()
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    requirePredictable(response.isSuccessful) { "Fetch avatar failed: ${response.code}" }
                    response.body.byteStream().use { input ->
                        avatarFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                avatarFile.setLastModified(System.currentTimeMillis())
                avatarFile
            }.getOrNull() ?: url
        }
}
