/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import java.io.InputStream
import java.io.OutputStream

object DataStoreSerializer : Serializer<ChaoxingSignFakerDataStore> {
    override val defaultValue: ChaoxingSignFakerDataStore
        get() = ChaoxingSignFakerDataStore.newBuilder()
            .setAgreeTerms(false)
            .build()

    override suspend fun readFrom(input: InputStream): ChaoxingSignFakerDataStore = try {
        ChaoxingSignFakerDataStore.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", exception)
    }

    override suspend fun writeTo(t: ChaoxingSignFakerDataStore, output: OutputStream) =
        t.writeTo(output)
}

val Context.chaoxingDataStore: DataStore<ChaoxingSignFakerDataStore> by dataStore(
    fileName = "datastore.pb",
    serializer = DataStoreSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { DataStoreSerializer.defaultValue }
)
