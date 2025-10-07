package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.flujoAguaHoy
import com.joasasso.minitoolbox.data.flujoFrecuenciaMinutos
import com.joasasso.minitoolbox.data.flujoNotificacionesActivas
import com.joasasso.minitoolbox.data.flujoObjetivo
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.data.guardarAguaHoy
import com.joasasso.minitoolbox.data.guardarFrecuenciaMinutos
import com.joasasso.minitoolbox.data.guardarNotificacionesActivas
import com.joasasso.minitoolbox.data.guardarObjetivo
import com.joasasso.minitoolbox.data.guardarPorVaso
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.widgets.AguaMiniWidget
import com.joasasso.minitoolbox.widgets.AguaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaReminderScreen(
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

    // Helper para buscar el índice más cercano si el valor no está en la lista
    fun closestIndex(values: List<Int>, target: Int): Int =
        values.withIndex().minByOrNull { kotlin.math.abs(it.value - target) }?.index ?: 0

    // Inicializa el índice según el valor actual del DS (o el más cercano)
    var sliderIndex by remember {
        val exact = minutosList.indexOf(freqMinDS)
        mutableIntStateOf(if (exact >= 0) exact else closestIndex(minutosList, freqMinDS))
    }

    // Strings para el snackbar
    val zeroWarning = stringResource(R.string.water_zero_warning)
    val addedText = stringResource(R.string.water_added_feedback)
    val removedText = stringResource(R.string.water_removed_feedback)
    val resetText = stringResource(R.string.water_reset_feedback)
    val deniedText = stringResource(R.string.notif_permission_denied)

    // --- Lanzador de permiso de notificaciones ---
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar(deniedText)
            }
        }
    }

    // --- Sincronizar estados locales con DataStore ---
    LaunchedEffect(aguaHoy) { totalAgua = aguaHoy }
    LaunchedEffect(objetivoDS) { objetivoML = objetivoDS }
    LaunchedEffect(porVasoDS) { mlPorVaso = porVasoDS }
    LaunchedEffect(notifDS) { }
    LaunchedEffect(freqMinDS) { }

    // -- Lógica agregar agua --
    fun agregarAgua(
        cantidad: Int,
        zeroWarning: String,
        addedText: String,
        removedText: String
    ) {
        if (totalAgua == 0 && cantidad < 0) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(zeroWarning)
            }
            return
        }
        totalAgua += cantidad
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val text = if (cantidad > 0) addedText.format(cantidad) else removedText.format(kotlin.math.abs(cantidad))
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
    fun resetear(resetText: String) {
        totalAgua = 0
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            context.guardarAguaHoy(0)
            actualizarWidgetAgua(context)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(resetText)
        }
        if (notifDS) {
            programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
        }
    }
    //Actualizar widget y DataStore si cambia el objetivo
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
    //Actualizar widget y DataStore si cambia la cantidad del vaso
    LaunchedEffect(mlPorVaso) {
        if (mlPorVaso != porVasoDS) {
            scope.launch {
                context.guardarPorVaso(mlPorVaso)
                actualizarWidgetAgua(context)
            }
        }
    }

    //Crear canal de notificaciones y programar actualizacion diaria del widget
    LaunchedEffect(Unit) {
        createWaterReminderChannel(context)
        programarResetAguaDiario(context)
    }

    //Reprogramar recordatorio si se activa las notificaciones
    LaunchedEffect(freqMinDS) {
        // Sincroniza el knob con el valor persistido
        val exact = minutosList.indexOf(freqMinDS)
        sliderIndex = if (exact >= 0) exact else closestIndex(minutosList, freqMinDS)

        // Si las notificaciones están activas, reprogramá con el valor persistido
        if (notifDS) {
            programarRecordatorioAgua(context, freqMinDS, totalAgua, objetivoML)
        }
    }


    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_water_reminder), onBack, {showInfo = true})},
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
                Text("${context.getString(R.string.water_goal_label)} ${(objetivoML / 1000f).let { "%.2f".format(it) }}L", fontSize = 16.sp)
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { agregarAgua(-mlPorVaso, zeroWarning, addedText, removedText) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.water_loss),
                        contentDescription = stringResource(R.string.water_remove),
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("-$mlPorVaso ml", fontSize = 18.sp)
                }
                Spacer(Modifier.width(32.dp))
                Button(
                    onClick = { agregarAgua(mlPorVaso, zeroWarning, addedText, removedText) }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.water_full),
                        contentDescription = stringResource(R.string.water_add),
                        Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("+$mlPorVaso ml", fontSize = 18.sp)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showDialogVaso = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) { Text(stringResource(R.string.water_change_glass)) }
            }

            // --- Area de Notificaciones ---
            HorizontalDivider(
                Modifier.padding(vertical = 10.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )
            Text(stringResource(R.string.water_notif_title), fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.water_notif_enabled), fontSize = 16.sp)
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
                                snackbarHostState.showSnackbar(context.getString(R.string.notif_enabled_snackbar))
                            } else {
                                cancelarRecordatorioAgua(context)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(context.getString(R.string.notif_disabled_snackbar))
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
                    Text(stringResource(R.string.water_notif_freq, freqMinDS / 60f), fontSize = 16.sp)
                    Slider(
                        value = sliderIndex.toFloat(),
                        onValueChange = { value ->
                            sliderIndex = value.roundToInt().coerceIn(0, minutosList.size - 1)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onValueChangeFinished = {
                            val minutosSeleccionados = minutosList[sliderIndex]
                            scope.launch { context.guardarFrecuenciaMinutos(minutosSeleccionados) }
                            programarRecordatorioAgua(context, minutosSeleccionados, totalAgua, objetivoML)
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
                        resetear(resetText)
                    }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.reset_content_desc))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reset_content_desc))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onShowEstadisticas()
                    }
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.stats_content_desc))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.stats))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            )
            {
                Text(stringResource(R.string.water_widget_hint))
            }
        }
    }

    // --- Cambiar cantidad del vaso (diálogo simple) ---
    if (showDialogVaso) {
        var tempMl by remember { mutableStateOf(mlPorVaso.toString()) }
        AlertDialog(
            onDismissRequest = { showDialogVaso = false },
            title = { Text(stringResource(R.string.glass_amount_title)) },
            text = {
                OutlinedTextField(
                    value = tempMl,
                    onValueChange = { tempMl = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.glass_amount_label)) },
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
                }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialogVaso = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    //Menú de ayuda con información sobre la tool
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.water_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.water_help_line1))
                    Text(stringResource(R.string.water_help_line2))
                    Text(stringResource(R.string.water_help_line3))
                    Text(stringResource(R.string.water_help_line4))
                    Text(stringResource(R.string.water_help_line5))
                    Text(stringResource(R.string.water_help_line6))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.close)) }
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
    CoroutineScope(Dispatchers.IO).launch {
        val agua = context.flujoAguaHoy().first()
        val objetivo = context.flujoObjetivo().first()
        val porVaso = context.flujoPorVaso().first()

        val glanceManager = GlanceAppWidgetManager(context)

        // Actualiza AguaWidget (el largo)
        val idsLargos = glanceManager.getGlanceIds(AguaWidget::class.java)
        idsLargos.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AguaWidget.KEY_AGUA] = agua
                    this[AguaWidget.KEY_OBJETIVO] = objetivo
                    this[AguaWidget.KEY_POR_VASO] = porVaso
                }
            }
        }
        AguaWidget().updateAll(context)

        // Actualiza AguaMiniWidget (el compacto)
        val idsMini = glanceManager.getGlanceIds(AguaMiniWidget::class.java)
        idsMini.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AguaMiniWidget.KEY_AGUA] = agua
                    this[AguaMiniWidget.KEY_OBJETIVO] = objetivo
                    this[AguaMiniWidget.KEY_POR_VASO] = porVaso
                }
            }
        }
        AguaMiniWidget().updateAll(context)
    }
}


fun programarResetAguaDiario(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ResetAguaReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}
