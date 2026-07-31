/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StoredData<P, R>(private val block: suspend (P) -> R) {
    private var cache: R? = null

    private val mutex = Mutex()

    suspend fun getValue(param: P): R {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: block(param).also { cache = it }
        }
    }

    fun setValue(value: R) {
        cache = value
    }

    fun invalidate() {
        cache = null
    }
}

fun <P, R> storedData(block: suspend (P) -> R): StoredData<P, R> {
    return StoredData(block)
}