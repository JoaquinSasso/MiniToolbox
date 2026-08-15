package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.joasasso.minitoolbox.R
import android.media.AudioAttributes as PlatformAudioAttributes
import androidx.media3.common.AudioAttributes as Media3AudioAttributes

const val ACTION_RING_ALARM   = "POMODORO_RING_ALARM"
const val ACTION_STOP_RINGING = "POMODORO_STOP_RINGING"

internal const val EX_RING_TITLE = "ring_title"
internal const val EX_RING_TEXT  = "ring_text"
internal const val EX_RING_ROUTE = "ring_route"

private const val TAG = "PomodoroAlarmService"

/** Cuánto suena la alarma antes de auto-silenciarse. */
private const val RING_MS = 30_000L

/**
 * Servicio en primer plano que hace sonar la alarma de fin de fase.
 *
 * ¿Por qué un foreground service? Cuando onReceive() termina, el proceso vuelve
 * a estado "cached" y el sistema lo puede matar en cualquier momento. Un FGS es
 * la única forma soportada de mantener audio vivo desde background.
 *
 * ¿Por qué MediaSessionService + ExoPlayer y no un Service + MediaPlayer común?
 * En Android 17 (API 37), el "background audio hardening" silencia en secreto
 * la reproducción de audio de apps que no tienen capacidades while-in-use (WIU),
 * incluso con el permiso de alarma exacta y AudioAttributes.USAGE_ALARM — la
 * exención que la documentación promete no se estaba honrando en pruebas reales
 * (confirmado vía `adb shell dumpsys` mostrando "AudioHardening background
 * playback muted ... level: full"). La documentación oficial recomienda migrar
 * a la MediaSessionService de la librería media3 precisamente para este caso:
 * https://developer.android.com/about/versions/17/changes/bg-audio
 *
 * OJO: la documentación dice que la app "no es probable que se vea afectada"
 * con este cambio, no que esté garantizado al 100%. Hay que volver a correr el
 * Test 1 (timer de 2 minutos, pantalla bloqueada) después de este cambio para
 * confirmarlo con evidencia real, igual que con cada fix anterior.
 */
