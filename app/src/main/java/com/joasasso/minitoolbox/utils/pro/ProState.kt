package com.joasasso.minitoolbox.utils.pro

import androidx.compose.runtime.staticCompositionLocalOf

data class ProState(val isPro: Boolean)

// CompositionLocal global para leer el estado PRO en cualquier Composable
val LocalProState = staticCompositionLocalOf { ProState(isPro = true) } //TODO Cambiar el true a false cuando se publique la app