package org.aquamarine5.brainspark.chaoxingsignfaker

import android.content.Context
import com.umeng.commonsdk.UMConfigure

object UMengHelper {
    private const val API_KEY = "67d42c1c48ac1b4f87e7edae"
    private const val API_CHANNEL = "WXPublish"
    fun preInit(context: Context) {
        UMConfigure.preInit(context, API_KEY, API_CHANNEL)
    }

    fun init(context: Context) {
        UMConfigure.init(context, API_KEY, API_CHANNEL, UMConfigure.DEVICE_TYPE_PHONE, "")

        
    }
}