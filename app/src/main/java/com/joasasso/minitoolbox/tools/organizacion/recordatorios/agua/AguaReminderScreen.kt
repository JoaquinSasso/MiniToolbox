package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.utils.NotificationPermission
import kotlinx.coroutines.launch
import com.joasasso.minitoolbox.widgets.AguaWidgetReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaReminderScreen(
    onBack: () -> Unit,
    onShowEstadisticas: () -> Unit,
    viewModel: AguaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showAjustes by rememberSaveable { mutableStateOf(false) }
    var showConfirmReset by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val zeroWarning = stringResource(R.string.water_zero_warning)
    val addedText = stringResource(R.string.water_added_feedback)
    val removedText = stringResource(R.string.water_removed_feedback)
    val resetText = stringResource(R.string.water_reset_feedback)
    val undoText = stringResource(R.string.water_undo)

    // Estado real del sistema, no solo la preferencia guardada: el usuario puede bloquear las
    // notificaciones desde Ajustes de Android y el switch no se entera.
    fun notificacionesPermitidas() = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var notifPermitidas by remember { mutableStateOf(notificacionesPermitidas()) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermitidas = notificacionesPermitidas()
        // Solo se activa con permiso: antes el switch quedaba encendido aunque se rechazara
        if (granted) viewModel.toggleNotificaciones(true)
    }

    // Al volver a primer plano, verificar si cambió la fecha (medianoche) y el permiso
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.verificarFecha()
                notifPermitidas = notificacionesPermitidas()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /**
     * Cada snackbar se lanza en su propia corrutina: si el colector esperara a que termine,
     * tres toques seguidos encolarían tres avisos de 4 s y el "Deshacer" llegaría tarde.
     */
    fun mostrarAviso(mensaje: String, onDeshacer: (() -> Unit)? = null) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val resultado = snackbarHostState.showSnackbar(
                message = mensaje,
                actionLabel = if (onDeshacer != null) undoText else null,
                // Con acción, 4 s no alcanzan para leer y reaccionar: el aviso dura 10 s
                duration = if (onDeshacer != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (resultado == SnackbarResult.ActionPerformed) onDeshacer?.invoke()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AguaEvent.ShowZeroWarning -> mostrarAviso(zeroWarning)
                is AguaEvent.WaterAdded -> mostrarAviso(addedText.format(event.cantidad)) {
                    viewModel.deshacerCambio(event.cantidad)
                }
                is AguaEvent.WaterRemoved -> mostrarAviso(removedText.format(event.cantidad)) {
                    viewModel.deshacerCambio(-event.cantidad)
                }
                is AguaEvent.WaterReset -> mostrarAviso(resetText) {
                    viewModel.deshacerReset(event.anterior)
                }
            }
        }
    }

    // Crear canal de notificaciones y programar actualización diaria del widget
    LaunchedEffect(Unit) {
        createWaterReminderChannel(context)
        programarResetAguaDiario(context)
        viewModel.scheduleMidnightCheck()
    }

    // Vibración distinta al cruzar la meta, no al abrir la pantalla con la meta ya cumplida
    val alcanzado = !uiState.isLoading &&
        AguaMath.objetivoAlcanzado(uiState.totalAgua, uiState.objetivoML, uiState.mlPorVaso)
    var alcanzadoAnterior by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(alcanzado, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        if (alcanzadoAnterior == false && alcanzado) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        alcanzadoAnterior = alcanzado
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_water_reminder),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onShowEstadisticas) {
                        Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.stats))
                    }
                    IconButton(onClick = { showAjustes = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.water_settings))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.water_more_options))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.water_reset_menu)) },
                                leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
                                enabled = uiState.totalAgua > 0,
                                onClick = {
                                    showMenu = false
                                    showConfirmReset = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.water_help_menu)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    showInfo = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgresoHoy(
                totalML = uiState.totalAgua,
                objetivoML = uiState.objetivoML,
                mlPorVaso = uiState.mlPorVaso,
                alcanzado = alcanzado
            )
            AccionesRegistro(
                totalML = uiState.totalAgua,
                mlPorVaso = uiState.mlPorVaso,
                cantidadesRapidas = uiState.cantidadesRapidas,
                onCambio = { delta ->
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    viewModel.agregarAgua(delta)
                }
            )
            ResumenSemana(
                semana = uiState.semana,
                hoy = uiState.fecha,
                objetivoML = uiState.objetivoML,
                mlPorVaso = uiState.mlPorVaso,
                onClick = onShowEstadisticas
            )
            SugerenciaWidget()
        }
    }

    if (showAjustes) {
        AguaAjustesSheet(
            uiState = uiState,
            onDismiss = { showAjustes = false },
            onObjetivo = viewModel::guardarObjetivo,
            onPorVaso = viewModel::guardarPorVaso,
            onCantidadesRapidas = viewModel::guardarCantidadesRapidas,
            onNotificaciones = { activar ->
                when {
                    !activar -> viewModel.toggleNotificaciones(false)
                    NotificationPermission.shouldRequest(context) ->
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Bloqueadas desde Ajustes de Android (o rechazadas dos veces): no se enciende
                    // y el aviso de abajo explica por qué
                    !notificacionesPermitidas() -> notifPermitidas = false
                    else -> viewModel.toggleNotificaciones(true)
                }
            },
            onFrecuencia = viewModel::guardarFrecuenciaMinutos,
            onHorario = viewModel::guardarHorarioRecordatorio,
            onPausarAlCumplir = viewModel::guardarPausarAlCumplir,
            avisoPermiso = {
                if (!notifPermitidas) {
                    AvisoNotificacionesBloqueadas(onAbrirAjustes = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    })
                }
            }
        )
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            icon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.water_reset_confirm_title)) },
            text = {
                Text(stringResource(R.string.water_reset_confirm_text, AguaFormat.litros(uiState.totalAgua)))
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmReset = false
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    viewModel.resetear()
                }) { Text(stringResource(R.string.water_reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.water_help_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.water_help_line1))
                    Text(stringResource(R.string.water_help_line2))
                    Text(stringResource(R.string.water_help_line3))
                    Text(stringResource(R.string.water_help_line4))
                    Text(stringResource(R.string.water_help_line5))
                    Text(stringResource(R.string.water_help_line6))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgresoHoy(totalML: Int, objetivoML: Int, mlPorVaso: Int, alcanzado: Boolean) {
    val fraccion = if (objetivoML > 0) (totalML / objetivoML.toFloat()).coerceIn(0f, 1f) else 1f
    val progreso by animateFloatAsState(
        targetValue = fraccion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "progresoAgua"
    )
    val colorProgreso = if (alcanzado) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val stroke = with(LocalDensity.current) { Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .widthIn(max = 320.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            CircularWavyProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxSize(),
                color = colorProgreso,
                // surfaceContainerHighest: con secondaryContainer el riel y el progreso eran dos tonos casi iguales
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                stroke = stroke,
                trackStroke = stroke,
                wavelength = 48.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.water_liters_value, AguaFormat.litros(totalML)),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.water_of_goal, AguaFormat.litros(objetivoML)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val vasosObjetivo = AguaMath.calcularVasosObjetivo(objetivoML, mlPorVaso)
                Text(
                    pluralStringResource(
                        R.plurals.water_glasses_progress,
                        vasosObjetivo,
                        AguaMath.calcularVasosConsumidos(totalML, mlPorVaso),
                        vasosObjetivo
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (alcanzado) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.water_goal_reached),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        } else {
            val faltan = (objetivoML - totalML).coerceAtLeast(0)
            Text(
                if (AguaFormat.usaLitros(faltan)) {
                    stringResource(R.string.water_remaining_liters, AguaFormat.litros(faltan))
                } else {
                    stringResource(R.string.water_remaining_ml, faltan)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccionesRegistro(
    totalML: Int,
    mlPorVaso: Int,
    cantidadesRapidas: List<Int>,
    onCambio: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Restar es una corrección, no la acción del día: va chico y tonal, a la izquierda del principal
            FilledTonalIconButton(
                onClick = { onCambio(-mlPorVaso) },
                enabled = totalML > 0,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.water_remove_desc, mlPorVaso)
                )
            }
            Button(
                onClick = { onCambio(mlPorVaso) },
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .widthIn(min = 180.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.water_full),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.water_add, mlPorVaso), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (cantidadesRapidas.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                cantidadesRapidas.forEach { ml ->
                    key("rapida-$ml") {
                        SuggestionChip(
                            onClick = { onCambio(ml) },
                            label = { Text(stringResource(R.string.water_add, ml)) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.water_drop),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvisoNotificacionesBloqueadas(onAbrirAjustes: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsOff, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.water_notif_blocked),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(onClick = onAbrirAjustes, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.water_notif_open_settings))
            }
        }
    }
}

/**
 * Ofrece fijar el widget en la pantalla de inicio; si el launcher no lo permite, deja el texto
 * de ayuda. No se oculta cuando ya hay un widget: getGlanceIds también devuelve ids huérfanos
 * que el launcher reservó pero no muestra, y el botón desaparecería sin motivo.
 */
@Composable
private fun SugerenciaWidget() {
    val context = LocalContext.current
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        OutlinedButton(onClick = {
            appWidgetManager.requestPinAppWidget(ComponentName(context, AguaWidgetReceiver::class.java), null, null)
        }) {
            Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.water_widget_add))
        }
    } else {
        Text(
            stringResource(R.string.water_widget_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
