package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AlarmSoundPlayer {
    private var player: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playing = AtomicBoolean(false)
    private var autoJob: Job? = null

    fun play(
        context: Context,
        durationMs: Long = 10_000L,
        onAutoStop: (() -> Unit)? = null
    ) {
        stop() // limpia cualquier reproducción previa
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(context.applicationContext, uri)
            isLooping = false
            prepare()
            start()
        }
        playing.set(true)

        // Auto-stop tras duración (sin cancelar la notificación aquí)
        autoJob = scope.launch {
            try {
                delay(durationMs)
            } finally {
                if (playing.get()) {
                    stop()
                    onAutoStop?.invoke()
                }
            }
        }
    }

    fun stop() {
        playing.set(false)
        autoJob?.cancel()
        autoJob = null
        player?.let {
            runCatching {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        }
        player = null
    }

    fun isPlaying(): Boolean = playing.get()
}
