package com.example.minitoolbox.tools.recordatorios.agua

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.minitoolbox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
    val notifDS by context.flujoNotificacionesActivas().collectAsState(initial = false)
    val freqMinDS by context.flujoFrecuenciaMinutos().collectAsState(initial = 180)

    var totalAgua by remember { mutableIntStateOf(aguaHoy) }
    var objetivoML by remember { mutableIntStateOf(objetivoDS) }
    var mlPorVaso by remember { mutableIntStateOf(porVasoDS) }
    var showInfo by remember { mutableStateOf(false) }
    var showDialogVaso by remember { mutableStateOf(false) }

    // Lista de valores posibles para la frecuencia en minutos
    val minutosList = listOf(30, 60, 90, 120, 150, 180, 210, 240)

    // Inicializa el índice del slider acorde al valor actual, o usa el más cercano si no está en la lista
    var sliderIndex by remember { mutableIntStateOf(minutosList.indexOf(freqMinDS).takeIf { it >= 0 } ?: 0) }

    // --- Lanzador de permiso de notificaciones ---
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("No se otorgó permiso para notificaciones.")
            }
        }
    }

    // --- Sincronizar estados locales con DataStore ---
    LaunchedEffect(aguaHoy) { totalAgua = aguaHoy }
    LaunchedEffect(objetivoDS) { objetivoML = objetivoDS }
    LaunchedEffect(porVasoDS) { mlPorVaso = porVasoDS }

    // -- Lógica agregar agua --
    fun agregarAgua(cantidad: Int) {
        if (totalAgua == 0 && cantidad < 0){
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("Ya no hay agua")
            }
            return
        }
        totalAgua += cantidad
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val text = if (cantidad > 0) "Agregaste $cantidad ml 💧" else "Quitaste $cantidad ml 💧"
        scope.launch {
            context.guardarAguaHoy(totalAgua.coerceAtLeast(0))
            actualizarWidgetAgua(context)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
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
            actualizarWidgetAgua(context)
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
            scope.launch {
                context.guardarObjetivo(objetivoML)
                actualizarWidgetAgua(context)
            }
            if (notifDS) {
                programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
            }
        }
    }
    LaunchedEffect(mlPorVaso) {
        if (mlPorVaso != porVasoDS) {
            scope.launch {
                context.guardarPorVaso(mlPorVaso)
                actualizarWidgetAgua(context)
            }
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
            Spacer(Modifier.height(16.dp))
            Text(
                "${(totalAgua / 1000f).let { "%.2f".format(it) }} L / ${(objetivoML / 1000f).let { "%.2f".format(it) }} L",
                fontSize = 20.sp
            )
            // --- Visual de progreso ---
            AguaLevelBar(totalAgua, objetivoML)

            // --- Selector de objetivo ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Meta diaria: ${(objetivoML / 1000f).let { "%.2f".format(it) }}L", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))

                var lastSliderValue by remember { mutableFloatStateOf(objetivoML.toFloat()) }
                Slider(
                    value = objetivoML / 1000f,
                    onValueChange = { valor ->
                        val step = 0.25f
                        val newValue = (valor / step).roundToInt() * step
                        objetivoML = (newValue * 1000).roundToInt()
                        if (newValue != lastSliderValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastSliderValue = newValue
                        }
                    },
                    valueRange = 1.5f..3f,
                    steps = ((3f - 1.5f) / 0.25f).toInt() - 1,
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Agregar agua", fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { agregarAgua(mlPorVaso) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.water_full),
                        contentDescription = "Agregar agua",
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("+$mlPorVaso ml", fontSize = 18.sp)
                }
                Spacer(Modifier.width(32.dp))
                Button(
                    onClick = { agregarAgua(-mlPorVaso) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.water_loss),
                        contentDescription = "Quitar agua",
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("-$mlPorVaso ml", fontSize = 18.sp)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showDialogVaso = true }
                ) { Text("Cambiar cantidad del vaso") }
            }

            // --- Area de Notificaciones ---
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
                Text("Notificaciones", fontSize = 16.sp)
                Switch(
                    checked = notifDS,
                    onCheckedChange = { checked ->
                        // Si el usuario activa el switch, pedir permiso en Android 13+
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            context.guardarNotificacionesActivas(checked)
                            if (checked) {
                                programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Notificaciones activadas")
                            } else {
                                cancelarRecordatorioAgua(context)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Notificaciones desactivadas")
                            }
                        }
                    }
                )
            }
            //Si el usuario activa las notificaciones se le pregunta la frecuencia de estas
            if (notifDS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Frecuencia: ${minutosList[sliderIndex] / 60f} hs", fontSize = 16.sp)
                    Slider(
                        value = sliderIndex.toFloat(),
                        onValueChange = { value ->
                            sliderIndex = value.roundToInt().coerceIn(0, minutosList.size - 1)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onValueChangeFinished = {
                            val minutosSeleccionados = minutosList[sliderIndex]
                            scope.launch { context.guardarFrecuenciaMinutos(minutosSeleccionados) }
                            // Si usás alarmas, podés actualizar acá también
                        },
                        valueRange = 0f..(minutosList.size - 1).toFloat(),
                        steps = minutosList.size - 2, // Para 6 valores: 4 pasos intermedios
                        modifier = Modifier.width(200.dp)
                    )
                }
            }
            Spacer(Modifier.weight(1f))

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
            Spacer(Modifier.height(45.dp))
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

    //Menú de ayuda con información sobre la tool
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Sobre el recordatorio de agua") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Lleva el control de la cantidad de agua que consumes a diario.")
                    Text("• Puedes configurar tu meta diaria y la cantidad que sumas con cada botón.")
                    Text("• Si activas las notificaciones, la app te recordará tomar agua según el intervalo elegido.")
                    Text("• Todo se almacena localmente y no se comparte fuera de tu dispositivo.")
                    Text("• Puedes agregar el widget a tu pantalla de inicio para ver tu progreso diario y sumar o restar agua rápidamente sin abrir la app.")
                    Text("💧 Recomendación: un adulto debe consumir entre 1.5 y 3 litros de agua pura al día. La meta diaria sugerida es de 2 litros, pero puedes ajustarla según tus necesidades, actividad y clima.")
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

