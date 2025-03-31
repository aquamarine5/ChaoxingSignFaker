/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingOtherUserHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingOtherUserSharedEntity

class ImportOtherUserActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        runCatching {
            intent.data?.let {
                if (it.host == "import") {
                    this.lifecycle.coroutineScope.launch {
                        ChaoxingOtherUserHelper.saveOtherUser(
                            this@ImportOtherUserActivity, ChaoxingOtherUserSharedEntity(
                                it.getQueryParameter("phone")!!,
                                it.getQueryParameter("pwd")!!,
                                it.getQueryParameter("name")!!,
                            )
                        )
                    }.invokeOnCompletion {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }.onFailure {
            Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show()
        }.onSuccess {
            Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show()
        }
    }
}