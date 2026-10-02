package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

val Context.aguaDataStore by preferencesDataStore(name = "agua")

private val KEY_OBJETIVO = intPreferencesKey("agua_objetivo_ml")
private val KEY_POR_VASO = intPreferencesKey("agua_ml_por_vaso")
private val KEY_NOTIF_ACTIVAS = intPreferencesKey("agua_notif_activas")
private val KEY_FRECUENCIA_MIN = intPreferencesKey("agua_notif_frecuencia_min")

fun keyFecha(fecha: LocalDate): Preferences.Key<Int> =
    intPreferencesKey("agua_ml_$fecha")

/**
 * Emite la fecha local actual y emite reactivamente la nueva fecha en cuanto el reloj
 * cruza la medianoche (00:00:00).
 */
fun flujoFechaActual(): Flow<LocalDate> = flow {
    var currentDate = LocalDate.now()
    emit(currentDate)
    while (true) {
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val delayMs = Duration.between(now, midnight).toMillis().coerceAtLeast(200L) + 50L
        delay(delayMs.milliseconds)
        val newDate = LocalDate.now()
        if (newDate != currentDate) {
            currentDate = newDate
            emit(currentDate)
        }
    }
}

fun Context.flujoAguaFecha(fecha: LocalDate): Flow<Int> =
    aguaDataStore.data.map { it[keyFecha(fecha)] ?: 0 }

/**
 * Flujo reactivo del consumo de agua del día actual.
 * Responde tanto a cambios en DataStore como al rollover de medianoche a través de [dateFlow],
 * conmutando automáticamente al nuevo día con 0 sin arrastrar el día anterior.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Context.flujoAguaHoy(dateFlow: Flow<LocalDate> = flujoFechaActual()): Flow<Int> =
    dateFlow.flatMapLatest { fecha ->
        aguaDataStore.data.map { it[keyFecha(fecha)] ?: 0 }
    }

fun Context.flujoObjetivo(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_OBJETIVO] ?: 2000 }

fun Context.flujoPorVaso(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_POR_VASO] ?: 250 }

fun Context.flujoNotificacionesActivas(): Flow<Boolean> =
    aguaDataStore.data.map { it[KEY_NOTIF_ACTIVAS] == 1 }

fun Context.flujoFrecuenciaMinutos(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_FRECUENCIA_MIN] ?: 30 }

suspend fun Context.guardarAguaFecha(fecha: LocalDate, valor: Int) {
    aguaDataStore.edit { it[keyFecha(fecha)] = valor }
}

suspend fun Context.guardarAguaHoy(valor: Int) {
    guardarAguaFecha(LocalDate.now(), valor)
}

suspend fun Context.guardarObjetivo(valor: Int) {
    aguaDataStore.edit { it[KEY_OBJETIVO] = valor }
}

suspend fun Context.guardarPorVaso(valor: Int) {
    aguaDataStore.edit { it[KEY_POR_VASO] = valor }
}

suspend fun Context.guardarNotificacionesActivas(activo: Boolean) {
    aguaDataStore.edit { it[KEY_NOTIF_ACTIVAS] = if (activo) 1 else 0 }
}

suspend fun Context.guardarFrecuenciaMinutos(min: Int) {
    aguaDataStore.edit { it[KEY_FRECUENCIA_MIN] = min }
}
