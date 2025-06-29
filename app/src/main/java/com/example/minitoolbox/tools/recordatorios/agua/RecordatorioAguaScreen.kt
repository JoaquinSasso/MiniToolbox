package com.example.minitoolbox.tools.recordatorios.agua

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.minitoolbox.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorioAguaScreen(
    onBack: () -> Unit,
    onShowEstadisticas: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // -------- DataStore: cargar estados iniciales --------
    val aguaHoy by context.flujoAguaHoy().collectAsState(initial = 0)
    val objetivoDS by context.flujoObjetivo().collectAsState(initial = 2000)
    val porVasoDS by context.flujoPorVaso().collectAsState(initial = 250)

    var totalAgua by remember { mutableIntStateOf(aguaHoy) }
    var objetivoML by remember { mutableIntStateOf(objetivoDS) }
    var mlPorVaso by remember { mutableIntStateOf(porVasoDS) }
    var showInfo by remember { mutableStateOf(false) }
    var showDialogVaso by remember { mutableStateOf(false) }

    val notifDS by context.flujoNotificacionesActivas().collectAsState(initial = false)
    val freqMinDS by context.flujoFrecuenciaMinutos().collectAsState(initial = 180)



    // --- Cuando DataStore cambia, sincroniza la UI ---
    LaunchedEffect(aguaHoy) { totalAgua = aguaHoy }
    LaunchedEffect(objetivoDS) { objetivoML = objetivoDS }
    LaunchedEffect(porVasoDS) { mlPorVaso = porVasoDS }

    // -- Lógica agregar agua --
    fun agregarAgua(cantidad: Int) {
        totalAgua += cantidad
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            context.guardarAguaHoy(totalAgua)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("Agregaste ${cantidad}ml 💧")
        }
        if (notifDS) {
            programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
        }
    }

    // -- Reset de consumo --
    fun resetear() {
        totalAgua = 0
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            context.guardarAguaHoy(0)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar("¡Contador de agua reiniciado!")
        }
        if (notifDS) {
            programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
        }
    }

    // -- Cuando cambian objetivo o porVaso, guarda en DataStore --
    LaunchedEffect(objetivoML) {
        if (objetivoML != objetivoDS) {
            scope.launch { context.guardarObjetivo(objetivoML) }
            if (notifDS) {
                programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
            }
        }
    }
    LaunchedEffect(mlPorVaso) {
        if (mlPorVaso != porVasoDS) {
            scope.launch { context.guardarPorVaso(mlPorVaso) }
        }
    }

    LaunchedEffect(Unit) {
        createWaterReminderChannel(context)
    }

    LaunchedEffect(freqMinDS) {
        if (notifDS) {
            programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordatorio de Agua") },
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
                        programarRecordatorioAgua(context, 0, totalAgua, objetivoML) // lanza ya
                        scope.launch { snackbarHostState.showSnackbar("Prueba: notificación en breve") }
                    }) {
                        Icon(Icons.Filled.NotificationAdd, contentDescription = "Prueba notificación")
                    }

                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(14.dp))
            // --- Selector de objetivo ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Meta diaria:", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))

                // Slider con muchos pasos intermedios
                var lastSliderValue by remember { mutableFloatStateOf(objetivoML.toFloat()) }
                Slider(
                    value = objetivoML / 1000f,
                    onValueChange = { valor ->
                        // Solo permite valores múltiplos de 0.25L
                        val step = 0.25f
                        val newValue = (valor / step).roundToInt() * step
                        objetivoML = (newValue * 1000).roundToInt()
                        if (newValue != lastSliderValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastSliderValue = newValue
                        }
                    },
                    valueRange = 1f..4f,
                    steps = ((4f - 1f) / 0.25f).toInt() - 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text("${(objetivoML / 1000f).let { "%.2f".format(it) }}L")
            }
            // --- Botón de agregar agua ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        agregarAgua(mlPorVaso)
                    }
                ) {
                    Icon(Icons.Filled.LocalDrink, contentDescription = "Agregar agua")
                    Spacer(Modifier.width(6.dp))
                    Text("+$mlPorVaso ml")
                }
                Spacer(Modifier.width(16.dp))
                TextButton(
                    onClick = { showDialogVaso = true }
                ) { Text("Cambiar cantidad") }
            }

            // --- Reset y estadísticas ---
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        resetear()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Resetear")
                    Spacer(Modifier.width(6.dp))
                    Text("Resetear")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowEstadisticas()
                    }
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = "Estadísticas")
                    Spacer(Modifier.width(6.dp))
                    Text("Estadísticas")
                }
            }

            // --- Visual de progreso ---
            AguaLevelBar(totalAgua, objetivoML)
            Spacer(Modifier.height(6.dp))
            Text(
                "${(totalAgua / 1000f).let { "%.2f".format(it) }} L / ${(objetivoML / 1000f).let { "%.2f".format(it) }} L",
                fontSize = 20.sp
            )

            // --- Recordatorio ---
            HorizontalDivider(
                Modifier.padding(vertical = 10.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            Text("Recordatorio de beber agua", fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Notificaciones: $notifDS", fontSize = 16.sp)
                Switch(
                    checked = notifDS,
                    onCheckedChange = { checked ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            context.guardarNotificacionesActivas(checked)
                            if (checked) {
                                programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
                                snackbarHostState.showSnackbar("Notificaciones activadas")
                            } else {
                                cancelarRecordatorioAgua(context)
                                snackbarHostState.showSnackbar("Notificaciones desactivadas")
                            }
                        }
                    }
                )
            }
            if (notifDS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Frecuencia (horas)", fontSize = 16.sp)
                    Slider(
                        value = freqMinDS.toFloat(),
                        onValueChange = { minutos ->
                            scope.launch {
                                context.guardarFrecuenciaMinutos(minutos.toInt())
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        valueRange = 30f..180f,
                        steps = 5,  // 30, 60, 90, 120, 150, 180 → 6 posiciones = 5 pasos
                        modifier = Modifier.width(200.dp)
                    )
                    Text("$freqMinDS min")
                }
            }
        }
    }

    // --- Cambiar cantidad del vaso (diálogo simple) ---
    if (showDialogVaso) {
        var tempMl by remember { mutableStateOf(mlPorVaso.toString()) }
        AlertDialog(
            onDismissRequest = { showDialogVaso = false },
            title = { Text("Cantidad por vaso") },
            text = {
                OutlinedTextField(
                    value = tempMl,
                    onValueChange = { tempMl = it.filter { c -> c.isDigit() } },
                    label = { Text("ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    tempMl.toIntOrNull()?.let { newValue ->
                        mlPorVaso = newValue.coerceIn(50, 1500)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDialogVaso = false
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialogVaso = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text("Cancelar") }
            }
        )
    }

    // --- Información ---
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Sobre el recordatorio de agua") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Lleva el control de la cantidad de agua que consumes a diario.")
                    Text("• Puedes configurar tu meta diaria y la cantidad que sumas con cada botón.")
                    Text("• Si activas las notificaciones, la app te recordará tomar agua según el intervalo elegido.")
                    Text("• La notificación indica tu avance (por ejemplo, 1.3L/3L).")
                    Text("• Todo se almacena localmente y no se comparte fuera de tu dispositivo.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text("Cerrar") }
            }
        )
    }
}

