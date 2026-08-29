package com.joasasso.minitoolbox.metrics

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
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

/**
 * Cooldown para deduplicar eventos de uso de herramientas (rebotes de navegación).
 * 5 segundos es suficiente para que un usuario entre y salga sin inflar la métrica,
 * pero permite registrar usos legítimos consecutivos en una sesión normal.
 */
const val TOOL_USAGE_METRIC_COOLDOWN_MS = 5_000L

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
        UploadScheduler.maybeSchedule(ctx, MetricsConfig.endpoint)
    }
}

// Helpers
/**
 * Factory para inyectar un repositorio mock en tests.
 * Se guarda una función en lugar de la instancia para evitar StaticFieldLeak.
 * Existe porque el proyecto todavía no tiene inyección de dependencias,
 * y debe eliminarse cuando se introduzca Hilt.
 */
@VisibleForTesting
internal var metricsRepoFactory: ((Context) -> AggregatesRepository)? = null
private fun Context.repo() =
    metricsRepoFactory?.invoke(applicationContext) ?: AggregatesRepository(applicationContext)

/**
 * Hook para interceptar el agendamiento de subida en tests.
 * Existe porque el proyecto todavía no tiene inyección de dependencias,
 * y debe eliminarse cuando se introduzca Hilt.
 */
@VisibleForTesting
internal var metricsTestScheduleHook: ((Context) -> Unit)? = null
private fun scheduleIfEnabled(ctx: Context) {
    val hook = metricsTestScheduleHook
    if (hook != null) {
        hook(ctx)
        return
    }
    if (!isMetricsEnabled(ctx)) return
    UploadScheduler.markDirty(ctx)
    UploadScheduler.maybeSchedule(ctx, MetricsConfig.endpoint)
}

private fun io(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { block() }
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
        ctx.repo().incrementDailyActive()
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
