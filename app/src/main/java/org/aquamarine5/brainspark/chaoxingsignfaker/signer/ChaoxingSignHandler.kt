/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.signer

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.hapticfeedback.HapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingHttpClient
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.api.SignDestination
import org.aquamarine5.brainspark.chaoxingsignfaker.checkIsLast
import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignStatus
import org.aquamarine5.brainspark.chaoxingsignfaker.ifShouldDeselect
import org.aquamarine5.brainspark.chaoxingsignfaker.snackbarReport

class ChaoxingSignHandler<in T>(
    private val onSelfSigning: suspend (value: T) -> Result<Boolean>,
    private val onOtherUserSigning: suspend (value: T, session: ChaoxingOtherUserSession, bypassChecking: Boolean, index: Int) -> Result<Boolean>,
    private val destination: SignDestination,
    private val onSigningFinished: suspend (value: T, name: String, isOtherUser: Boolean) -> Unit,
    private val onAllSigningFinished: suspend (isSuccessful: Boolean) -> Unit,
) {
    fun startSigning(
        value: T,
        isSelf: Boolean,
        otherUserSessionList: List<ChaoxingOtherUserSession?>,
        userSelections: SnapshotStateList<Boolean>,
        signStatus: MutableList<ChaoxingSignStatus>,
        hapticFeedback: HapticFeedback,
        coroutineScope: CoroutineScope,
        snackbarHost: SnackbarHostState
    ) {
        var isCaptchaSigning = false
        coroutineScope.launch {
            if (isSelf) {
                signStatus[0].loading()
                onSelfSigning(value).onSuccess {
                    isCaptchaSigning = it
                    userSelections[0] = false
                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                        signStatus[0].successForLate()
                    else
                        signStatus[0].success()
                    if (otherUserSessionList.all { it == null }) {
                        onAllSigningFinished(true)
                    }
                    onSigningFinished(value, ChaoxingHttpClient.instance!!.userEntity.name, false)
                }.onFailure {
                    signStatus[0].failed(it)
                    it.ifShouldDeselect {
                        userSelections[0] = false
                    }
                    if (otherUserSessionList.all { it == null }) {
                        onAllSigningFinished(userSelections.all { !it })
                    }
                    it.snackbarReport(
                        snackbarHost,
                        coroutineScope,
                        "为${ChaoxingHttpClient.instance!!.userEntity.name}签到失败",
                        hapticFeedback
                    )
                }
            }
            var isFirstOtherUserForSign = true
            otherUserSessionList.forEachIndexed { index, session ->
                if (session == null) return@forEachIndexed
                signStatus[index + 1].loading()
                if (!isCaptchaSigning || (isSelf && isFirstOtherUserForSign))
                    delay(ChaoxingOtherUserHelper.TIMEOUT_NEXT_SIGN)
                isFirstOtherUserForSign = false
                onOtherUserSigning(value, session, false, index).onSuccess {
                    isCaptchaSigning = it
                    if (destination.endTime != null && System.currentTimeMillis() > destination.endTime!!)
                        signStatus[1 + index].successForLate()
                    else
                        signStatus[1 + index].success()
                    userSelections[index + 1] = false
                    if (otherUserSessionList.checkIsLast(
                            index + 1
                        )
                    ) {
                        onAllSigningFinished(true)
                    }
                    onSigningFinished(value, session.name, true)
                }.onFailure {
                    it.snackbarReport(
                        snackbarHost,
                        coroutineScope,
                        "为${session.name}签到失败",
                        hapticFeedback
                    )
                    it.ifShouldDeselect {
                        userSelections[index + 1] = false
                    }
                    if (otherUserSessionList.checkIsLast(index + 1)) {
                        onAllSigningFinished(userSelections.all { !it })
                    }
                    signStatus[index + 1].failed(it)
                }
            }
        }
    }
}