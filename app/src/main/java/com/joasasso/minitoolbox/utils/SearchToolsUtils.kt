package com.joasasso.minitoolbox.utils
import com.joasasso.minitoolbox.tools.Tool
import java.text.Normalizer
import java.util.Locale

/* ===================== Utilidades de búsqueda ===================== */

// Normaliza: quita acentos y pasa a minúsculas (para comparar sin diacríticos)
fun normalize(text: String): String {
    val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
    val noAccents = nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    return noAccents.lowercase(Locale.ROOT)
}

// Tokeniza por separadores no alfanuméricos
fun tokenizeNormalized(text: String): List<String> =
    normalize(text).split("[^a-z0-9]+".toRegex()).filter { it.isNotBlank() }

// Stopwords mínimas ES/EN para evitar que “o”, “de”, “the” condicionen la búsqueda
private val STOPWORDS_ES = setOf("de","del","la","el","los","las","un","una","unos","unas","y","o","u","para","por","en","con","al","a","que")
private val STOPWORDS_EN = setOf("the","a","an","and","or","to","for","in","on","of","with","at")

fun stopwordsFor(locale: Locale): Set<String> =
    when (locale.language.lowercase(Locale.ROOT)) {
        "es" -> STOPWORDS_ES
        "en" -> STOPWORDS_EN
        else -> emptySet()
    }

data class ToolSearchIndex(
    val route: String,
    val haystack: String,     // título + resumen normalizados
    val tokens: Set<String>   // tokens únicos (rápida verificación)
)

// Construye índice de TODAS las tools (global)
fun buildSearchIndexForAllTools(
    context: android.content.Context,
    allTools: List<Tool>
): List<ToolSearchIndex> {
    return allTools.map { tool ->
        // Título (obligatorio)
        val title = runCatching { context.getString(tool.name) }
            .getOrElse { tool.screen.route }

        // Resumen (puede ser null en tu modelo)
        val summary = tool.summary?.let { context.getString(it) } ?: ""

        // Categoría (usa el string de la categoría si existe)
        val categoryText = runCatching { context.getString(tool.category.titleRes) }
            .getOrElse { tool.category.toString().replace('_', ' ') }

        // Subcategoría (puede no tener titleRes según tu modelo)
        val subcategoryText = runCatching {
            // Si tu subCategoría tiene titleRes:
            val field = tool.subCategory
            // Intenta acceder a un posible titleRes; si no existe, lanza y cae al else
            val titleResField = field!!::class.members.firstOrNull { it.name == "titleRes" }
            val resId = titleResField?.call(field) as? Int
            if (resId != null) context.getString(resId) else throw IllegalStateException()
        }.getOrElse {
            // Fallback: nombre “humano” a partir del toString()/name
            tool.subCategory?.toString()?.replace('_', ' ') ?: ""
        }

        // Concatenamos todos para que la búsqueda considere
        // título + resumen + categoría + subcategoría
        val concat = listOf(title, summary, categoryText, subcategoryText)
            .joinToString(" ")
            .trim()

        val norm = normalize(concat)
        val toks = tokenizeNormalized(concat).toSet()

        ToolSearchIndex(
            route = tool.screen.route,
            haystack = norm,
            tokens = toks
        )
    }
}


// Devuelve true si TODAS las palabras “reales” (sin stopwords) de la query
// aparecen en el título+resumen (sin importar el orden). “cruz” o “cruz o cara” matchean “Cara o Cruz”.
fun matchesQuery(index: ToolSearchIndex, query: String, locale: Locale): Boolean {
    val rawTokens = tokenizeNormalized(query)
    val stops = stopwordsFor(locale)
    val qTokens = rawTokens.filterNot { it in stops }
    if (qTokens.isEmpty()) return true
    return qTokens.all { qt -> index.haystack.contains(qt) || index.tokens.contains(qt) }
}