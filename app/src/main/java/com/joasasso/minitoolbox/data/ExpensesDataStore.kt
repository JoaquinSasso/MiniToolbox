package com.joasasso.minitoolbox.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


import kotlin.math.roundToLong

@Serializable
data class Reunion(
    val id: String,
    val nombre: String,
    val fecha: Long,
    val integrantes: List<String>, // Simplificado: lista de nombres
    val gastos: List<Gasto>
) {
    fun totalEnCentavos(): Long = gastos.sumOf { it.totalEnCentavos() }
}

@Serializable
data class Gasto(
    val id: String,
    val descripcion: String,
    val consumidoPor: Map<String, Int>, // Se mantiene Map por compatibilidad, pero el Int siempre será 1 o 0
    val aportesIndividuales: Map<String, Double> = emptyMap(),
    val aportesCentavos: Map<String, Long> = emptyMap(),
    val imagenComprobante: String? = null
) {
    fun obtenerAportesCentavos(): Map<String, Long> {
        if (aportesCentavos.isNotEmpty()) return aportesCentavos
        return aportesIndividuales.mapValues { (it.value * 100.0).roundToLong() }
    }

    fun totalEnCentavos(): Long = obtenerAportesCentavos().values.sum()
    fun aporteEnCentavos(nombre: String): Long = obtenerAportesCentavos()[nombre] ?: 0L
}

private val REUNIONES_KEY = stringPreferencesKey("lista_reuniones")


val Context.reunionesDataStore by preferencesDataStore("reuniones")

object ReunionesRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun flujoReuniones(context: Context): Flow<List<Reunion>> =
        context.reunionesDataStore.data.map { prefs ->
            prefs[REUNIONES_KEY]?.let {
                try {
                    json.decodeFromString<List<Reunion>>(it)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun guardarReuniones(context: Context, reuniones: List<Reunion>) {
        val jsonString = json.encodeToString(reuniones)
        context.reunionesDataStore.edit { it[REUNIONES_KEY] = jsonString }
    }

    suspend fun agregarReunion(context: Context, reunion: Reunion) {
        val reuniones = flujoReuniones(context).firstOrNull().orEmpty().toMutableList()
        reuniones.add(reunion)
        guardarReuniones(context, reuniones)
    }

    suspend fun actualizarReunion(context: Context, actualizada: Reunion) {
        val actuales = flujoReuniones(context).firstOrNull() ?: emptyList()
        guardarReuniones(context, actuales.map { if (it.id == actualizada.id) actualizada else it })
    }

    suspend fun eliminarReunion(context: Context, id: String) {
        val actuales = flujoReuniones(context).firstOrNull() ?: emptyList()
        guardarReuniones(context, actuales.filterNot { it.id == id })
    }
}
