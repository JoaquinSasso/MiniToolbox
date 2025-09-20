import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MetricsConsentAfterAdsGate() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { AggregatesRepository(context.applicationContext) }

    var show by remember { mutableStateOf(false) }
    var decided by remember { mutableStateOf(false) }
    var eea by remember { mutableStateOf(false) }

    // Al montar, chequeamos si debemos pedir
    LaunchedEffect(Unit) {
        // ojo: en este punto el ConsentGate ya corrió.
        eea = isEeaOrUk(context)
        decided = repo.hasDecidedMetricsConsent()
        // Si estamos en EEA y todavía no tenemos decisión (o seteado a false por seed), mostramos
        show = eea && !decided
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { /* forzamos una acción explícita */ },
            title = { Text("Anonymous Metrics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Help improve MiniToolbox by sending anonymous usage metrics (no personal data, no device IDs, no precise location).")
                    Text("You can change this anytime in Privacy options.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    scope.launch(Dispatchers.IO) {
                        repo.setMetricsConsent(true)
                        repo.setMetricsConsentDecided()
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    show = false
                    scope.launch(Dispatchers.IO) {
                        repo.setMetricsConsent(false)
                        repo.setMetricsConsentDecided()
                    }
                }) { Text("Don’t allow") }
            }
        )
    }
}
