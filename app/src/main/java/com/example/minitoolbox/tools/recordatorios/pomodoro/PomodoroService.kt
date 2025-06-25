package com.example.minitoolbox.tools.pomodoro

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*

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
            }
        }

        val workMin  = intent?.getIntExtra("WORK_MINUTES", 25) ?: 25
        val shortMin = intent?.getIntExtra("SHORT_BREAK", 5)  ?: 5
        val longMin  = intent?.getIntExtra("LONG_BREAK", 15)  ?: 15

        // Mostrar notificación de conteo inicial (MM:00)
        startForeground(
            NOTIFICATION_ID,
            buildCountdownNotification(this, "Pomodoro iniciado", "%02d:00".format(workMin))
        )

        scope.launch {
            while (isActive) {
                runPhase("Trabajo", workMin)
                cycleCount++
                if (cycleCount % 4 == 0) runPhase("Descanso largo", longMin)
                else                    runPhase("Descanso corto", shortMin)
            }
        }
        return START_STICKY
    }

    private suspend fun runPhase(name: String, minutes: Int) {
        // Descontar 5 segundos de heads-up del siguiente ciclo
        val pauseSec = 5L
        val totalSec = if (cycleCount > 0) minutes * 60L - pauseSec
        else                 minutes * 60L

        // Guardar estado
        val endMs = System.currentTimeMillis() + totalSec * 1000L
        stateRepo.updatePhase(name, endMs, totalSec)

        // Vibrar breve al inicio de fase
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

        // Conteo por segundo (solo bandeja)
        for (sec in totalSec downTo 1) {
            val mm = sec / 60
            val ss = sec % 60
            NotificationManagerCompat.from(this)
                .notify(
                    NOTIFICATION_ID,
                    buildCountdownNotification(this, "⏳ $name", "%02d:%02d".format(mm, ss))
                )
            delay(1000L)
        }

        // Heads-up de fin de fase
        NotificationManagerCompat.from(this)
            .notify(
                NOTIFICATION_ID,
                buildAlarmNotification(this, "✅ $name terminado", "Pulsa para silenciar o detener")
            )

        // Vibración y sonido de alarma
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        playSound()

        // Mantener heads-up visible al menos 5 s
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
