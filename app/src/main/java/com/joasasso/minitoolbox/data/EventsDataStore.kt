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
data class EventoImportante(
    val id: String,                // UUID para identificar
    val nombre: String,
    val fecha: String              // fecha en formato ISO (yyyy-MM-dd)
)


private val Context.eventosDataStore by preferencesDataStore("eventos_importantes")

private val EVENTOS_KEY = stringPreferencesKey("lista_eventos_importantes")

object EventosDataStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun flujoEventos(context: Context): Flow<List<EventoImportante>> =
        context.eventosDataStore.data.map { prefs ->
            prefs[EVENTOS_KEY]?.let {
                try {
                    json.decodeFromString<List<EventoImportante>>(it)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }

    suspend fun guardarEventos(context: Context, eventos: List<EventoImportante>) {
        val jsonString = json.encodeToString(eventos)
        context.eventosDataStore.edit { prefs ->
            prefs[EVENTOS_KEY] = jsonString
        }
    }

    suspend fun agregarEvento(context: Context, evento: EventoImportante) {
        val actuales = flujoEventos(context).firstOrNull().orEmpty().toMutableList()
        actuales.add(evento)
        guardarEventos(context, actuales)
    }

    suspend fun actualizarEvento(context: Context, actualizado: EventoImportante) {
        val actuales = flujoEventos(context).firstOrNull().orEmpty()
        guardarEventos(context, actuales.map { if (it.id == actualizado.id) actualizado else it })
    }

    suspend fun eliminarEvento(context: Context, id: String) {
        val actuales = flujoEventos(context).firstOrNull().orEmpty()
        guardarEventos(context, actuales.filterNot { it.id == id })
    }
}
