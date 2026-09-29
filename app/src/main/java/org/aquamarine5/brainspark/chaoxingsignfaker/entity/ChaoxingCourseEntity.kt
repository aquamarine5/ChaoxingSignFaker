/*
 * Copyright (c) 2025-2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.entity

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
@Stable
data class ChaoxingCourseEntity(
    val courseName: String,
    val teacherName: String?,
    val courseId: Long,
    val classId: Int,
    val className: String,
    val imageUrl: String,
    val schools: String?,
    @Transient
    val isPreferred: MutableState<Boolean> = mutableStateOf(false),
    val isCloneSession: Boolean = false
) {
    companion object {
        object ChaoxingCourseEntityListNavType : NavType<List<ChaoxingCourseEntity>>(false) {
            private val listSerializer = ListSerializer(serializer())
            override fun get(bundle: Bundle, key: String): List<ChaoxingCourseEntity>? {
                return Json.decodeFromString(listSerializer, bundle.getString(key) ?: return null)
            }

            override fun parseValue(value: String): List<ChaoxingCourseEntity> {
                return Json.decodeFromString(listSerializer, Uri.decode(value))
            }

            override fun serializeAsValue(value: List<ChaoxingCourseEntity>): String {
                return Uri.encode(Json.encodeToString(listSerializer, value))
            }

            override fun put(bundle: Bundle, key: String, value: List<ChaoxingCourseEntity>) {
                bundle.putString(key, Json.encodeToString(listSerializer, value))
            }
        }

        val Saver: Saver<SnapshotStateList<ChaoxingCourseEntity>, *> = listSaver(
            save = { saver ->
                saver.map {
                    listOf(
                        it.courseName,
                        it.teacherName,
                        it.courseId,
                        it.classId,
                        it.className,
                        it.imageUrl,
                        it.schools,
                        it.isPreferred.value,
                        it.isCloneSession
                    )
                }
            },
            restore = { restorer ->
                SnapshotStateList<ChaoxingCourseEntity>().apply {
                    addAll(
                        restorer.map {
                            ChaoxingCourseEntity(
                                it[0] as String,
                                it[1] as String?,
                                it[2] as Long,
                                it[3] as Int,
                                it[4] as String,
                                it[5] as String,
                                it[6] as String?,
                                mutableStateOf(it[7] as Boolean),
                                it[8] as Boolean
                            )
                        }
                    )
                }
            }
        )
    }
}