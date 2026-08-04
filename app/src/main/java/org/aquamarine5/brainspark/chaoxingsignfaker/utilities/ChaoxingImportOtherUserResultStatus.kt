/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import org.aquamarine5.brainspark.chaoxingsignfaker.datastore.ChaoxingOtherUserSession

typealias ImportOtherUserResult = Triple<ChaoxingImportOtherUserResultStatus,String, ChaoxingOtherUserSession>

enum class ChaoxingImportOtherUserResultStatus {
    SUCCESS,
    EXISTED_BUT_UPDATE_PASSWORD,
    EXISTED_BUT_UPDATE_FACE_IMAGES
}

fun ImportOtherUserResult.getResultTips(): String=
    when(this.first){
        ChaoxingImportOtherUserResultStatus.SUCCESS->"$second 用户成功导入"
        ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_PASSWORD -> "已更新 $second 密码"
        ChaoxingImportOtherUserResultStatus.EXISTED_BUT_UPDATE_FACE_IMAGES -> "已添加 $second 的人脸照片信息"
    }