// --- Barra de nivel visual ---
@Composable
fun AguaLevelBar(ml: Int, objetivo: Int) {
    val colorFondo = MaterialTheme.colorScheme.surfaceVariant
    val AguaAzul = Color(0xFF2196F3)
    val ColorGota = MaterialTheme.colorScheme.onSurfaceVariant
    val grosor = 38.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(grosor)
    ) {
        // Fondo gris claro
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = colorFondo,
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
        }
        // Barra azul proporcional al progreso
        val frac = (ml / objetivo.toFloat()).coerceIn(0f, 1f)
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = AguaAzul,
                size = androidx.compose.ui.geometry.Size(width = size.width * frac, height = size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
        }
        // Gota (opcional, decorativa)
        Icon(
            painter = painterResource(id = R.drawable.ic_water),
            contentDescription = "Gota decorativa",
            tint = ColorGota,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(24.dp)
        )

    }
}

// --- Lógica de alarmas (receiver y canal deberías tenerlos ya) ---
fun programarRecordatorioAgua(
    context: Context,
    frecuenciaMinutos: Int,
    consumidoML: Int,
    objetivoML: Int
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, WaterReminderReceiver::class.java).apply {
        putExtra("agua_consumida_ml", consumidoML)
        putExtra("agua_objetivo_ml", objetivoML)
        putExtra("frecuencia_minutos", frecuenciaMinutos)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context, 101,
        intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = System.currentTimeMillis() + frecuenciaMinutos * 60 * 1000L

    println("Programando recordatorio inexacto para $frecuenciaMinutos min")

    alarmManager.cancel(pendingIntent)  // Cancela cualquier alarma previa
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        frecuenciaMinutos * 60 * 1000L,
        pendingIntent
    )
}



fun cancelarRecordatorioAgua(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WaterReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 101,
        intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}