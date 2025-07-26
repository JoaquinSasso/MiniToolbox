import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class RachaActividad(
    val emoji: String,
    val nombre: String,
    val inicio: String // fecha de inicio
)

object RachaPrefs {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("racha_prefs", Context.MODE_PRIVATE)


    fun loadAll(context: Context): List<RachaActividad> {

        val raw = prefs(context).getString("rachas", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RachaActividad(
                emoji = o.getString("emoji"),
                nombre = o.getString("nombre"),
                inicio = o.getString("inicio")
            )
        }
    }

    fun saveAll(context: Context, list: List<RachaActividad>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().apply {
                    put("emoji", it.emoji)
                    put("nombre", it.nombre)
                    put("inicio", it.inicio)
                }
            )
        }
        prefs(context).edit { putString("rachas", arr.toString()) }
    }
}