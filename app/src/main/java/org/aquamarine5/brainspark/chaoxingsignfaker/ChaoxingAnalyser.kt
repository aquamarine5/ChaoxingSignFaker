/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.flow.first

object ChaoxingAnalyser {
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

    suspend fun setupStateAnalyser(context: Context): MutableStateAnalyser {
        context.chaoxingDataStore.data.first().apply {
            mutableAnalyser.photoSignCount.value = analysis.photoSign
            mutableAnalyser.gestureSignCount.value = analysis.gestureSign
            mutableAnalyser.locationSignCount.value = analysis.locationSign
            mutableAnalyser.qrcodeSignCount.value = analysis.qrcodeSign
            mutableAnalyser.clickSignCount.value = analysis.clickSign
            mutableAnalyser.otherUserSignCount.value = analysis.otherUserSign
            mutableAnalyser.isLoaded.value = true
        }
        return mutableAnalyser
    }
}
