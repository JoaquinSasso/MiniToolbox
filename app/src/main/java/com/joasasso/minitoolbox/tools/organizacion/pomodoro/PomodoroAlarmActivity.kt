package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R

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
 * TODO: si la app tiene un composable de tema propio (p. ej. MiniToolboxTheme),
 * reemplazar el MaterialTheme { } de abajo por ese, para que colores/tipografía
 * coincidan con el resto de la app. Se dejó MaterialTheme puro para que
 * compile sin depender de un nombre que no puedo confirmar desde acá.
 */
class PomodoroAlarmActivity : ComponentActivity() {

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_POMODORO_ALARM_STOP) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val title = intent.getStringExtra(EX_ALARM_TITLE) ?: getString(R.string.tool_pomodoro_timer)
        val text  = intent.getStringExtra(EX_ALARM_TEXT) ?: getString(R.string.pomodoro_tap_to_stop)
        val route = intent.getStringExtra(EX_ALARM_ROUTE)

        setContent {
            MaterialTheme {
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
        }
        startActivity(i)
        finish()
    }
}

@Composable
private fun PomodoroAlarmRingingScreen(
    title: String,
    text: String,
    onSilence: () -> Unit,
    onStopPomodoro: () -> Unit,
    onOpenApp: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSilence()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text(stringResource(R.string.pomodoro_silence), fontSize = 20.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onStopPomodoro()
            }) {
                Text(stringResource(R.string.pomodoro_finish_full))
            }

            if (onOpenApp != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenApp) {
                    Text(stringResource(R.string.pomodoro_open_app))
                }
            }
        }
    }
}
