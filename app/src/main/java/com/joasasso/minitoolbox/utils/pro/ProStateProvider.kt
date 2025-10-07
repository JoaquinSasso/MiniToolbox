package com.joasasso.minitoolbox.utils.pro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Inyecta globalmente ProState en la composición.
 * Requiere ProViewModel con proState: StateFlow<ProState>.
 */
@Composable
fun ProStateProvider(
    content: @Composable () -> Unit
) {
    val vm: ProViewModel = viewModel()
    val pro by vm.proState.collectAsState()

    CompositionLocalProvider(LocalProState provides pro) {
        content()
    }
}

