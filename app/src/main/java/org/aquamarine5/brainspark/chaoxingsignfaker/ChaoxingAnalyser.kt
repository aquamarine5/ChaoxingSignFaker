/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import com.alibaba.fastjson2.JSONArray
import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Request
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingSignFakerDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingAnalyserRankAnalysis
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingAnalyserRankRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val SUPABASE_ENDPOINT =
    "https://zpkavhhjdtghljleztpb.supabase.co/rest/v1"
private const val SUPABASE_DATABASE_ID = "AnalyserData"
private const val SUPABASE_RANK_VIEW_ID = "user_sign_rank"
private const val SUPABASE_RANK_ANALYSIS_ID = "total_sign_view"
private const val SUPABASE_API_KEY = "sb_publishable_dFuI4bOoYPlDozMXOGKgPg_cCQ0o22B"

object ChaoxingAnalyser {
    lateinit var rankUUID: String

    suspend fun getTotalRankAnalysis(): Result<ChaoxingAnalyserRankAnalysis> {
        return withContext(Dispatchers.IO) {
            runCatching {
                ChaoxingHttpClient.instance!!.newCall(
                    Request.Builder()
                        .url("$SUPABASE_ENDPOINT/$SUPABASE_RANK_ANALYSIS_ID?select=userCount,totalRecordSignCount&limit=1")
                        .get()
                        .header(
                            "apikey",
                            SUPABASE_API_KEY
                        )
                        .build()
                ).execute().use { response ->
                    response.checkResponseThrowException()
                    val jsonObject = JSONArray.parseArray(response.body.string()).getJSONObject(0)
                    return@runCatching ChaoxingAnalyserRankAnalysis(
                        jsonObject.getInteger("userCount"),
                        jsonObject.getInteger("totalRecordSignCount")
                    )
                }
            }
        }
    }

