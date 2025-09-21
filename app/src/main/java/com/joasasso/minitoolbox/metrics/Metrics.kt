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

// Helpers
private fun Context.repo() = AggregatesRepository(applicationContext)
private fun io(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { block() }
}

/** Suma app open y agenda upload oportunista */
fun appOpen(context: Context) = io {
    val ctx = context.applicationContext
    ctx.repo().incrementAppOpen()
    UploadScheduler.markDirty(ctx)
    UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
}

/** Marca 1 vez por día (simple) + agenda upload */
fun dailyOpenOnce(context: Context) = io {
    val ctx = context.applicationContext
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = fmt.format(Date())
    val sp = ctx.getSharedPreferences("metrics_daily_once", Context.MODE_PRIVATE)
    val last = sp.getString("last_day", null)
    if (last != today) {
        ctx.repo().incrementAppOpen()
        sp.edit { putString("last_day", today) }
        UploadScheduler.markDirty(ctx)
        UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
    }
}

/** Uso de tool + agenda upload */
fun toolUse(context: Context, toolId: String) = io {
    val ctx = context.applicationContext
    ctx.repo().incrementToolUse(toolId)
    UploadScheduler.markDirty(ctx)
    UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
}

/** Impresión de anuncio + agenda upload */
fun adImpression(context: Context, type: String) = io {
    val ctx = context.applicationContext
    ctx.repo().incrementAdImpression(type)
    UploadScheduler.markDirty(ctx)
    UploadScheduler.maybeSchedule(ctx, UploadConfig.getEndpoint(ctx), UploadConfig.getApiKey(ctx))
}
