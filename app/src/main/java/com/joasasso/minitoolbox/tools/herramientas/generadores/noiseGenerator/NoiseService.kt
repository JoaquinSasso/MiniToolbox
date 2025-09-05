package com.joasasso.minitoolbox.tools.herramientas.generadores.noiseGenerator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoiseService : Service() {

    companion object {
        private const val CHANNEL_ID = "noise_channel"
        private const val NOTIF_ID = 2025

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE = "ACTION_TOGGLE"

        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val EXTRA_VOLUME = "EXTRA_VOLUME"
        const val EXTRA_TIMER_MS = "EXTRA_TIMER_MS"
        const val EXTRA_FADE_MS = "EXTRA_FADE_MS"

        fun startForeground(
            context: Context,
            type: NoiseType,
            volume: Float,
            timerMs: Long? = null,
            fadeMs: Long = 3000
        ) {
            val i = Intent(context, NoiseService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TYPE, type.name)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_TIMER_MS, timerMs ?: -1L)
                putExtra(EXTRA_FADE_MS, fadeMs)
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun toggle(context: Context) {
            val i = Intent(context, NoiseService::class.java).apply {
                action = ACTION_TOGGLE
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, NoiseService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, i)
        }
    }

    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val type = NoiseType.valueOf(
                    intent.getStringExtra(EXTRA_TYPE) ?: NoiseType.White.name
                )
                val vol = intent.getFloatExtra(EXTRA_VOLUME, 0.5f)
                val timerMs = intent.getLongExtra(EXTRA_TIMER_MS, -1L).takeIf { it > 0 }
                val fadeMs = intent.getLongExtra(EXTRA_FADE_MS, 3000L)

                NoiseEngine.setVolume(vol)
                if (!NoiseEngine.isRunning()) NoiseEngine.start(type) else NoiseEngine.changeType(type)

                startForeground(NOTIF_ID, buildNotification(isPlaying = true, type, vol, timerMs))
                manageTimer(timerMs, fadeMs)
            }

            ACTION_TOGGLE -> {
                if (NoiseEngine.isRunning()) {
                    stopSelfSafely(fadeMs = 1000L)
                } else {
                    NoiseEngine.setVolume(0.5f)
                    NoiseEngine.start(NoiseType.White)
                    startForeground(
                        NOTIF_ID,
                        buildNotification(true, NoiseType.White, 0.5f, null)
                    )
                }
            }

            ACTION_STOP -> stopSelfSafely(fadeMs = 1000L)
        }
        return START_STICKY
    }

    private fun manageTimer(timerMs: Long?, fadeMs: Long) {
        timerJob?.cancel()
        if (timerMs == null) return
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(timerMs)
            NoiseEngine.stop(fadeMs)
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun stopSelfSafely(fadeMs: Long) {
        CoroutineScope(Dispatchers.Default).launch {
            NoiseEngine.stop(fadeMs)
            stopForegroundCompat()
            stopSelf()
        }
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(
        isPlaying: Boolean,
        type: NoiseType,
        volume: Float,
        timerMs: Long?
    ): Notification {
        // Intents con 'action' en el propio Intent (NO en PendingIntent)
        val stopIntent = Intent(this, NoiseService::class.java).apply { action = ACTION_STOP }
        val toggleIntent = Intent(this, NoiseService::class.java).apply { action = ACTION_TOGGLE }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stopPi = PendingIntent.getService(this, 1, stopIntent, flags)
        val togglePi = PendingIntent.getService(this, 2, toggleIntent, flags)

        val title = getString(R.string.noise_notif_title)
        val text = buildString {
            append(type.name)
            append(" • Vol ${(volume * 100).toInt()}%")
            if (timerMs != null) append(" • ${timerMs / 60000} min")
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.noise_generator) // reemplaza por tu recurso
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_pause, getString(R.string.noise_notif_toggle), togglePi)
            .addAction(R.drawable.ic_stop, getString(R.string.noise_notif_stop), stopPi)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.noise_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