class PomodoroAlarmService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onCreate() {
        super.onCreate()
        player = buildPlayer()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    /** Requerido por MediaSessionService. No exponemos esta sesión a controllers
     *  externos (Android Auto, Bluetooth, Asistente): sólo la usamos para que el
     *  sistema reconozca este FGS como reproducción de media legítima. */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Intencionalmente vacío: NO llamar a super.
     *
     * media3 trae adentro su propio gestor de notificaciones. En cuanto el
     * player arranca, construye una notificación de estilo media ("Now playing",
     * canal `default_channel_id` con IMPORTANCE_LOW) y llama a startForeground()
     * con SU id (1001 por defecto, el de DefaultMediaNotificationProvider).
     *
     * El problema es que un servicio tiene una sola notificación de primer
     * plano: cuando se llama a startForeground() con un id distinto del que ya
     * tenía, el framework CANCELA la anterior antes de poner la nueva
     * (ActiveServices.setServiceForegroundInnerLocked). Es decir, media3 se
     * llevaba puesta la notificación de alarma unos milisegundos después de
     * publicarla — el heads-up desaparecía y en el shade quedaba sólo una
     * tarjeta de reproducción en un canal LOW.
     *
     * Neutralizando este callback, la notificación del servicio queda bajo
     * nuestro control exclusivo (la publicamos en onStartCommand). No rompe
     * nada del contrato del FGS: ya llamamos a startForeground() por nuestra
     * cuenta con el tipo mediaPlayback.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // no-op a propósito
    }

    private fun buildPlayer(): ExoPlayer {
        val attrs = Media3AudioAttributes.Builder()
            .setUsage(C.USAGE_ALARM)
            .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
            .build()

        return ExoPlayer.Builder(this).build().apply {
            // handleAudioFocus=false a propósito: si ExoPlayer pidiera audio focus
            // y el sistema se lo negara (lo que puede pasar bajo el mismo
            // hardening), su comportamiento por defecto es pausarse — exactamente
            // lo opuesto de lo que necesitamos de una alarma.
            setAudioAttributes(attrs, /* handleAudioFocus = */ false)
            setWakeMode(C.WAKE_MODE_LOCAL)
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        Log.d(TAG, "ExoPlayer listo, reproducción en curso")
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        if (intent?.action == ACTION_STOP_RINGING) {
            stopRinging()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EX_RING_TITLE)
            ?: getString(R.string.tool_pomodoro_timer)
        val text = intent?.getStringExtra(EX_RING_TEXT)
            ?: getString(R.string.pomodoro_tap_to_stop)
        val route = intent?.getStringExtra(EX_RING_ROUTE)

        // 1) Marcar la alarma como activa ANTES de publicar la notificación.
        //    PomodoroAlarmActivity.onStart() hace `if (!AlarmState.isActive) finish()`,
        //    y el full-screen intent puede lanzarla en cuanto la notificación se
        //    publica. Si el flag se escribiera después, la pantalla de alarma
        //    podría abrirse y cerrarse sola.
        AlarmState.setActive(this, true)

        // 2) Pasar a primer plano. Hay ~10 s de margen antes de que el sistema
        //    mate el proceso por no llamar a startForeground().
        ensurePomodoroChannels(this)
        val notif = buildAlarmNotification(this, title, text, route)
        try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID_ALARM_SILENT,
                notif,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                else 0
            )
            Log.d(TAG, "startForeground OK")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground FALLÓ, el servicio va a ser matado por el sistema", e)
        }

        // 3) Avisarle a la UI que la alarma está sonando
        sendBroadcast(
            Intent(ACTION_POMODORO_ALARM_START)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
        )

        // 4) Sonido + vibración
        acquireWakeLock()
        startAudio()
        startVibration()

        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, RING_MS)

        return START_NOT_STICKY
    }

    // No overrideamos onTaskRemoved(): el default de Service no hace nada, y
    // queremos que sea así — si el usuario desliza la app fuera de "Recientes"
    // mientras suena, la alarma tiene que seguir sonando igual.

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniToolbox:PomodoroRing")
            ?.apply {
                setReferenceCounted(false)
                try {
                    acquire(RING_MS + 5_000L)
                    Log.d(TAG, "WakeLock del servicio adquirido")
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo adquirir el WakeLock del servicio", e)
                }
            }
        if (wakeLock == null) {
            Log.e(TAG, "No se pudo obtener PowerManager para el WakeLock del servicio")
        }
    }

    /**
     * Loguea el estado real del audio del sistema justo antes de intentar sonar.
     * Sigue siendo válido con ExoPlayer: el hardening opera a nivel de
     * AudioFlinger/política de audio, no en el objeto que reproduce.
     */
    private fun logAudioDiagnostics() {
        try {
            val am = getSystemService(AudioManager::class.java)
            val vol = am?.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val ringerMode = when (am?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                else -> "desconocido"
            }
            val nm = getSystemService(NotificationManager::class.java)
            val filter = when (nm?.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_ALL -> "ALL (sin restricciones)"
                NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "PRIORITY"
                NotificationManager.INTERRUPTION_FILTER_NONE -> "NONE (silencio total, esto bloquea la alarma)"
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> "ALARMS (sólo alarmas, debería sonar igual)"
                else -> "desconocido"
            }
            Log.d(
                TAG,
                "Diagnóstico audio: volumenAlarma=$vol/$maxVol ringerMode=$ringerMode dndFilter=$filter"
            )
            if (vol == 0) {
                Log.w(TAG, "¡El volumen de STREAM_ALARM está en 0! El audio va a reproducirse en silencio.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer el diagnóstico de audio", e)
        }
    }

    private fun startAudio() {
        val p = player ?: buildPlayer().also { player = it }
        logAudioDiagnostics()

        // getDefaultUri() devuelve "content://settings/system/alarm_alert": un
        // puntero simbólico al sonido configurado, no un archivo en sí. El
        // MediaPlayer viejo sabía resolverlo por dentro; el ContentDataSource
        // genérico de ExoPlayer no, y el sistema le niega el acceso directo
        // ("Direct file access no longer supported"). getActualDefaultRingtoneUri()
        // hace esa resolución nosotros mismos, ANTES de tocar ExoPlayer, y entrega
        // la URI real del archivo de audio.
        val uri: Uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION)
            ?: run {
                Log.w(TAG, "startAudio: no hay URI de sonido de alarma ni de notificación en el sistema")
                return
            }
        Log.d(TAG, "startAudio: uri=$uri")

        try {
            p.stop()
            p.clearMediaItems()
            p.setMediaItem(MediaItem.fromUri(uri))
            p.prepare()
            p.play()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo reproducir la alarma", e)
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
        val attrs = PlatformAudioAttributes.Builder()
            .setUsage(PlatformAudioAttributes.USAGE_ALARM)
            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            v.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
        } catch (_: Exception) { }
    }

    private fun stopRinging() {
        Log.d(TAG, "stopRinging")
        handler.removeCallbacks(autoStop)
        try { player?.stop() } catch (_: Exception) { }
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
        Log.d(TAG, "onDestroy")
        handler.removeCallbacks(autoStop)
        try { vibrator()?.cancel() } catch (_: Exception) { }
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) { }
        wakeLock = null

        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null

        super.onDestroy()
    }

    companion object {
        fun ring(context: Context, title: String, text: String, startRoute: String?) {
            Log.d(TAG, "ring(): pidiendo arranque del FGS")
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
            } catch (e: Exception) {
                Log.d(TAG, "stop(): el servicio ya no estaba corriendo (${e.javaClass.simpleName})")
            }
        }
    }
}