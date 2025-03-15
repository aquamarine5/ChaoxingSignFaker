package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity


@Composable
fun CourseSignActivityColumnCard(activity: ChaoxingSignActivityEntity) {
    Row {
        Icon(painter = ChaoxingActivityHelper.getSignIcon(activity), contentDescription = null)
        Column {
            Text(activity.nameOne)
            Text("结束时间：${activity.nameFour}")
        }

    }
    Spacer(modifier = Modifier.height(16.dp))
}