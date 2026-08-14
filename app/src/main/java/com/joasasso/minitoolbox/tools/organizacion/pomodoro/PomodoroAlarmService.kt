package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R

const val ACTION_RING_ALARM   = "POMODORO_RING_ALARM"
const val ACTION_STOP_RINGING = "POMODORO_STOP_RINGING"

internal const val EX_RING_TITLE = "ring_title"
internal const val EX_RING_TEXT  = "ring_text"
internal const val EX_RING_ROUTE = "ring_route"

/** Cuánto suena la alarma antes de auto-silenciarse. */
private const val RING_MS = 30_000L

/**
 * Servicio en primer plano que hace sonar la alarma de fin de fase.
 *
 * ¿Por qué un servicio y no el BroadcastReceiver?
 * Cuando onReceive() (o goAsync().finish()) termina, el proceso vuelve a estado
 * "cached" y el sistema lo puede matar en cualquier momento. El MediaPlayer y el
 * Handler.postDelayed que había en AlarmSoundPlayer vivían justo en esa ventana,
 * así que el sonido podía cortarse o no sonar nunca. Un foreground service es la
 * única forma soportada de mantener audio vivo desde background.
 *
 * El arranque desde background está permitido porque venimos de una alarma
 * exacta (es una de las exenciones documentadas), y el tipo mediaPlayback está
 * explícitamente habilitado para "apps con SCHEDULE_EXACT_ALARM o USE_EXACT_ALARM
 * que usan un foreground service para continuar alarmas en background".
 */
class PomodoroAlarmService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_RINGING) {
            stopRinging()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EX_RING_TITLE)
            ?: getString(R.string.tool_pomodoro_timer)
        val text = intent?.getStringExtra(EX_RING_TEXT)
            ?: getString(R.string.pomodoro_tap_to_stop)
        val route = intent?.getStringExtra(EX_RING_ROUTE)

        // 1) Primero de todo: pasar a primer plano. Hay ~10 s de margen antes de
        //    que el sistema mate el proceso por no llamar a startForeground().
        ensurePomodoroChannels(this)
        val notif = buildAlarmNotification(this, title, text, route)
        ServiceCompat.startForeground(
            this,
            NOTIF_ID_ALARM_SILENT,
            notif,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else 0
        )

        // 2) Estado visible para la UI
        AlarmState.setActive(this, true)
        sendBroadcast(
            Intent(ACTION_POMODORO_ALARM_START)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
        )

        // 3) Sonido + vibración
        acquireWakeLock()
        startAudio()
        startVibration()

        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, RING_MS)

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniToolbox:PomodoroRing")
            ?.apply {
                setReferenceCounted(false)
                try { acquire(RING_MS + 5_000L) } catch (_: Exception) { }
            }
    }

    private fun startAudio() {
        releasePlayer()
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        try {
            val mp = MediaPlayer()
            player = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // Mantiene la CPU despierta mientras reproduce (necesita WAKE_LOCK)
            mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mp.isLooping = true
            mp.setDataSource(applicationContext, uri)
            mp.setOnPreparedListener { it.start() }
            mp.setOnErrorListener { _, what, extra ->
                Log.w("PomodoroAlarmService", "MediaPlayer error $what/$extra")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w("PomodoroAlarmService", "No se pudo reproducir la alarma", e)
            releasePlayer()
        }
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    private fun startVibration() {
        val v = vibrator() ?: return
        val pattern = longArrayOf(0, 600, 400)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
        } catch (_: Exception) { }
    }

    private fun releasePlayer() {
        player?.let {
            try { if (it.isPlaying) it.stop() } catch (_: Exception) { }
            try { it.reset() } catch (_: Exception) { }
            try { it.release() } catch (_: Exception) { }
        }
        player = null
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        try { vibrator()?.cancel() } catch (_: Exception) { }
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) { }
        wakeLock = null

        AlarmState.setActive(this, false)
        sendBroadcast(
            Intent(ACTION_POMODORO_ALARM_STOP)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
        )

        ContextCompat.getSystemService(this, NotificationManager::class.java)
            ?.cancel(NOTIF_ID_ALARM_SILENT)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        releasePlayer()
        try { vibrator()?.cancel() } catch (_: Exception) { }
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) { }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        fun ring(context: Context, title: String, text: String, startRoute: String?) {
            val i = Intent(context, PomodoroAlarmService::class.java).apply {
                action = ACTION_RING_ALARM
                putExtra(EX_RING_TITLE, title)
                putExtra(EX_RING_TEXT, text)
                putExtra(EX_RING_ROUTE, startRoute)
            }
            ContextCompat.startForegroundService(context.applicationContext, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, PomodoroAlarmService::class.java).apply {
                action = ACTION_STOP_RINGING
            }
            try {
                context.applicationContext.startService(i)
            } catch (_: Exception) {
                // El servicio no estaba vivo: no hay nada que silenciar.
            }
        }
    }
}