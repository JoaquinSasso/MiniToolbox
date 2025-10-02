package com.joasasso.minitoolbox.metrics

import android.content.Context
import androidx.core.content.edit
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.uploader.UploadConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -------------------------
// Opt-in / Opt-out (default ON)
// -------------------------
private const val METRICS_SP = "metrics_prefs"
private const val KEY_ENABLED = "enabled"

fun isMetricsEnabled(context: Context): Boolean {
    val sp = context.applicationContext.getSharedPreferences(METRICS_SP, Context.MODE_PRIVATE)
    return sp.getBoolean(KEY_ENABLED, true) // ON por defecto
}

/**
 * Cambia el estado global de métricas.
 * Si se habilita, agenda un envío oportunista (por si había datos acumulados).
 * Si se deshabilita, no borra datos locales (compliance mínima) pero deja de registrar/subir.
 */
fun setMetricsEnabled(context: Context, enabled: Boolean) {
    val ctx = context.applicationContext
    val sp = ctx.getSharedPreferences(METRICS_SP, Context.MODE_PRIVATE)
    sp.edit { putBoolean(KEY_ENABLED, enabled) }
    if (enabled) {
        // Si vuelven a habilitar, intentamos enviar lo acumulado
        UploadScheduler.markDirty(ctx)
        UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
    }
}

// Helpers
private fun Context.repo() = AggregatesRepository(applicationContext)
private fun io(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { block() }
}
private fun scheduleIfEnabled(ctx: Context) {
    if (!isMetricsEnabled(ctx)) return
    UploadScheduler.markDirty(ctx)
    UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
}

/** Suma app open y agenda upload oportunista (respeta opt-out) */
fun appOpen(context: Context) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    ctx.repo().incrementAppOpen()
    scheduleIfEnabled(ctx)
}

/** Marca 1 vez por día (simple) + agenda upload (respeta opt-out) */
fun dailyOpenOnce(context: Context) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    val sp = ctx.getSharedPreferences("metrics_daily_once", Context.MODE_PRIVATE)
    val last = sp.getString("last_day", null)
    if (last != today) {
        ctx.repo().incrementAppOpen()
        sp.edit { putString("last_day", today) }
        scheduleIfEnabled(ctx)
    }
}

/** Uso de tool + agenda upload (respeta opt-out) */
fun toolUse(context: Context, toolId: String) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    ctx.repo().incrementToolUse(toolId)
    scheduleIfEnabled(ctx)
}

/** Impresión de anuncio + agenda upload (respeta opt-out) */
fun adImpression(context: Context, type: String) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    ctx.repo().incrementAdImpression(type)
    scheduleIfEnabled(ctx)
}

/** NUEVO: Uso de widget + agenda upload (respeta opt-out) */
fun widgetUse(context: Context, widgetType: String) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    ctx.repo().incrementWidgetUse(widgetType)
    scheduleIfEnabled(ctx)
}

/**
 * NUEVO: Heartbeat diario de versión/idiomas sin contar "app open".
 * Útil para procesos que no abren la app (p.ej., widgets/foreground services).
 * No agenda upload por sí mismo (evita tráfico innecesario), pero puedes activarlo si quieres.
 */
fun versionHeartbeat(context: Context) = io {
    val ctx = context.applicationContext
    if (!isMetricsEnabled(ctx)) return@io
    // Requiere agregar el wrapper público en AggregatesRepository (ver sección 2)
    ctx.repo().dailyVersionAndLangHeartbeat()
    // Opcional: descomenta si deseas forzar envío oportunista:
    // scheduleIfEnabled(ctx)
}
