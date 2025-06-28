package com.example.minitoolbox.tools.recordatorios.agua

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaEstadisticasScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    var historico by remember { mutableStateOf<List<Pair<LocalDate, Int>>>(emptyList()) }
    var promedioSemana by remember { mutableIntStateOf(0) }
    var objetivoML by remember { mutableIntStateOf(2000) }

    // Cargar datos de DataStore al entrar a la screen
    LaunchedEffect(Unit) {
        // Leer toda la DataStore
        val prefs = context.aguaDataStore.data.first()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // Filtra y parsea solo claves de consumo de agua
        val lista = prefs.asMap()
            .filter { (k, _) -> k.name.startsWith("agua_ml_") }
            .mapNotNull { (k, v) ->
                val fechaStr = k.name.removePrefix("agua_ml_")
                try {
                    val fecha = LocalDate.parse(fechaStr, formatter)
                    (v as? Int)?.let { fecha to it }
                } catch (e: Exception) { null }
            }
            .sortedBy { it.first }
        historico = lista

        // Promedio últimos 7 días (pueden no ser consecutivos)
        val ultimos7 = lista.takeLast(7)
        promedioSemana = if (ultimos7.isNotEmpty()) ultimos7.map { it.second }.average().roundToInt() else 0

        // Objetivo actual
        objetivoML = context.flujoObjetivo().first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas de Agua") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Consumo últimos 7 días", fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            // --- Barra semanal ---
            BarChartAguaSemana(
                historico.takeLast(7),
                objetivoML
            )
            Spacer(Modifier.height(16.dp))
            // --- Datos ---
            if (historico.isEmpty()) {
                Text("Sin registros guardados", color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    "Promedio semanal: ${(promedioSemana/1000f).let { "%.2f".format(it) }}L/día",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mejor día: " +
                            historico.maxByOrNull { it.second }?.let {
                                "${it.first.dayOfMonth}/${it.first.monthValue} - ${(it.second/1000f).let { f-> "%.2f".format(f)}} L"
                            }.orEmpty(),
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(Modifier.height(18.dp))
            Text("Historial completo", fontSize = 17.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            if (historico.isEmpty()) {
                Text("Aún no has registrado consumo de agua.")
            } else {
                historico.sortedByDescending { it.first }.forEach { (fecha, ml) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ml >= objetivoML) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year}",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(ml / 1000f).let { "%.2f".format(it) }}L",
                                fontSize = 18.sp,
                                color = if (ml >= objetivoML) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Sobre las estadísticas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Visualiza tu consumo diario de agua de la última semana y el promedio.")
                    Text("• El gráfico muestra si alcanzaste tu objetivo cada día.")
                    Text("• Solo se almacena en tu dispositivo, no en la nube.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// --- Gráfico semanal básico ---
@Composable
fun BarChartAguaSemana(
    datos: List<Pair<LocalDate, Int>>,
    objetivoML: Int
) {
    if (datos.isEmpty()) {
        Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
            Text("Sin datos")
        }
        return
    }
    val maxY = objetivoML.coerceAtLeast(datos.maxOf { it.second })
    val barWidth = 38.dp
    val colors = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((fecha, ml) in datos) {
            val frac = (ml / maxY.toFloat()).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(110.dp)
                    .width(barWidth)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(
                        Modifier
                            .height(95.dp)
                            .width(barWidth)
                    ) {
                        val barHeight = size.height * frac
                        drawRoundRect(
                            color = if (ml >= objetivoML) colors.primary else colors.error,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                    }
                }
                Text(
                    text = "${fecha.dayOfMonth}/${fecha.monthValue}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
