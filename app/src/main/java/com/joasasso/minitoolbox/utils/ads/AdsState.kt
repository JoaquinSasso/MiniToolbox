package com.joasasso.minitoolbox.utils.ads

data class AdsState(
    val isPro: Boolean,
    val canRequestAds: Boolean
) {
    val shouldShowAds: Boolean get() = !isPro && canRequestAds
}