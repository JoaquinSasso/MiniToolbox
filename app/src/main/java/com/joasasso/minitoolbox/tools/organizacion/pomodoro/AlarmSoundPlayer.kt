// AlarmSoundPlayer.kt
package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

object AlarmSoundPlayer {
    private var player: MediaPlayer? = null
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null
    private var appRef: WeakReference<Context>? = null

    fun play(context: Context, durationMs: Long = 10_000L, onAutoSilenced: (() -> Unit)? = null) {
        stop() // limpia cualquier anterior
        val app = context.applicationContext
        appRef = WeakReference(app)

        val uri: Uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        mp.isLooping = true // loop seguro: lo cortamos nosotros a los 10 s
        mp.setDataSource(app, uri)
        mp.setOnPreparedListener { it.start() }
        mp.setOnCompletionListener {
            // En caso de no loop (por cambios de OEM), asegura cleanup
            stopInternal(invokeCallback = true, cb = onAutoSilenced)
        }
        mp.prepareAsync()

        // Auto-silencio duro en 10s, independiente del receiver
        stopHandler = Handler(Looper.getMainLooper())
        stopRunnable = Runnable {
            stopInternal(invokeCallback = true, cb = onAutoSilenced)
        }
        stopHandler?.postDelayed(stopRunnable!!, durationMs)
    }

    fun stop() {
        stopInternal(invokeCallback = false, cb = null)
    }

    private fun stopInternal(invokeCallback: Boolean, cb: (() -> Unit)?) {
        stopHandler?.removeCallbacks(stopRunnable ?: return)
        stopHandler = null
        stopRunnable = null

        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) { }
            try { it.reset() } catch (_: Exception) { }
            try { it.release() } catch (_: Exception) { }
        }
        player = null

        if (invokeCallback) {
            try { cb?.invoke() } catch (_: Exception) { }
        }
    }
}
