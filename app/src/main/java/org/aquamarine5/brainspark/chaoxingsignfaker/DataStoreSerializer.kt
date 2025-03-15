package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import android.os.Bundle
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.navigation.NavType
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingPostLocationEntity
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
