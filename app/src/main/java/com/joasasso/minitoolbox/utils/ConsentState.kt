package com.joasasso.minitoolbox.utils

import androidx.compose.runtime.staticCompositionLocalOf

data class ConsentState(
    val canRequestAds: Boolean,
    val privacyOptionsRequired: Boolean = false
)

val LocalConsentState = staticCompositionLocalOf { ConsentState(canRequestAds = false) }