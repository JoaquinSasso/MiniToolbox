package com.joasasso.minitoolbox.metrics

import android.content.Context
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Helpers ----------
private fun Context.repo() = AggregatesRepository(applicationContext)

private fun io(block: suspend () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch { block() }
}

// ---------- API pública ----------

// Suma 1 cada vez que se abre la app (cuenta “aperturas” reales)
fun appOpen(context: Context) = io {
    context.repo().incrementAppOpen()
}

// Marca una “apertura diaria” (como DAU): incrementa una sola vez por día natural
fun dailyOpenOnce(context: Context) = io {
    val ds = context.repo()
    // Implementación simple: si querés evitar doble conteo en el mismo día,
    // podés llevar un último día marcado en otro key. Si no tenés esa clave,
    // esta versión puede delegar a incrementAppOpen() pero sólo 1 vez por día.
    // Te dejo una mini lógica in-archivo (sin tocar el repo):
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = dayFmt.format(Date())

    // Usamos una preferencia liviana guardada por esta función
    val prefs = context.getSharedPreferences("metrics_daily_once", Context.MODE_PRIVATE)
    val last = prefs.getString("last_day", null)
    if (last != today) {
        context.repo().incrementAppOpen() // cuenta 1 para el día
        prefs.edit().putString("last_day", today).apply()
    }
}

// Uso de tool por visita (agregado por día y toolId)
fun toolUse(context: Context, toolId: String) = io {
    context.repo().incrementToolUse(toolId)
}

// Impresión de anuncios por tipo (banner/interstitial/rewarded)
fun adImpression(context: Context, type: String) = io {
    context.repo().incrementAdImpression(type)
}
