/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.api

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aquamarine5.brainspark.chaoxingsignfaker.chaoxingDataStore
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.RecommendHabit
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.RecommendRecord
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.RecommendRecordList
import java.time.LocalDateTime
import kotlin.math.abs

object ChaoxingRecommendHelper {
    const val MINIMUM_HABIT_LEARN_INTERVAL = 30
    const val MINIMUM_RECORD_ANALYSE_INTERVAL = 5
    const val MINIMUM_HABIT_MERGE_INTERVAL = 20
    const val FIND_RECOMMEND_HABIT_INTERVAL = 15

    suspend fun getAllLearnedHabits(context: Context): List<RecommendHabit> =
        withContext(Dispatchers.IO) {
            context.chaoxingDataStore.data.first().recommendHabitsList
        }

    suspend fun getRecommendedCourses(context: Context): List<Pair<Int, Int>> {
        val allHabits = getAllLearnedHabits(context)
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value
        val currentMinuteOfDay = now.hour * 60 + now.minute

        return allHabits.filter { habit ->
            habit.dayOfWeek == currentDayOfWeek &&
                    abs(habit.minuteOfDay - currentMinuteOfDay) <= FIND_RECOMMEND_HABIT_INTERVAL
        }.map { it.classId to it.courseId }.distinct()
    }

    suspend fun checkRecommendedActivities(
        context: Context,
        snackbarHostState: SnackbarHostState
    ): List<Pair<Long, Any>> {
        buildList {
            getRecommendedCourses(context).forEach { ids ->
                ChaoxingActivityHelper.checkCourseHaveAvailableActivity(
                    ChaoxingHttpClient.instance!!,
                    context,
                    ids.first,
                    ids.second,
                    snackbarHostState
                )?.let { add(it) }
            }
            return this
        }
    }

    suspend fun analyseRecommendHabit(
        context: Context,
        classId: Int,
        courseId: Int,
        recommendRecords: Map<Int, RecommendRecordList>,
        recommendHabits: List<RecommendHabit>,
        currentRecommendRecord: RecommendRecord
    ) = withContext(Dispatchers.IO) {
        val allRelatedHabits = recommendHabits.filter { it.classId == classId }
        recommendRecords[classId]?.recordsList?.forEach { record ->
            if (record.dayOfWeek == currentRecommendRecord.dayOfWeek &&
                abs(record.minuteOfDay - currentRecommendRecord.minuteOfDay) <= MINIMUM_RECORD_ANALYSE_INTERVAL
            ) {
                val newHabitMinuteOfDay =
                    (record.minuteOfDay + currentRecommendRecord.minuteOfDay) / 2
                val closeHabit = allRelatedHabits.find { habit ->
                    habit.dayOfWeek == currentRecommendRecord.dayOfWeek &&
                            abs(habit.minuteOfDay - newHabitMinuteOfDay) <= MINIMUM_HABIT_MERGE_INTERVAL
                }
                if (closeHabit != null) {
                    val totalRecordCount = closeHabit.recordCount + 1
                    val weightedMinuteOfDay =
                        (closeHabit.minuteOfDay * closeHabit.recordCount + newHabitMinuteOfDay * 2) / totalRecordCount
                    val updatedHabit = closeHabit.toBuilder()
                        .setMinuteOfDay(weightedMinuteOfDay)
                        .setRecordCount(totalRecordCount)
                        .build()
                    context.chaoxingDataStore.updateData { datastore ->
                        val habitIndex = datastore.recommendHabitsList.indexOf(closeHabit)
                        if (habitIndex != -1) {
                            datastore.toBuilder().setRecommendHabits(habitIndex, updatedHabit)
                                .build()
                        } else {
                            datastore
                        }
                    }
                } else {
                    val isTooCloseToExistingHabits = allRelatedHabits.any { habit ->
                        habit.dayOfWeek == currentRecommendRecord.dayOfWeek &&
                                abs(habit.minuteOfDay - newHabitMinuteOfDay) <= MINIMUM_HABIT_LEARN_INTERVAL
                    }

                    if (!isTooCloseToExistingHabits) {
                        val newHabit = RecommendHabit.newBuilder()
                            .setClassId(classId)
                            .setCourseId(courseId)
                            .setRecordCount(2)
                            .setDayOfWeek(currentRecommendRecord.dayOfWeek)
                            .setMinuteOfDay(newHabitMinuteOfDay)
                            .build()
                        context.chaoxingDataStore.updateData { datastore ->
                            datastore.toBuilder().addRecommendHabits(newHabit).build()
                        }
                    }
                }
            }
        }

    }

    suspend fun recordRecommendEvent(context: Context, classId: Int, courseId: Int) =
        withContext(Dispatchers.IO) {
            val time = LocalDateTime.now()
            context.chaoxingDataStore.updateData { datastore ->
                datastore.toBuilder().apply {
                    val newRecord = RecommendRecord.newBuilder()
                        .setDayOfWeek(time.dayOfWeek.value)
                        .setMinuteOfDay(time.hour * 60 + time.minute)
                        .setCourseId(courseId)
                        .build()
                    coroutineScope {
                        launch {
                            analyseRecommendHabit(
                                context,
                                classId, courseId,
                                recommendRecordsMap,
                                recommendHabitsList,
                                newRecord
                            )
                        }
                    }
                    val recordListBuilder =
                        if (containsRecommendRecords(classId)) getRecommendRecordsOrThrow(
                            classId
                        ).toBuilder() else RecommendRecordList.newBuilder()
                    putRecommendRecords(classId, recordListBuilder.addRecords(newRecord).build())
                }.build()
            }
        }
}