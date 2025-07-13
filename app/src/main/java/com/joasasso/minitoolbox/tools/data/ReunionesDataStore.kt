package com.joasasso.minitoolbox.tools.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joasasso.minitoolbox.tools.data.ReunionesRepository.flujoReuniones
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class Reunion(
    val id: String, // UUID
    val nombre: String,
    val fecha: Long, // Epoch millis
    val integrantes: List<String>,
    val gastos: List<Gasto>
)

@Serializable
data class Gasto(
    val id: String, // UUID
    val descripcion: String,
    val monto: Double,
    val pagadoPor: List<String>,
    val consumidoPor: List<String>,
    val aportesIndividuales: Map<String, Double> = emptyMap(),
    val imagenComprobante: String? = null // Ruta local de la imagen (si se implementa)
)

private val Context.dataStore by preferencesDataStore(name = "reuniones_gastos")

private val REUNIONES_KEY = stringPreferencesKey("reuniones")

object GastosDataStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Obtener la lista de reuniones
    fun getReuniones(context: Context): Flow<List<Reunion>> =
        context.reunionesDataStore.data.map { prefs ->
            prefs[KEY_REUNIONES]?.let { json ->
                Json.decodeFromString<List<Reunion>>(json)
            } ?: emptyList()
        }

    suspend fun obtenerReunionPorId(context: Context, id: String): Reunion? {
        return flujoReuniones(context).firstOrNull()?.find { it.id == id }
    }


    // Guardar lista completa de reuniones
    private suspend fun setReuniones(context: Context, reuniones: List<Reunion>) {
        val encoded = json.encodeToString(reuniones)
        context.dataStore.edit { prefs -> prefs[REUNIONES_KEY] = encoded }
    }

    // Agregar o actualizar una reunión
    suspend fun guardarReunion(context: Context, reunion: Reunion) {
        val actuales = getReuniones(context).firstOrNull() ?: emptyList()
        val actualizadas = actuales.filter { it.id != reunion.id } + reunion
        setReuniones(context, actualizadas)
    }

    // Eliminar una reunión
    suspend fun eliminarReunion(context: Context, reunionId: String) {
        val actuales = getReuniones(context).firstOrNull() ?: emptyList()
        val filtradas = actuales.filterNot { it.id == reunionId }
        setReuniones(context, filtradas)
    }
}

val Context.reunionesDataStore by preferencesDataStore("reuniones")

private val KEY_REUNIONES = stringPreferencesKey("lista_reuniones")

object ReunionesRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun flujoReuniones(context: Context): Flow<List<Reunion>> =
        context.reunionesDataStore.data.map { prefs ->
            prefs[KEY_REUNIONES]?.let {
                try {
                    json.decodeFromString<List<Reunion>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun guardarReuniones(context: Context, reuniones: List<Reunion>) {
        val json = Json.encodeToString(reuniones)
        context.reunionesDataStore.edit { it[KEY_REUNIONES] = json }
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