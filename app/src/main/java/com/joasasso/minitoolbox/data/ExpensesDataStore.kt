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

    private fun deserializar(raw: String?): List<Reunion> {
        if (raw == null) return emptyList()
        return try {
            json.decodeFromString<List<Reunion>>(raw)
        } catch (e: SerializationException) {
            Log.w("ReunionesRepo", "JSON de reuniones corrupto", e)
            emptyList()
        } catch (e: IllegalArgumentException) {
            Log.w("ReunionesRepo", "Error de argumento al deserializar reuniones", e)
            emptyList()
        }
    }

    fun flujoReuniones(context: Context): Flow<List<Reunion>> =
        context.reunionesDataStore.data.map { prefs ->
            deserializar(prefs[REUNIONES_KEY])
        }

    suspend fun guardarReuniones(context: Context, reuniones: List<Reunion>) {
        val jsonString = json.encodeToString(reuniones)
        context.reunionesDataStore.edit { it[REUNIONES_KEY] = jsonString }
    }

    suspend fun agregarReunion(context: Context, reunion: Reunion) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY]).toMutableList()
            actuales.add(reunion)
            prefs[REUNIONES_KEY] = json.encodeToString(actuales)
        }
    }

    suspend fun actualizarReunion(context: Context, actualizada: Reunion) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { if (it.id == actualizada.id) actualizada else it }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun eliminarReunion(context: Context, id: String) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.filterNot { it.id == id }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun guardarGasto(context: Context, reunionId: String, gasto: Gasto) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { reunion ->
                if (reunion.id == reunionId) {
                    val gastosActualizados = if (reunion.gastos.any { it.id == gasto.id }) {
                        reunion.gastos.map { if (it.id == gasto.id) gasto else it }
                    } else {
                        reunion.gastos + gasto
                    }
                    reunion.copy(gastos = gastosActualizados)
                } else {
                    reunion
                }
            }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun eliminarGasto(context: Context, reunionId: String, gastoId: String) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { reunion ->
                if (reunion.id == reunionId) {
                    reunion.copy(gastos = reunion.gastos.filterNot { it.id == gastoId })
                } else {
                    reunion
                }
            }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun actualizarIntegrante(context: Context, reunionId: String, original: String, nuevo: String) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { reunion ->
                if (reunion.id == reunionId) {
                    val nuevosIntegrantes = reunion.integrantes.map { if (it == original) nuevo else it }
                    val nuevosGastos = reunion.gastos.map { g ->
                        val nuevosAportesInd = g.aportesIndividuales.mapKeys { if (it.key == original) nuevo else it.key }
                        val nuevosAportesCent = g.aportesCentavos.mapKeys { if (it.key == original) nuevo else it.key }
                        val nuevosConsumidores = g.consumidoPor.mapKeys { if (it.key == original) nuevo else it.key }
                        g.copy(
                            aportesIndividuales = nuevosAportesInd,
                            aportesCentavos = nuevosAportesCent,
                            consumidoPor = nuevosConsumidores
                        )
                    }
                    reunion.copy(integrantes = nuevosIntegrantes, gastos = nuevosGastos)
                } else {
                    reunion
                }
            }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun eliminarIntegrante(context: Context, reunionId: String, nombre: String) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { reunion ->
                if (reunion.id == reunionId) {
                    val nuevosIntegrantes = reunion.integrantes.filterNot { it == nombre }
                    val nuevosGastos = reunion.gastos.map { g ->
                        val nuevosAportesInd = g.aportesIndividuales.filterKeys { it != nombre }
                        val nuevosAportesCent = g.aportesCentavos.filterKeys { it != nombre }
                        val nuevosConsumidores = g.consumidoPor.filterKeys { it != nombre }
                        g.copy(
                            aportesIndividuales = nuevosAportesInd,
                            aportesCentavos = nuevosAportesCent,
                            consumidoPor = nuevosConsumidores
                        )
                    }
                    reunion.copy(integrantes = nuevosIntegrantes, gastos = nuevosGastos)
                } else {
                    reunion
                }
            }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }

    suspend fun agregarIntegrante(context: Context, reunionId: String, nuevoNombre: String) {
        context.reunionesDataStore.edit { prefs ->
            val actuales = deserializar(prefs[REUNIONES_KEY])
            val nuevas = actuales.map { reunion ->
                if (reunion.id == reunionId && !reunion.integrantes.contains(nuevoNombre)) {
                    val nuevosIntegrantes = reunion.integrantes + nuevoNombre
                    val nuevosGastos = reunion.gastos.map { gasto ->
                        val consumidoPor = gasto.consumidoPor.toMutableMap()
                        consumidoPor[nuevoNombre] = 1
                        gasto.copy(consumidoPor = consumidoPor)
                    }
                    reunion.copy(integrantes = nuevosIntegrantes, gastos = nuevosGastos)
                } else {
                    reunion
                }
            }
            prefs[REUNIONES_KEY] = json.encodeToString(nuevas)
        }
    }
}
