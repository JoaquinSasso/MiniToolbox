package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import java.util.Locale

/** Valor junto con la unidad en que está expresado. */
data class Quantity(val value: Double, val unit: ConversionUnit)

enum class InputErrorKind {
    /** Hay texto pero ningún número (`hola`, `ft`). */
    InvalidFormat,

    /** Un número mal formado (`1.2.3`). */
    InvalidNumber,

    /** Una palabra que no es una unidad conocida. */
    UnknownUnit,

    /** Comillas que no se pueden interpretar (`5''`, `5'11'`). */
    AmbiguousQuotes,
    InchesOutOfRange,
    OuncesOutOfRange,
    FractionalFeet,
    FractionalPounds,

    /** Dos partes que no forman una unidad compuesta soportada (`1 m 20 cm`). */
    UnsupportedCombination
}

sealed interface ParsedInput {
    data object Empty : ParsedInput

    /**
     * Entrada válida. [explicitUnit] indica si la unidad salió del texto (`180 cm`, `5'11`)
     * en lugar de la unidad de origen elegida.
     */
    data class Valid(val quantity: Quantity, val explicitUnit: Boolean) : ParsedInput

    /**
     * Entrada inválida. [detail] lleva el fragmento problemático cuando aporta (la unidad
     * desconocida) y [suggestion] una interpretación obvia, si existe (`5'13` → `6'1"`).
     */
    data class Invalid(
        val kind: InputErrorKind,
        val detail: String? = null,
        val suggestion: Quantity? = null
    ) : ParsedInput
}

/**
 * Parser puro de la entrada del conversor. Acepta un número solo (en la unidad de origen),
 * un número con unidad (`180 cm`, `70kg`, `30 °C`) y las medidas imperiales compuestas
 * pies + pulgadas (`5'11"`, `5 ft 11 in`) y libras + onzas (`7 lb 4 oz`).
 * La coma y el punto valen como separador decimal; no hay separador de miles.
 */
object MeasurementParser {

    private sealed interface Token {
        data class Number(val text: String) : Token
        data class Word(val text: String) : Token
        data object FeetMark : Token
        data object InchMark : Token
        data object DoubleApostrophe : Token
    }

    private class Part(val value: Double, val unit: Token?)

    /** Corta el parseo apenas aparece un error. Sin stack trace: es control de flujo, no un fallo. */
    private class ParseFailure(val result: ParsedInput) : Exception() {
        override fun fillInStackTrace(): Throwable = this
    }

    private val aliases: Map<String, String> = buildMap {
        fun add(id: String, vararg names: String) = names.forEach { put(it, id) }
        add("m", "m", "meter", "meters", "metre", "metres", "metro", "metros")
        add("cm", "cm", "centimeter", "centimeters", "centímetro", "centímetros", "centimetro", "centimetros")
        add("mm", "mm", "millimeter", "millimeters", "milímetro", "milímetros", "milimetro", "milimetros")
        add("km", "km", "kilometer", "kilometers", "kilómetro", "kilómetros", "kilometro", "kilometros")
        add("in", "in", "inch", "inches", "pulg", "pulgada", "pulgadas")
        add("ft", "ft", "foot", "feet", "pie", "pies")
        add("yd", "yd", "yard", "yards", "yarda", "yardas")
        add("mi", "mi", "mile", "miles", "milla", "millas")
        add("kg", "kg", "kilo", "kilos", "kilogram", "kilograms", "kilogramo", "kilogramos")
        add("g", "g", "gr", "gram", "grams", "gramo", "gramos")
        add("mg", "mg", "milligram", "milligrams", "miligramo", "miligramos")
        add("t", "t", "tonne", "tonnes", "tonelada", "toneladas")
        add("lb", "lb", "lbs", "pound", "pounds", "libra", "libras")
        add("oz", "oz", "ounce", "ounces", "onza", "onzas")
        add("c", "°c", "c", "celsius")
        add("f", "°f", "f", "fahrenheit")
        add("k", "k", "°k", "kelvin")
        add("s", "s", "sec", "seg", "second", "seconds", "segundo", "segundos")
        add("min", "min", "mins", "minute", "minutes", "minuto", "minutos")
        add("h", "h", "hr", "hrs", "hour", "hours", "hora", "horas")
        add("d", "d", "day", "days", "día", "días", "dia", "dias")
    }