/** Funcion para dibujar la barra de progreso de consumo de agua, debe llamarse dentro de Scaffold*/
@Composable
    fun AguaLevelBar(ml: Int, objetivo: Int) {
        val colorFondo = MaterialTheme.colorScheme.surfaceBright
        val colorProgreso = MaterialTheme.colorScheme.primary
        val colorClaro = MaterialTheme.colorScheme.onSurfaceVariant
        val colorOscuro = MaterialTheme.colorScheme.onPrimary
        val grosor = 38.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(grosor)
        ) {
            val frac = (ml / objetivo.toFloat()).coerceIn(0f, 1f)

            // Fondo
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = colorFondo,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                )
            }

            // Progreso
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = colorProgreso,
                    size = androidx.compose.ui.geometry.Size(width = size.width * frac, height = size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
                )
            }

            // Texto de porcentaje
            val porcentaje = (frac * 100).roundToInt()
            val textColor = if (frac > 0.9f) colorOscuro else colorClaro

            Text(
                text = "$porcentaje%",
                color = textColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
    }

/**Funcion para re/programar el recordatorio de agua cada vez que cambia el progreso de consumo de agua*/
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

    alarmManager.cancel(pendingIntent)  // Cancela cualquier alarma previa
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        frecuenciaMinutos * 60 * 1000L,
        pendingIntent
    )
}


/**Funcion para cancelar el recordatorio de agua cuando se desactivan las notificaciones*/
fun cancelarRecordatorioAgua(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WaterReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 101,
        intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

/**Funcion para actualizar el progreso del consumo de agua en el widget desde la app*/
fun actualizarWidgetAgua(context: Context) {
    // Lanza la actualización en un hilo de fondo
    CoroutineScope(Dispatchers.IO).launch {
        val glanceManager = GlanceAppWidgetManager(context)
        val glanceIds = glanceManager.getGlanceIds(AguaWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AguaWidget.KEY_AGUA] = context.flujoAguaHoy().first()
                    this[AguaWidget.KEY_OBJETIVO] = context.flujoObjetivo().first()
                    this[AguaWidget.KEY_POR_VASO] = context.flujoPorVaso().first()
                }
            }
        }
        AguaWidget().updateAll(context)
    }
}