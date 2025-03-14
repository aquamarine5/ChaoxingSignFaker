package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import com.umeng.commonsdk.UMConfigure

object UMengHelper {
    private const val API_KEY="59892f08310c9307b60023d0"
    private const val API_CHANNEL="WXPublish"
    fun preInit(context: Context){
        UMConfigure.preInit(context, API_KEY, API_CHANNEL)
    }
    fun init(context: Context){
        UMConfigure.init(context, API_KEY, API_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")
    }
}