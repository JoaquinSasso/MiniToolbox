package com.example.minitoolbox.tools.recordatorios.agua

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// 1. Extensión para obtener el DataStore
val Context.aguaDataStore by preferencesDataStore(name = "agua")

// 2. Keys principales
private val KEY_OBJETIVO = intPreferencesKey("agua_objetivo_ml")
private val KEY_POR_VASO = intPreferencesKey("agua_ml_por_vaso")
private fun keyHoy(): Preferences.Key<Int> =
    intPreferencesKey("agua_ml_${LocalDate.now()}")

// 3. Flujos
fun Context.flujoAguaHoy(): Flow<Int> =
    aguaDataStore.data.map { it[keyHoy()] ?: 0 }

fun Context.flujoObjetivo(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_OBJETIVO] ?: 2000 }

fun Context.flujoPorVaso(): Flow<Int> =
    aguaDataStore.data.map { it[KEY_POR_VASO] ?: 250 }

// 4. Setters
suspend fun Context.guardarAguaHoy(valor: Int) {
    aguaDataStore.edit { it[keyHoy()] = valor }
}
suspend fun Context.guardarObjetivo(valor: Int) {
    aguaDataStore.edit { it[KEY_OBJETIVO] = valor }
}
suspend fun Context.guardarPorVaso(valor: Int) {
    aguaDataStore.edit { it[KEY_POR_VASO] = valor }
}
