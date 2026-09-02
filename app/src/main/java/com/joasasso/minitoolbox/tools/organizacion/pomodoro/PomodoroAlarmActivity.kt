package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.metrics.MetricsSource

internal const val EX_ALARM_TITLE = "alarm_title"
internal const val EX_ALARM_TEXT  = "alarm_text"
internal const val EX_ALARM_ROUTE = "alarm_route"

/**
 * Pantalla completa que se muestra cuando termina una fase del pomodoro,
 * incluso con el teléfono bloqueado — el mismo patrón que usa la app Reloj
 * de Android para sus alarmas.
 *
 * A propósito es una Activity separada, fuera del NavHost de MainActivity:
 * así no depende de que el deep-link a una ruta puntual funcione en un
 * arranque en frío (proceso matado) — que es justamente el escenario donde
 * más falta hace que esta pantalla aparezca sola, sin que el usuario tenga
 * que navegar a mano.
 *
 * Extiende AppCompatActivity, no ComponentActivity, por el mismo motivo que
 * MainActivity: Theme.MiniToolbox trae action bar, y hace falta el
 * supportActionBar?.hide() de abajo para sacarla. Con ComponentActivity no
 * hay forma de ocultarla sin declarar un tema aparte en el manifiesto.
 *
 * El layout es deliberadamente un espejo de PomodoroScreen: mismo título
 * arriba, mismo círculo con el mismo grosor y separación, y el botón de
 * silenciar del mismo tamaño y en la misma posición donde vive el de
 * iniciar/detener. La idea es que el pulgar caiga donde ya está
 * acostumbrado, y que la pantalla se lea como el timer en otro estado y no
 * como una pantalla ajena.
 */
class PomodoroAlarmActivity : AppCompatActivity() {

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_POMODORO_ALARM_STOP) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        showOverLockScreen()

        val title = intent.getStringExtra(EX_ALARM_TITLE) ?: getString(R.string.tool_pomodoro_timer)
        val text  = intent.getStringExtra(EX_ALARM_TEXT) ?: getString(R.string.pomodoro_tap_to_stop)
        val route = intent.getStringExtra(EX_ALARM_ROUTE)

        setContent {
            MiniToolboxTheme {
                PomodoroAlarmRingingScreen(
                    title = title,
                    text = text,
                    onSilence = {
                        PomodoroAlarmReceiver.silenceAlarm(applicationContext)
                        finish()
                    },
                    onStopPomodoro = {
                        PomodoroAlarmReceiver.stopPomodoro(applicationContext)
                        finish()
                    },
                    onOpenApp = route?.let { r -> { openMainActivity(r) } }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(stopReceiver, IntentFilter(ACTION_POMODORO_ALARM_STOP), Context.RECEIVER_NOT_EXPORTED)
        // Si la alarma ya se apagó por otra vía (p. ej. tocaron la acción de
        // la notificación mientras esta pantalla todavía cargaba, o venció el
        // auto-silencio de 30s), no dejar esta pantalla mostrando "sonando".
        if (!AlarmState.isActive(this)) finish()
    }

    override fun onStop() {
        super.onStop()
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) { }
    }

    private fun showOverLockScreen() {
        // Mantener la pantalla encendida mientras esta Activity esté al
        // frente, pase lo que pase con el timeout normal del sistema. Sin
        // este flag, setTurnScreenOn() prende la pantalla una sola vez al
        // arrancar, pero nada impide que el sistema la vuelva a apagar a los
        // pocos segundos — la Activity queda "arriba" pero invisible, y hay
        // que despertar el teléfono a mano para verla.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun openMainActivity(route: String) {
        val i = Intent(this, com.joasasso.minitoolbox.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("startRoute", route)
            putExtra(MetricsSource.EXTRA_START_SOURCE, MetricsSource.NOTIFICATION)
        }
        startActivity(i)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PomodoroAlarmRingingScreen(
    title: String,
    text: String,
    onSilence: () -> Unit,
    onStopPomodoro: () -> Unit,
    onOpenApp: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current

    // El círculo está completo (la fase terminó), así que el progreso no puede
    // comunicar nada. Lo que comunica que la alarma está sonando es la onda:
    // late entre amplitud baja y máxima. Mismo componente y mismos parámetros
    // que PomodoroScreen, sólo que animado.
    val pulse = rememberInfiniteTransition(label = "ringPulse")
    val amplitude by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAmplitude"
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSilence()
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(80.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = text.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp)
                // Deja libre la franja donde flota el FAB (80.dp + margen).
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1) TÍTULO ARRIBA — ocupa el lugar del nombre de fase en el timer
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 2) CÍRCULO — mismas medidas exactas que PomodoroScreen
            val stroke = with(LocalDensity.current) {
                Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                    stroke = stroke,
                    trackStroke = stroke,
                    gapSize = 10.dp,
                    amplitude = { amplitude },
                    wavelength = 80.dp,
                    waveSpeed = WavyProgressIndicatorDefaults.CircularWavelength
                )

                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(24.dp))

            // 3) ACCIONES SECUNDARIAS — peso visual bajo a propósito: silenciar
            //    es lo que el usuario quiere el 95% de las veces, y ese es el FAB.
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStopPomodoro()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.pomodoro_finish_full))
            }

            if (onOpenApp != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pomodoro_open_app))
                }
            }
        }
    }
}