    /**
     * Interpreta [raw]. Un número sin unidad se toma en [defaultUnit]; si esa unidad es
     * compuesta, el número es la parte mayor (`5` en pies + pulgadas son 5 pies) y dos números
     * separados por espacio son las dos partes (`5 11`).
     */
    fun parse(raw: String, defaultUnit: ConversionUnit): ParsedInput {
        return try {
            parseOrThrow(raw, defaultUnit)
        } catch (failure: ParseFailure) {
            failure.result
        }
    }

    private fun parseOrThrow(raw: String, defaultUnit: ConversionUnit): ParsedInput {
        val text = normalize(raw)
        var negative = false
        var body = text
        if (body.startsWith("-")) {
            negative = true
            body = body.substring(1).trimStart()
        }
        if (body.isEmpty() || body.all { it == '.' || it == ',' }) return ParsedInput.Empty

        val tokens = tokenize(body)
        if (tokens.none { it is Token.Number }) fail(InputErrorKind.InvalidFormat)
        val parts = groupIntoParts(tokens)
        val sign = if (negative) -1.0 else 1.0

        return when (parts.size) {
            1 -> resolveSingle(parts[0], defaultUnit, sign)
            2 -> resolveCompound(parts[0], parts[1], defaultUnit, sign)
            else -> fail(InputErrorKind.InvalidFormat)
        }
    }

