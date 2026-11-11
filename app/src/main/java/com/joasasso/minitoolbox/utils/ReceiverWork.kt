package com.joasasso.minitoolbox.utils

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Trabajo asíncrono lanzado desde un `BroadcastReceiver` (normalmente tras `goAsync()`).
 *
 * Un `try/finally` sin `catch` en un scope sin manejador deja escapar la excepción al
 * handler del hilo y mata el proceso. Acá se loguea, se re-lanza solo
 * `CancellationException` y [onDone] (típicamente `PendingResult.finish()`) corre siempre.
 *
 * Crea su propio scope porque `appScope` todavía es privado (backlog: `app-scope`).
 */
internal fun launchReceiverWork(
    tag: String,
    onDone: () -> Unit = {},
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> Unit
): Job = CoroutineScope(dispatcher + SupervisorJob()).launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(tag, "Error en trabajo asíncrono del receiver", e)
    } finally {
        onDone()
    }
}
