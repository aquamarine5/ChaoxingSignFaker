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

object ChaoxingHttpRequesterPool {
    private val clients = ConcurrentHashMap<String, ChaoxingHttpRequester>()
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

    suspend fun put(client: ChaoxingHttpRequester) {
        sessionsMutex.withLock {
            val phoneNumber = client.phoneNumber
            clientSessions.remove(phoneNumber)
            clients[phoneNumber] = client
        }
    }

    suspend fun getRequester(context: Context, phoneNumber: String): ChaoxingHttpRequester {
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
            val client = ChaoxingHttpRequester.loadFromOtherSession(session, context)
            sessionsMutex.withLock {
                clientSessions[phoneNumber] = session
                clients[phoneNumber] = client
            }
            client
        }
    }

    suspend fun getClient(context: Context, phoneNumber: String): ChaoxingHttpClient {
        (clients[phoneNumber] as? ChaoxingHttpClient)?.let { return it }
        val requester = getRequester(context, phoneNumber)
        if (requester is ChaoxingHttpClient) return requester
        val loadingMutex = loadingMutexes.getOrPut(phoneNumber) { Mutex() }
        return loadingMutex.withLock {
            (clients[phoneNumber] as? ChaoxingHttpClient)?.let { return@withLock it }
            val source = clients[phoneNumber] ?: requester
            if (source is ChaoxingHttpClient) return@withLock source
            val client = source.toChaoxingHttpClient(context)
            sessionsMutex.withLock {
                clients[phoneNumber] = client
            }
            client
        }
    }
}

