package com.joasasso.minitoolbox.tools.herramientas.generadores.noiseGenerator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

enum class NoiseType { White, Pink, Brown }

object NoiseEngine {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048
    private var track: AudioTrack? = null
    private var job: Job? = null
    private var volumeLinear = 0.5f
    private var currentType = NoiseType.White

    // Estados para filtros
    private var pink_b0 = 0f; private var pink_b1 = 0f; private var pink_b2 = 0f
    private var brown_prev = 0f

    fun isRunning(): Boolean = job?.isActive == true

    fun setVolume(vol: Float) { volumeLinear = vol.coerceIn(0f, 1f) }

    fun start(type: NoiseType) {
        if (isRunning()) return
        currentType = type

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuf, BUFFER_SIZE * 2)

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()

        track?.play()

        // Reset filtros
        pink_b0 = 0f; pink_b1 = 0f; pink_b2 = 0f; brown_prev = 0f

        job = CoroutineScope(Dispatchers.Default).launch {
            val buf = ShortArray(BUFFER_SIZE)
            while (isActive) {
                for (i in buf.indices) {
                    val s = when (currentType) {
                        NoiseType.White -> white()
                        NoiseType.Pink  -> pink()
                        NoiseType.Brown -> brown()
                    } * volumeLinear
                    // Convertir a PCM 16-bit
                    buf[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track?.write(buf, 0, buf.size, AudioTrack.WRITE_BLOCKING)
            }
        }
    }

    fun changeType(type: NoiseType) { currentType = type }

    suspend fun stop(fadeOutMillis: Long = 0L) {
        if (!isRunning()) return
        if (fadeOutMillis > 0) {
            val steps = 30
            val stepDur = (fadeOutMillis / steps).coerceAtLeast(10)
            val startVol = volumeLinear
            for (k in steps downTo 1) {
                setVolume(startVol * (k / steps.toFloat()))
                delay(stepDur)
            }
        }
        job?.cancelAndJoin()
        job = null
        track?.stop()
        track?.release()
        track = null
    }

    // --------- Generadores ----------
    private fun white(): Float {
        // Uniforme en [-1, 1]
        return (Random.nextFloat() * 2f - 1f)
    }

    // Filtro IIR "Paul Kellet" para 1/f (pink) – aproximación práctica
    private fun pink(): Float {
        val w = white()
        pink_b0 = 0.99765f * pink_b0 + w * 0.0990460f
        pink_b1 = 0.96300f * pink_b1 + w * 0.2965164f
        pink_b2 = 0.57000f * pink_b2 + w * 1.0526913f
        var pink = pink_b0 + pink_b1 + pink_b2 + w * 0.1848f
        // Normaliza un poco la ganancia
        pink *= 0.05f
        return pink
    }

    // "Brown/Red" integrando white de forma amortiguada
    private fun brown(): Float {
        val w = white()
        val b = (brown_prev + w) / 1.02f
        brown_prev = b
        return (b * 3.5f).coerceIn(-1f, 1f)
    }
}
