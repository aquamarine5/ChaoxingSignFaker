/*
 * Copyright (c) 2025, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun OtherUserScreen() {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(8.dp)) {

            Button(onClick={

            }) {
                Icon(painterResource(R.drawable.ic_user_round_plus), contentDescription = "Add User")
            }

        }
    }
}