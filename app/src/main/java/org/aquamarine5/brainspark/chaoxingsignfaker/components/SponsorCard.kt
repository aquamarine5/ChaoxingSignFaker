package org.aquamarine5.brainspark.chaoxingsignfaker.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aquamarine5.brainspark.chaoxingsignfaker.R

@Composable
fun SponsorCard() {
    var isShowDialog by remember { mutableStateOf(false) }
    Button(
        onClick = {
            isShowDialog = true
        },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xffea7293))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_coffee), contentDescription = "sponsor")
            Spacer(modifier = Modifier.width(8.dp))
            Text(buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily(
                            Font(R.font.gilroy)
                        ),
                        fontSize = 14.sp
                    )
                ) {
                    append("ChaoxingSignFaker")
                }
                withStyle(
                    SpanStyle(fontSize = 14.sp)
                ){

                    append("帮到你了嘛？\n")
                    append("那就给作者赞赏一杯奶茶吧。或者给两杯奶茶，怎么样？")
                }
            })
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (isShowDialog) {
        AlertDialog(onDismissRequest = {
            isShowDialog = false
        }, confirmButton = {
            Button(onClick = {
                isShowDialog = false
            }) {
                Text("OK")
            }
        }, text = {
            Image(painterResource(R.drawable.image_sponsor), contentDescription = "sponsor")
        })
    }
}