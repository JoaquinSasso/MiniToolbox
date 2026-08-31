package com.joasasso.minitoolbox.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.BuildConfig
import com.joasasso.minitoolbox.ui.components.TopBarReusable

/**
 * Diagnóstico del pipeline de métricas.
 *
 * A diferencia de [MetricsDevScreen], está pensada para estar disponible en release: es de
 * sólo lectura y sus acciones no pueden destruir datos.
 *
 * Usa Scaffold como el resto de las pantallas, que es lo que aplica el padding de la barra
 * de estado y del recorte de cámara. Las herramientas de desarrollo se muestran como vista
 * alternativa y nunca anidadas, porque [MetricsDevScreen] trae su propio scroll vertical.
 */
@Composable
fun MetricsDiagnosticsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    var health by remember { mutableStateOf<MetricsHealth?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var showDevTools by remember { mutableStateOf(false) }
    var showRawPayload by remember { mutableStateOf(false) }

    LaunchedEffect(reloadTick) {
        health = loadMetricsHealth(ctx)
    }

    if (showDevTools && BuildConfig.DEBUG) {
        Scaffold(
            topBar = {
                TopBarReusable(
                    title = "Herramientas de desarrollo",
                    onBack = {
                        showDevTools = false
                        reloadTick++
                    }
                )
            }
        ) { innerPadding ->
            // MetricsDevScreen tiene su propio verticalScroll: anidar dos scrolls
            // verticales hace fallar la medición con altura infinita.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MetricsDevScreen()
            }
        }
        return
    }

    Scaffold(
        topBar = { TopBarReusable(title = "Diagnóstico de métricas", onBack = onBack) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val current = health
            if (current == null) {
                Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (current.payload.looksValid) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        summarizeHealth(current),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Estado", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Text(
                        text = buildDiagnosticsText(current),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        triggerFlushNow(ctx)
                        Toast.makeText(ctx, "Envío encolado", Toast.LENGTH_SHORT).show()
                        reloadTick++
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Forzar envío")
                }

                OutlinedButton(
                    onClick = { reloadTick++ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Actualizar")
                }
            }

            Button(
                onClick = {
                    copyToClipboard(ctx, buildDiagnosticsText(current))
                    Toast.makeText(ctx, "Diagnóstico copiado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copiar diagnóstico")
            }

            Text(
                "El informe contiene sólo contadores y códigos de estado, sin datos personales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---------------------------------------------------------
            // Payload crudo: fuera del informe por defecto, porque incluye los
            // contadores de uso de la persona. Se comparte sólo por decisión explícita.
            // ---------------------------------------------------------
            if (current.payload.present) {
                HorizontalDivider()

                Text("Lote pendiente", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Contiene tus contadores de uso por día (qué herramientas abriste y " +
                            "cuántas veces). No incluye contenido de las herramientas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showRawPayload = !showRawPayload },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showRawPayload) "Ocultar contenido" else "Ver contenido")
                }

                if (showRawPayload) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        // Altura acotada y scroll horizontal propio: el JSON puede ser
                        // largo y no debe empujar el resto de la pantalla.
                        Box(
                            Modifier
                                .heightIn(max = 320.dp)
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = prettyPayload(current.pendingPayloadRaw),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            copyToClipboard(ctx, current.pendingPayloadRaw)
                            Toast.makeText(ctx, "Lote copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copiar lote completo")
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = { showDevTools = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Herramientas de desarrollo")
                }
            }

            OutlinedButton(
                onClick = {
                    DevUnlock.setUnlocked(ctx, false)
                    Toast.makeText(ctx, "Diagnóstico bloqueado de nuevo", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ocultar esta pantalla")
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("MiniToolbox diagnóstico", text))
}