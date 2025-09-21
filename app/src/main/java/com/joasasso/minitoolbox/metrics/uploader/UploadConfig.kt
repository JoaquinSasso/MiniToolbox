package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import androidx.core.content.edit

object UploadConfig {
    private const val SP = "metrics_upload_cfg"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_APIKEY = "api_key"

    fun set(context: Context, endpoint: String, apiKey: String) {
        val sp = context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
        sp.edit { putString(KEY_ENDPOINT, endpoint).putString(KEY_APIKEY, apiKey) }
    }

    fun getEndpoint(context: Context): String =
        context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getString(KEY_ENDPOINT, "") ?: ""

    fun getApiKey(context: Context): String =
        context.applicationContext.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getString(KEY_APIKEY, "") ?: ""
}
