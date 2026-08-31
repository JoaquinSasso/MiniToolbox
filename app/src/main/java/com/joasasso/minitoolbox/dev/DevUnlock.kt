package com.joasasso.minitoolbox.dev

import android.content.Context
import androidx.core.content.edit

/**
 * Desbloqueo del acceso a las herramientas de diagnóstico en builds de release,
 * al estilo de las opciones de desarrollador de Android: varios toques sobre el
 * número de versión.
 *
 * Motivo: cuando un dispositivo deja de enviar métricas no hay forma de saber por qué
 * sin acceso al estado interno, y hasta ahora ese estado sólo era visible en debug. Sin
 * esto, la única alternativa es pedirle al usuario que instale una build especial.
 *
 * El acceso desbloqueado es de **sólo lectura más acciones seguras**. Las acciones
 * destructivas siguen restringidas a `BuildConfig.DEBUG`.
 */
object DevUnlock {

    private const val PREFS_NAME = "dev_prefs"
    private const val KEY_UNLOCKED = "dev_unlocked"

    /** Toques necesarios sobre la versión para desbloquear. */
    const val TAPS_REQUIRED = 7

    /** A partir de este toque se empieza a avisar cuántos faltan. */
    const val TAPS_BEFORE_HINT = 4

    /** Tiempo máximo entre toques para que cuenten como una secuencia. */
    const val TAP_WINDOW_MS = 3_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUnlocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_UNLOCKED, false)

    fun setUnlocked(context: Context, unlocked: Boolean) {
        prefs(context).edit { putBoolean(KEY_UNLOCKED, unlocked) }
    }

    /**
     * Estado de la secuencia de toques. Se mantiene fuera de la UI para poder testearlo
     * sin Compose.
     */
    class TapCounter(
        private val tapsRequired: Int = TAPS_REQUIRED,
        private val windowMs: Long = TAP_WINDOW_MS
    ) {
        private var count = 0
        private var lastTapMs = 0L

        /** Resultado de registrar un toque. */
        sealed interface Result {
            /** Todavía faltan toques y no corresponde avisar nada. */
            data object Ignored : Result

            /** Faltan [remaining] toques; conviene avisarle al usuario. */
            data class Hint(val remaining: Int) : Result

            /** Se completó la secuencia. */
            data object Unlocked : Result
        }

        fun onTap(nowMs: Long): Result {
            count = if (nowMs - lastTapMs > windowMs) 1 else count + 1
            lastTapMs = nowMs

            return when {
                count >= tapsRequired -> {
                    count = 0
                    Result.Unlocked
                }
                count >= TAPS_BEFORE_HINT -> Result.Hint(tapsRequired - count)
                else -> Result.Ignored
            }
        }

        fun reset() {
            count = 0
            lastTapMs = 0L
        }
    }
}