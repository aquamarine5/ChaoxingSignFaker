package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.aquamarine5.brainspark.chaoxingsignfaker.entity.ChaoxingCourseEntity

@Composable
fun CourseInfoColumnCard(course: ChaoxingCourseEntity, onClick:()->Unit){
    Button(onClick = onClick,shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("课程名称：${course.courseName}")
            Text("教师名称：${course.teacherName}")
            Text("courseId：${course.courseId}")
            Text("classId:${course.classId}")
        }
    }
    Spacer(modifier= Modifier.height(8.dp))
}