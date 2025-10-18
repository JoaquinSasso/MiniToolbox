// ProStateProvider.kt
package com.joasasso.minitoolbox.utils.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProStateProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isPro by ProRepository.isProFlow(context).collectAsState(initial = true) //TODO Cambiar el initial a false cuando se publique la app
    CompositionLocalProvider(LocalProState provides ProState(isPro = isPro)) {
        content()
    }
}
