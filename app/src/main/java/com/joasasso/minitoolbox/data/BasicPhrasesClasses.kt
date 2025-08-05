package com.joasasso.minitoolbox.data

import androidx.annotation.StringRes
import com.joasasso.minitoolbox.R
import java.util.Locale

data class Frase(
    val categoria: String,
    val traducciones: Map<String, String>
)


enum class Categoria(val id: String, @StringRes val nombreResId: Int) {
    GREETINGS("greetings", R.string.frases_categoria_saludos),
    QUESTIONS("questions", R.string.frases_categoria_preguntas),
    EMERGENCIES("emergencies", R.string.frases_categoria_emergencias),
    TRANSPORT("transport", R.string.frases_categoria_transporte),
    FOOD("food", R.string.frases_categoria_comida),
    OTHER("other", R.string.frases_categoria_otros)
}


data class Idioma(
    val codigo: String,                  // ISO 639-1 (ej: "en", "es", "fr", "it")
    val locale: Locale,
    @StringRes val nombreResId: Int
)

val idiomasDisponibles = listOf(
    Idioma("en", Locale("en"), R.string.frases_idioma_ingles),
    Idioma("es", Locale("es"), R.string.frases_idioma_espanol),
    Idioma("fr", Locale("fr"), R.string.frases_idioma_frances),
    Idioma("it", Locale("it"), R.string.frases_idioma_italiano),
    Idioma("de", Locale("de"), R.string.frases_idioma_aleman),
    Idioma("pt", Locale("pt"), R.string.frases_idioma_portugues),
    Idioma("ja", Locale("ja"), R.string.frases_idioma_japones),
    Idioma("zh", Locale("zh"), R.string.frases_idioma_chino),
    Idioma("ko", Locale("ko"), R.string.frases_idioma_coreano)
)