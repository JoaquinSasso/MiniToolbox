package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.aguaDataStore by preferencesDataStore(name = "agua")

internal val KEY_OBJETIVO = intPreferencesKey("agua_objetivo_ml")
internal val KEY_POR_VASO = intPreferencesKey("agua_ml_por_vaso")
internal val KEY_NOTIF_ACTIVAS = intPreferencesKey("agua_notif_activas")
internal val KEY_FRECUENCIA_MIN = intPreferencesKey("agua_notif_frecuencia_min")

fun keyFecha(fecha: LocalDate): Preferences.Key<Int> =
    intPreferencesKey("agua_ml_$fecha")

/**
 * Emite la fecha local actual.
 */
fun flujoFechaActual(): Flow<LocalDate> = flow {
    emit(LocalDate.now())
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

/**
 * Incrementa (o decrementa si [delta] es negativo) de forma atómica el consumo de [fecha]
 * dentro de una única transacción de DataStore.
 * Devuelve el par `(valorAnterior, valorNuevo)`.
 */
suspend fun Context.incrementarAguaFecha(fecha: LocalDate, delta: Int): Pair<Int, Int> {
    var anterior = 0
    var nuevo = 0
    aguaDataStore.edit { prefs ->
        val key = keyFecha(fecha)
        anterior = prefs[key] ?: 0
        nuevo = (anterior + delta).coerceAtLeast(0)
        if (nuevo != anterior || prefs.contains(key)) {
            prefs[key] = nuevo
        }
    }
    return anterior to nuevo
}

suspend fun Context.incrementarAguaHoy(delta: Int): Pair<Int, Int> =
    incrementarAguaFecha(LocalDate.now(), delta)

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
