package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val ACTION_STOP    = "STOP_POMODORO"
const val ACTION_SILENCE = "SILENCE_ALARM"

class PomodoroService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var stateRepo: PomodoroStateRepository
    private var mediaPlayer: MediaPlayer? = null
    private var cycleCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createPomodoroChannels(this)
        stateRepo = PomodoroStateRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("PomodoroService", "onStartCommand")

        intent?.action?.let { action ->
            when (action) {
                ACTION_STOP -> {
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_SILENCE -> {
                    mediaPlayer?.takeIf { it.isPlaying }?.apply {
                        stop()
                        reset()
                    }
                    return START_NOT_STICKY
                }

                else -> {}
            }
        }

        val workMin  = intent?.getIntExtra("WORK_MINUTES", 25) ?: 25
        val shortMin = intent?.getIntExtra("SHORT_BREAK", 5)  ?: 5
        val longMin  = intent?.getIntExtra("LONG_BREAK", 15)  ?: 15

        // Mostrar notificación de conteo inicial (MM:00)
        startForeground(
            NOTIFICATION_ID,
            buildCountdownNotification(
                this,
                getString(R.string.pomodoro_started),
                "%02d:00".format(workMin)
            )
        )


        scope.launch {
            try {
                while (isActive) {
                    runPhase("Trabajo", workMin)
                    cycleCount++
                    if (cycleCount % 4 == 0) runPhase("Descanso largo", longMin)
                    else                    runPhase("Descanso corto", shortMin)
                }
            }
            catch (_: SecurityException) { }
        }
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun runPhase(nameRaw: String, minutes: Int) {
        val name = when (nameRaw) {
            "Trabajo" -> getString(R.string.pomodoro_work)
            "Descanso corto" -> getString(R.string.pomodoro_short_break)
            "Descanso largo" -> getString(R.string.pomodoro_long_break)
            else -> nameRaw
        }

        val pauseSec = 5L
        val totalSec = if (cycleCount > 0) minutes * 60L - pauseSec else minutes * 60L
        val startMs = System.currentTimeMillis()
        val endMs = startMs + totalSec * 1000L

        stateRepo.updatePhase(name, endMs, totalSec)

        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

        while (true) {
            val now = System.currentTimeMillis()
            val remainingMs = endMs - now
            val remainingSec = (remainingMs / 1000L).coerceAtLeast(0L)
            val mm = remainingSec / 60
            val ss = remainingSec % 60
            NotificationManagerCompat.from(this)
                .notify(
                    NOTIFICATION_ID,
                    buildCountdownNotification(this, "⏳ $name", "%02d:%02d".format(mm, ss))
                )
            if (remainingMs <= 0) break
            delay((remainingMs % 1000L).let { if (it > 0) it else 1000L })
        }

        NotificationManagerCompat.from(this)
            .notify(
                NOTIFICATION_ID,
                buildAlarmNotification(
                    this,
                    getString(R.string.pomodoro_finished, name),
                    getString(R.string.pomodoro_tap_to_stop)
                )
            )

        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        playSound()
        delay(5_000L)
    }


    private fun playSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = false
                prepare()
                start()
            }
            // Detener la alarma automáticamente después de 10 segundos
            scope.launch {
                delay(10_000L)
                mediaPlayer?.takeIf { it.isPlaying }?.apply {
                    stop()
                    reset()
                }
            }
        } catch (e: Exception) {
            Log.e("PomodoroService", "Error sonido: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        CoroutineScope(Dispatchers.IO).launch {
            stateRepo.clearPhase()
        }
        Log.d("PomodoroService", "Servicio detenido")
    }
}
