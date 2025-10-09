package com.joasasso.minitoolbox.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.joasasso.minitoolbox.R

@Composable
fun ProToolPaywallDialog(
    onDismiss: () -> Unit,
    onGoToPro: () -> Unit,
    onWatchAd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700)) },
        title = { Text(stringResource(R.string.paywall_title)) },
        text = { Text(stringResource(R.string.paywall_description)) },
        confirmButton = {
            Button(onClick = onGoToPro) {
                Text(stringResource(R.string.paywall_go_pro))
            }
        },
        dismissButton = {
            TextButton(onClick = onWatchAd) {
                Text(stringResource(R.string.paywall_watch_ad))
            }
        }
    )
}