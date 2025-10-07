package com.joasasso.minitoolbox.utils.ads

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

// Centralizamos la creación de AdRequest
object AdsManager {
    private val _ready = mutableStateOf(false)
    val isReady: State<Boolean> get() = _ready
    fun initialize(ctx: Context) {
        if (_ready.value) return
        MobileAds.initialize(ctx) { _ready.value = true }
    }
    fun buildRequest(): AdRequest = AdRequest.Builder().build()
}