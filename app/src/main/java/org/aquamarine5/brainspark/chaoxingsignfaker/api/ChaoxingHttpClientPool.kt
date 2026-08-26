/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.utilities.chaoxingDataStore
import java.util.concurrent.ConcurrentHashMap

object ChaoxingHttpClientPool {
    private val clients = ConcurrentHashMap<String, ChaoxingHttpClient>()
    private val clientSessions = ConcurrentHashMap<String, ChaoxingOtherUserSession>()
    private val loadingMutexes = ConcurrentHashMap<String, Mutex>()
    private val sessionsMutex = Mutex()

    @Volatile
    private var cachedSessions: Map<String, ChaoxingOtherUserSession> = emptyMap()

    suspend fun initialize(otherUserSessions: List<ChaoxingOtherUserSession>) {
        sessionsMutex.withLock {
            val updatedSessions = otherUserSessions.associateBy { it.phoneNumber }
            clientSessions.forEach { (phoneNumber, clientSession) ->
                if (clientSession != updatedSessions[phoneNumber]) {
                    clients.remove(phoneNumber)
                    clientSessions.remove(phoneNumber, clientSession)
                }
            }
            cachedSessions = updatedSessions
        }
    }

    suspend fun put(client: ChaoxingHttpClient) {
        sessionsMutex.withLock {
            val phoneNumber = client.userEntity.phoneNumber
            clientSessions.remove(phoneNumber)
            clients[phoneNumber] = client
        }
    }

    suspend fun get(context: Context, phoneNumber: String): ChaoxingHttpClient {
        clients[phoneNumber]?.let { return it }
        val loadingMutex = loadingMutexes.getOrPut(phoneNumber) { Mutex() }
        return loadingMutex.withLock {
            clients[phoneNumber]?.let { return@withLock it }
            val session = cachedSessions[phoneNumber] ?: run {
                val sessions = context.chaoxingDataStore.data.first().otherUsersList
                initialize(sessions)
                cachedSessions[phoneNumber]
                    ?: throw IllegalStateException("未找到用户 $phoneNumber 的登录会话")
            }
            val client = ChaoxingHttpClient.loadFromOtherUserSession(session, context)
            sessionsMutex.withLock {
                clientSessions[phoneNumber] = session
                clients[phoneNumber] = client
            }
            client
        }
    }
}
