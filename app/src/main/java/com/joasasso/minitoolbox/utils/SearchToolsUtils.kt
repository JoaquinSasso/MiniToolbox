package com.joasasso.minitoolbox.utils

import android.content.Context
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.Tool
import java.text.Normalizer
import java.util.Locale

/* ===================== Utilidades de búsqueda y catálogo ===================== */

/**
 * Filtra las entradas técnicas del registro (como `About`, que vive en el TopAppBar)
 * para que no aparezcan como tarjetas de herramienta en el catálogo ni en la búsqueda.
 */
fun isCatalogTool(tool: Tool): Boolean = tool.screen != Screen.About

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
private val STOPWORDS_ES = setOf(
    "de", "del", "la", "el", "los", "las", "un", "una", "unos", "unas",
    "y", "o", "u", "para", "por", "en", "con", "al", "a", "que"
)
private val STOPWORDS_EN = setOf(
    "the", "a", "an", "and", "or", "to", "for", "in", "on", "of", "with", "at"
)

fun stopwordsFor(locale: Locale): Set<String> =
    when (locale.language.lowercase(Locale.ROOT)) {
        "es" -> STOPWORDS_ES
        "en" -> STOPWORDS_EN
        else -> emptySet()
    }

data class ToolSearchIndex(
    val route: String,
    val haystack: String,     // título + resumen + categoría + subcategoría normalizados
    val tokens: Set<String>   // tokens únicos (rápida verificación)
)

// Construye índice de todas las tools visibles del catálogo
fun buildSearchIndexForAllTools(
    context: Context,
    allTools: List<Tool>
): List<ToolSearchIndex> {
    return allTools
        .filter(::isCatalogTool)
        .map { tool ->
            val title = runCatching { context.getString(tool.name) }
                .getOrElse { tool.screen.route }

            val summary = tool.summary?.let {
                runCatching { context.getString(it) }.getOrDefault("")
            } ?: ""

            val categoryText = runCatching { context.getString(tool.category.titleRes) }
                .getOrDefault("")

            val subcategoryText = runCatching { context.getString(tool.subCategory) }
                .getOrDefault("")

            val concat = listOf(title, summary, categoryText, subcategoryText)
                .filter { it.isNotBlank() }
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
// aparecen en el índice (sin importar el orden).
fun matchesQuery(index: ToolSearchIndex, query: String, locale: Locale): Boolean {
    val rawTokens = tokenizeNormalized(query)
    val stops = stopwordsFor(locale)
    val qTokens = rawTokens.filterNot { it in stops }
    if (qTokens.isEmpty()) return true
    return qTokens.all { qt -> index.haystack.contains(qt) || index.tokens.contains(qt) }
}
