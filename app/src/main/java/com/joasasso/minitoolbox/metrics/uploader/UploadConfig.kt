package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import com.joasasso.minitoolbox.BuildConfig

object UploadConfig {
    fun getEndpoint(context: Context): String = BuildConfig.METRICS_ENDPOINT
    fun getApiKey(context: Context): String = BuildConfig.METRICS_API_KEY
}
