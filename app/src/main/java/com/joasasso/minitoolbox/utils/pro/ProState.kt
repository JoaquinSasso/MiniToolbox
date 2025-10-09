package com.joasasso.minitoolbox.utils.pro

import androidx.compose.runtime.staticCompositionLocalOf

data class ProState(val isPro: Boolean)

enum class ProSource { None, Lifetime, Subscription, Promo }

// CompositionLocal global para leer el estado PRO en cualquier Composable
val LocalProState = staticCompositionLocalOf { ProState(isPro = false) }