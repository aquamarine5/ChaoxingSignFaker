package org.aquamarine5.brainspark.chaoxingsignfaker.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Composable
fun CourseInfoColumnCard(course: ChaoxingCourseEntity,imageLoader: ImageLoader, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                course.imageUrl.replace("http://","https://"),
                imageLoader = imageLoader,
                contentScale = ContentScale.FillHeight,
                contentDescription = null,
                modifier = Modifier
                    .height(55.dp)
                    .width(55.dp)
                    .clip(
                        RoundedCornerShape(3.dp)
                    ),
                onError = {
                    Log.w("CourseInfoColumnCard", "Error loading image: ${it.result}")
                }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(course.courseName.replace("\n",""), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(course.teacherName.replace("\n",""))
                if(!course.schools.isNullOrBlank()){
                    Text(course.schools.replace("\n",""),  fontSize = 12.sp)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}