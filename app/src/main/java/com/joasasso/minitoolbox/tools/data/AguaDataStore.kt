package com.joasasso.minitoolbox.tools.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.aguaDataStore by preferencesDataStore(name = "agua")

private val KEY_OBJETIVO = intPreferencesKey("agua_objetivo_ml")
private val KEY_POR_VASO = intPreferencesKey("agua_ml_por_vaso")
private val KEY_NOTIF_ACTIVAS = intPreferencesKey("agua_notif_activas")
private val KEY_FRECUENCIA_MIN = intPreferencesKey("agua_notif_frecuencia_min")

private fun keyHoy(): Preferences.Key<Int> =
    intPreferencesKey("agua_ml_${LocalDate.now()}")

fun Context.flujoAguaHoy(): Flow<Int> =
    aguaDataStore.data.map { it[keyHoy()] ?: 0 }

fun Context.flujoObjetivo(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_OBJETIVO] ?: 2000 }

fun Context.flujoPorVaso(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_POR_VASO] ?: 250 }

fun Context.flujoNotificacionesActivas(): Flow<Boolean> =
    aguaDataStore.data.map { it[KEY_NOTIF_ACTIVAS] == 1 }

fun Context.flujoFrecuenciaMinutos(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_FRECUENCIA_MIN] ?: 30 }

suspend fun Context.guardarAguaHoy(valor: Int) {
    aguaDataStore.edit { it[keyHoy()] = valor }
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