    suspend fun getUserTopRank(uuid: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            runCatching {
                ChaoxingHttpClient.instance!!.newCall(
                    Request.Builder()
                        .url("$SUPABASE_ENDPOINT/$SUPABASE_RANK_VIEW_ID?select=rank&limit=1&uuid=eq.$uuid")
                        .get()
                        .header(
                            "apikey",
                            SUPABASE_API_KEY
                        )
                        .build()
                ).execute().use { response ->
                    response.checkResponseThrowException()
                    val responseBody = response.body.string()
                    return@runCatching JSONArray.parseArray(responseBody).getJSONObject(0)
                        .getInteger("rank")
                }
            }
        }
    }

    suspend fun getAnalyserTopRank(topCount: Int): Result<List<ChaoxingAnalyserRankRecord>> {
        return withContext(Dispatchers.IO) {
            runCatching {
                ChaoxingHttpClient.instance!!.newCall(
                    Request.Builder()
                        .url(
                            "$SUPABASE_ENDPOINT/$SUPABASE_DATABASE_ID?order=totalSignCount.desc&limit=${
                                topCount.coerceIn(
                                    1,
                                    100
                                )
                            }&isPublic=eq.TRUE"
                        )
                        .get()
                        .header(
                            "apikey",
                            SUPABASE_API_KEY
                        )
                        .build()
                ).execute().use { response ->
                    response.checkResponseThrowException()
                    val responseBody = response.body.string()
                    Json.decodeFromString<List<ChaoxingAnalyserRankRecord>>(responseBody)
                }
            }
        }
    }

    suspend fun checkAndUploadAnalyserRankData(context: Context) {
        withContext(Dispatchers.IO) {
            val currentDate =
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()
            val stringDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            context.chaoxingDataStore.updateData {
                if (it.disableAnalysisRank) return@updateData it
                val analysisName = it.analysisRankName.ifEmpty {
                    "****${ChaoxingHttpClient.instance!!.userEntity.phoneNumber.takeLast(2)} 用户"
                }
                it.toBuilder().apply {
                    val analysisDatabaseUUID = analysisUUID.ifEmpty {
                        rankUUID.also {
                            setAnalysisUUID(it)
                        }
                    }
                    if (lastUploadAnalysisDate < currentDate) {
                        runCatching {
                            ChaoxingHttpClient.instance!!.newCall(
                                Request.Builder()
                                    .url("$SUPABASE_ENDPOINT/$SUPABASE_DATABASE_ID")
                                    .header(
                                        "apikey",
                                        SUPABASE_API_KEY
                                    )
                                    .header("Prefer", "resolution=merge-duplicates")
                                    .post(
                                        FormBody.Builder()
                                            .addEncoded(
                                                "otherSign",
                                                mutableAnalyser.otherUserSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "photoSign",
                                                mutableAnalyser.photoSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "gestureSign",
                                                mutableAnalyser.gestureSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "locationSign",
                                                mutableAnalyser.locationSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "qrcodeSign",
                                                mutableAnalyser.qrcodeSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "clickSign",
                                                mutableAnalyser.clickSignCount.value.toString()
                                            )
                                            .addEncoded(
                                                "passwordSign",
                                                mutableAnalyser.passwordSignCount.value.toString()
                                            )
                                            .addEncoded("latestDate", stringDate)
                                            .addEncoded(
                                                "schoolName",
                                                ChaoxingHttpClient.instance!!.userEntity.schoolName.let { str ->
                                                    if (it.hideAnalysisRankSchoolName) str.plus("HIDE") else str
                                                }
                                            )
                                            .addEncoded("uuid", analysisDatabaseUUID)
                                            .addEncoded(
                                                "totalSignCount",
                                                (mutableAnalyser.photoSignCount.value + mutableAnalyser.gestureSignCount.value + mutableAnalyser.locationSignCount.value + mutableAnalyser.qrcodeSignCount.value + mutableAnalyser.clickSignCount.value + mutableAnalyser.passwordSignCount.value).toString()
                                            )
                                            .addEncoded("isPublic", "true")
                                            .addEncoded("name", analysisName)
                                            .build()
                                    ).build()
                            )
                                .execute().use { response ->
                                    response.checkResponseThrowException()
                                }
                        }.onSuccess {
                            setLastUploadAnalysisDate(currentDate)
                            setLastUploadAnalysisTimestamp(System.currentTimeMillis())
                        }.onFailure {
                            it.printStackTrace()
                            Sentry.captureException(it)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "上传排行榜数据失败: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }.build()
            }
        }
    }


    data class MutableStateAnalyser(
        val photoSignCount: MutableState<Int> = mutableIntStateOf(0),
        val gestureSignCount: MutableState<Int> = mutableIntStateOf(0),
        val locationSignCount: MutableState<Int> = mutableIntStateOf(0),
        val qrcodeSignCount: MutableState<Int> = mutableIntStateOf(0),
        val clickSignCount: MutableState<Int> = mutableIntStateOf(0),
        val otherUserSignCount: MutableState<Int> = mutableIntStateOf(0),
        val passwordSignCount: MutableState<Int> = mutableIntStateOf(0),
        val isLoaded: MutableState<Boolean> = mutableStateOf(false)
    ) {
        companion object {
            val Saver = listSaver(
                save = { analyser ->
                    listOf(
                        analyser.photoSignCount.value,
                        analyser.gestureSignCount.value,
                        analyser.locationSignCount.value,
                        analyser.qrcodeSignCount.value,
                        analyser.clickSignCount.value,
                        analyser.otherUserSignCount.value,
                        if (analyser.isLoaded.value) 1 else 0,
                        analyser.passwordSignCount.value
                    )
                },
                restore = { values ->
                    MutableStateAnalyser(
                        photoSignCount = mutableIntStateOf(values[0]),
                        gestureSignCount = mutableIntStateOf(values[1]),
                        locationSignCount = mutableIntStateOf(values[2]),
                        qrcodeSignCount = mutableIntStateOf(values[3]),
                        clickSignCount = mutableIntStateOf(values[4]),
                        otherUserSignCount = mutableIntStateOf(values[5]),
                        isLoaded = mutableStateOf(values[6] == 1),
                        passwordSignCount = mutableIntStateOf(values[7])
                    )
                }
            )
        }
    }

    private val mutableAnalyser = MutableStateAnalyser()
    suspend fun onPhotoSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.photoSignCount.value++
                setAnalysis(analysis.toBuilder().setPhotoSign(analysis.photoSign + 1))
            }.build()
        }
    }

    suspend fun onLocationSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.locationSignCount.value++
                setAnalysis(analysis.toBuilder().setLocationSign(analysis.locationSign + 1))
            }.build()
        }
    }

    suspend fun onQRCodeSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.qrcodeSignCount.value++
                setAnalysis(analysis.toBuilder().setQrcodeSign(analysis.qrcodeSign + 1))
            }.build()
        }
    }

    suspend fun onPasswordSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.passwordSignCount.value++
                setAnalysis(analysis.toBuilder().setPasswordSign(analysis.passwordSign + 1))
            }.build()
        }
    }

    suspend fun onGestureSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.gestureSignCount.value++
                setAnalysis(analysis.toBuilder().setGestureSign(analysis.gestureSign + 1))
            }.build()
        }
    }

    suspend fun onClickSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.clickSignCount.value++
                setAnalysis(analysis.toBuilder().setClickSign(analysis.clickSign + 1))
            }.build()
        }
    }

    suspend fun onOtherUserSignEvent(context: Context) {
        context.chaoxingDataStore.updateData {
            it.toBuilder().apply {
                mutableAnalyser.otherUserSignCount.value++
                setAnalysis(analysis.toBuilder().setOtherUserSign(analysis.otherUserSign + 1))
            }.build()
        }
    }

    fun createStateAnalyser(): MutableStateAnalyser {
        return mutableAnalyser
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun setupStateAnalyser(context: Context): MutableStateAnalyser {
        context.chaoxingDataStore.data.first().apply {
            mutableAnalyser.passwordSignCount.value = analysis.passwordSign
            mutableAnalyser.photoSignCount.value = analysis.photoSign
            mutableAnalyser.gestureSignCount.value = analysis.gestureSign
            mutableAnalyser.locationSignCount.value = analysis.locationSign
            mutableAnalyser.qrcodeSignCount.value = analysis.qrcodeSign
            mutableAnalyser.clickSignCount.value = analysis.clickSign
            mutableAnalyser.otherUserSignCount.value = analysis.otherUserSign
            mutableAnalyser.isLoaded.value = true
            rankUUID = analysisUUID.ifEmpty {
                Uuid.generateV7().toString()
            }
        }
        return mutableAnalyser
    }

    @OptIn(ExperimentalUuidApi::class)
    fun setupStateAnalyser(datastore: ChaoxingSignFakerDataStore): MutableStateAnalyser {
        datastore.apply {
            mutableAnalyser.passwordSignCount.value = analysis.passwordSign
            mutableAnalyser.photoSignCount.value = analysis.photoSign
            mutableAnalyser.gestureSignCount.value = analysis.gestureSign
            mutableAnalyser.locationSignCount.value = analysis.locationSign
            mutableAnalyser.qrcodeSignCount.value = analysis.qrcodeSign
            mutableAnalyser.clickSignCount.value = analysis.clickSign
            mutableAnalyser.otherUserSignCount.value = analysis.otherUserSign
            mutableAnalyser.isLoaded.value = true
            rankUUID = analysisUUID.ifEmpty {
                Uuid.generateV7().toString()
            }
        }
        return mutableAnalyser
    }
}
