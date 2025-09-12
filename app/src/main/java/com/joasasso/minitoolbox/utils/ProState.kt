package com.joasasso.minitoolbox.utils

import androidx.compose.runtime.staticCompositionLocalOf

data class ProState(
    val isPro: Boolean,
    val source: ProSource = ProSource.None,
    val sinceMillis: Long? = null
)

enum class ProSource { None, Lifetime, Subscription, Promo }

// CompositionLocal global para leer el estado PRO en cualquier Composable
val LocalProState = staticCompositionLocalOf { ProState(isPro = false) }

