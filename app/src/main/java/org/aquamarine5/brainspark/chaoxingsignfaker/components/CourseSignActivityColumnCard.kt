package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity


@Composable
fun CourseSignActivityColumnCard(
    activity: ChaoxingSignActivityEntity,
    onSignAction: (Any) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            coroutineScope.launch {
                val destination = ChaoxingActivityHelper.getSignDestination(activity)
                if (destination != null) {
                    onSignAction(destination)
                }
            }
        }) {
        Icon(painter = ChaoxingActivityHelper.getSignIcon(activity), contentDescription = null)
        Column {
            Text(activity.nameOne)
            Text("结束时间：${activity.nameFour}")
        }

    }
    Spacer(modifier = Modifier.height(16.dp))
}