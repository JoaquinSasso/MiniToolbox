package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
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
        ensurePomodoroChannels(this)
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
        showRunningNotification(
            this,
            getString(R.string.pomodoro_work),
            System.currentTimeMillis() + workMin * 60_000L
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

        // Mostrar la notificación "en curso" una sola vez (sin FGS, sin actualizar cada segundo)
        showRunningNotification(
            this,
            "⏳ $name",           // o usa getString(R.string.pomodoro_work) / short_break / long_break
            endMs                 // hora de fin de la fase (en millis)
        )

        // Cuando termine la fase (en tu flujo actual, justo antes de agendar la siguiente):
        cancelRunningNotification(this) // opcional, para ocultar la "en curso" antes de la alarma
        showAlarmNotification(
            this,
            getString(R.string.pomodoro_finished, name),
            getString(R.string.pomodoro_tap_to_stop)
        )

        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        delay(5_000L)
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
