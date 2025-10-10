package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import kotlinx.coroutines.delay

@Composable
fun ProToolPaywallDialog(
    onDismiss: () -> Unit,
    onGoToPro: () -> Unit,
    onWatchAd: () -> Unit
) {
    val context = LocalContext.current

    var hasPass by remember { mutableStateOf(CreditAccessManager.hasActivePass(context)) }
    var remainingMs by remember { mutableLongStateOf(CreditAccessManager.passRemainingMs(context)) }
    val graceLeft = remember { CreditAccessManager.graceLeftToday(context) }

    // Ticker para refrescar el tiempo mientras haya pase activo
    LaunchedEffect(hasPass) {
        if (!hasPass) return@LaunchedEffect
        while (true) {
            delay(1000)
            remainingMs = CreditAccessManager.passRemainingMs(context)
            hasPass = remainingMs > 0
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700)) },
        title = { Text(stringResource(R.string.paywall_title)) },
        text = {
            Column {
                Text(text = stringResource(R.string.paywall_description_time_only))

                Spacer(Modifier.height(8.dp))

                if (hasPass) {
                    Text(
                        text = stringResource(
                            R.string.paywall_pass_active,
                            CreditAccessManager.formatDurationMmSs(remainingMs)
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.paywall_pass_locked_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchAd,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp)
            ) {
                Text(stringResource(R.string.paywall_watch_ad_pass))
            }
        },
        dismissButton = {
            Button(
                onClick = onGoToPro,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700), // Dorado
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFC9A800) // Dorado más oscuro
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.paywall_go_pro),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