    private fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.trim()) {
            sb.append(
                when (ch) {
                    '’', '‘', '′', '´', '`' -> '\''
                    '”', '“', '″' -> '"'
                    'º' -> '°'
                    '−', '–' -> '-'
                    else -> ch
                }
            )
        }
        return sb.toString().lowercase(Locale.ROOT)
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' || ch == ',' -> {
                    val start = i
                    while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == ',')) i++
                    tokens += Token.Number(text.substring(start, i))
                }
                ch == '\'' -> {
                    if (i + 1 < text.length && text[i + 1] == '\'') {
                        tokens += Token.DoubleApostrophe
                        i += 2
                    } else {
                        tokens += Token.FeetMark
                        i++
                    }
                }
                ch == '"' -> {
                    tokens += Token.InchMark
                    i++
                }
                ch.isLetter() || ch == '°' -> {
                    val start = i
                    while (i < text.length && (text[i].isLetter() || text[i] == '°')) i++
                    tokens += Token.Word(text.substring(start, i))
                }
                else -> fail(InputErrorKind.InvalidFormat)
            }
        }
        return tokens
    }

    /** Agrupa los tokens en pares número + unidad opcional. */
    private fun groupIntoParts(tokens: List<Token>): List<Part> {
        val parts = mutableListOf<Part>()
        var i = 0
        while (i < tokens.size) {
            val number = tokens[i] as? Token.Number ?: fail(InputErrorKind.InvalidFormat)
            val unit = tokens.getOrNull(i + 1)?.takeUnless { it is Token.Number }
            parts += Part(parseNumber(number.text), unit)
            i += if (unit != null) 2 else 1
        }
        return parts
    }

    private fun parseNumber(text: String): Double {
        val separators = text.count { it == '.' || it == ',' }
        if (separators > 1 || text.none { it.isDigit() }) fail(InputErrorKind.InvalidNumber)
        val value = text.replace(',', '.').toDoubleOrNull()
        if (value == null || value.isInfinite()) fail(InputErrorKind.InvalidNumber)
        return value
    }

    private fun resolveSingle(part: Part, defaultUnit: ConversionUnit, sign: Double): ParsedInput {
        val value = part.value * sign
        return when (val unit = part.unit) {
            null -> {
                val factor = defaultUnit.compound?.minorPerMajor ?: 1
                ParsedInput.Valid(Quantity(value * factor, defaultUnit), explicitUnit = false)
            }
            Token.FeetMark -> ParsedInput.Valid(Quantity(value * 12, unitById("ft_in")), explicitUnit = true)
            Token.InchMark -> ParsedInput.Valid(Quantity(value, unitById("in")), explicitUnit = true)
            Token.DoubleApostrophe -> fail(InputErrorKind.AmbiguousQuotes)
            is Token.Word -> ParsedInput.Valid(Quantity(value, resolveWord(unit)), explicitUnit = true)
            is Token.Number -> fail(InputErrorKind.InvalidFormat)
        }
    }

    private fun resolveCompound(major: Part, minor: Part, defaultUnit: ConversionUnit, sign: Double): ParsedInput {
        val format = when (val unit = major.unit) {
            null -> defaultUnit.compound ?: fail(InputErrorKind.InvalidFormat)
            Token.FeetMark -> CompoundFormat.FeetInches
            Token.DoubleApostrophe, Token.InchMark -> fail(InputErrorKind.AmbiguousQuotes)
            is Token.Word -> when (resolveWord(unit).id) {
                "ft" -> CompoundFormat.FeetInches
                "lb" -> CompoundFormat.PoundsOunces
                else -> fail(InputErrorKind.UnsupportedCombination)
            }
            is Token.Number -> fail(InputErrorKind.InvalidFormat)
        }
        checkMinorUnit(minor.unit, format)

        val compoundUnit = unitById(if (format == CompoundFormat.FeetInches) "ft_in" else "lb_oz")
        val explicit = major.unit != null || minor.unit != null
        if (major.value % 1.0 != 0.0) {
            fail(if (format == CompoundFormat.FeetInches) InputErrorKind.FractionalFeet else InputErrorKind.FractionalPounds)
        }

        val total = (major.value * format.minorPerMajor + minor.value) * sign
        if (minor.value >= format.minorPerMajor) {
            val kind = if (format == CompoundFormat.FeetInches) InputErrorKind.InchesOutOfRange else InputErrorKind.OuncesOutOfRange
            fail(kind, suggestion = Quantity(total, compoundUnit))
        }
        return ParsedInput.Valid(Quantity(total, compoundUnit), explicit)
    }

    private fun checkMinorUnit(unit: Token?, format: CompoundFormat) {
        when (unit) {
            null -> Unit
            Token.FeetMark -> fail(
                if (format == CompoundFormat.FeetInches) InputErrorKind.AmbiguousQuotes else InputErrorKind.UnsupportedCombination
            )
            Token.InchMark, Token.DoubleApostrophe -> {
                if (format != CompoundFormat.FeetInches) fail(InputErrorKind.UnsupportedCombination)
            }
            is Token.Word -> {
                val expected = if (format == CompoundFormat.FeetInches) "in" else "oz"
                if (resolveWord(unit).id != expected) fail(InputErrorKind.UnsupportedCombination)
            }
            is Token.Number -> fail(InputErrorKind.InvalidFormat)
        }
    }

    private fun resolveWord(word: Token.Word): ConversionUnit {
        val id = aliases[word.text] ?: fail(InputErrorKind.UnknownUnit, detail = word.text)
        return unitById(id)
    }

    private fun unitById(id: String): ConversionUnit = UnitConverterEngine.unit(id)

    private fun fail(kind: InputErrorKind, detail: String? = null, suggestion: Quantity? = null): Nothing {
        throw ParseFailure(ParsedInput.Invalid(kind, detail, suggestion))
    }
}

/**
 * Edición del texto del campo desde las teclas propias del conversor (`'`, `"`, `lb`, `oz`, ±).
 * Es lógica pura para poder testear la posición del cursor.
 */
object ConverterInputEditing {

    data class Edit(val text: String, val cursor: Int)

    /** Reemplaza la selección [selectionStart]..[selectionEnd] por [token] y deja el cursor detrás. */
    fun insert(text: String, selectionStart: Int, selectionEnd: Int, token: String): Edit {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val newText = text.substring(0, start) + token + text.substring(end)
        return Edit(newText, start + token.length)
    }

    /** Agrega o quita el signo menos al principio del texto. */
    fun toggleSign(text: String): Edit {
        val newText = if (text.startsWith("-")) text.removePrefix("-") else "-$text"
        return Edit(newText, newText.length)
    }
}
