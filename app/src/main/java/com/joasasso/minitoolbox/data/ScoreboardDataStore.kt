import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class Equipo(
    val nombre: String,
    val puntos: Int,
    val color: Color
)


object MarcadorPrefs {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("marcador_prefs", Context.MODE_PRIVATE)

    fun loadAll(context: Context): List<Equipo> {
        val raw = prefs(context).getString("equipos", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Equipo(
                nombre = o.getString("nombre"),
                puntos = o.getInt("puntos"),
                color = Color(o.getInt("color"))
            )
        }
    }

    fun saveAll(context: Context, list: List<Equipo>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().apply {
                    put("nombre", it.nombre)
                    put("puntos", it.puntos)
                    put("color", it.color.toArgb())
                }
            )
        }
        prefs(context).edit { putString("equipos", arr.toString()) }
    }
}
