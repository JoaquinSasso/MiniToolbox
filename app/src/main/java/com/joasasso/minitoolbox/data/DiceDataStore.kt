package com.joasasso.minitoolbox.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class TiradaDeDados(
    val tipo: Int,              // ej: 6 para D6
    val cantidad: Int,          // cantidad de dados lanzados
    val resultados: List<Int>,  // resultados individuales
    val timestamp: Long         // fecha y hora
)


private val Context.dadosDataStore by preferencesDataStore(name = "historial_dados")
private val HISTORIAL_KEY = stringPreferencesKey("historial_tiradas")

object DadosHistorialRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun flujoHistorial(context: Context): Flow<List<TiradaDeDados>> =
        context.dadosDataStore.data.map { prefs ->
            prefs[HISTORIAL_KEY]?.let {
                try {
                    json.decodeFromString<List<TiradaDeDados>>(it)
                } catch (e: SerializationException) {
                    Log.w("DadosHistorialRepo", "Historial de dados corrupto", e)
                    emptyList()
                } catch (e: IllegalArgumentException) {
                    Log.w("DadosHistorialRepo", "Error de argumento al deserializar dados", e)
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun guardarTirada(context: Context, tirada: TiradaDeDados) {
        val historial = flujoHistorial(context).firstOrNull().orEmpty().toMutableList()

        historial.add(0, tirada) // agregar al principio
        if (historial.size > 30) {
            historial.subList(30, historial.size).clear() // eliminar el historial a partir del índice 30
        }

        val encoded = json.encodeToString(historial)
        context.dadosDataStore.edit { it[HISTORIAL_KEY] = encoded }
    }

}
