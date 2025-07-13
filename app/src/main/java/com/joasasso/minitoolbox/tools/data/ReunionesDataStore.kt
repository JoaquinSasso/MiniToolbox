package com.joasasso.minitoolbox.tools.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class Reunion(
    val id: String,
    val nombre: String,
    val fecha: Long,
    val integrantes: List<Grupo>,
    val gastos: List<Gasto>
)

@Serializable
data class Grupo(
    val nombre: String,
    val cantidad: Int // cantidad de personas reales en el grupo
)

@Serializable
data class Gasto(
    val id: String,
    val descripcion: String,
    val consumidoPor: Map<String, Int>, // nombre del grupo -> cantidad de personas que consumieron
    val aportesIndividuales: Map<String, Double> = emptyMap(),
    val imagenComprobante: String? = null
)


private val Context.dataStore by preferencesDataStore(name = "reuniones_gastos")
private val REUNIONES_KEY = stringPreferencesKey("lista_reuniones")

object GastosDataStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getReuniones(context: Context): Flow<List<Reunion>> =
        context.reunionesDataStore.data.map { prefs ->
            prefs[REUNIONES_KEY]?.let {
                try {
                    json.decodeFromString<List<Reunion>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun obtenerReunionPorId(context: Context, id: String): Reunion? {
        return getReuniones(context).firstOrNull()?.find { it.id == id }
    }

    private suspend fun setReuniones(context: Context, reuniones: List<Reunion>) {
        val encoded = json.encodeToString(reuniones)
        context.dataStore.edit { prefs -> prefs[REUNIONES_KEY] = encoded }
    }

    suspend fun guardarReunion(context: Context, reunion: Reunion) {
        val actuales = getReuniones(context).firstOrNull() ?: emptyList()
        val actualizadas = actuales.filter { it.id != reunion.id } + reunion
        setReuniones(context, actualizadas)
    }

    suspend fun eliminarReunion(context: Context, reunionId: String) {
        val actuales = getReuniones(context).firstOrNull() ?: emptyList()
        val filtradas = actuales.filterNot { it.id == reunionId }
        setReuniones(context, filtradas)
    }
}


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
