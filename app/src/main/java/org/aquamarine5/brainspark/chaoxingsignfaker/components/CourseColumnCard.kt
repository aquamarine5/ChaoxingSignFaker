package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.aquamarine5.brainspark.chaoxingsignfaker.api.ChaoxingActivityHelper
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingSignActivityEntity



@Composable
fun CourseColumnCard(activity:ChaoxingSignActivityEntity){
    Row{
       Icon(painter = ChaoxingActivityHelper.getSignIcon(activity), contentDescription = null)
        Column{
            Text(activity.nameOne)
            Text("结束时间：${activity.nameFour}")
        }
    }
}