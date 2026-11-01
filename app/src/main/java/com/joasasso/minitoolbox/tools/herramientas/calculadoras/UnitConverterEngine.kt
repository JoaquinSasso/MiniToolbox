package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.annotation.StringRes
import com.joasasso.minitoolbox.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.roundToLong

enum class UnitCategory(@StringRes val titleRes: Int) {
    Length(R.string.unit_type_length),
    Weight(R.string.unit_type_weight),
    Temperature(R.string.unit_type_temperature),
    Time(R.string.unit_type_time)
}

/**
 * Unidad compuesta de dos partes (mayor + menor). El valor de una [ConversionUnit] compuesta se
 * expresa siempre en la unidad menor: 5'11" se guarda como 71 pulgadas.
 */
enum class CompoundFormat(val minorPerMajor: Int, val majorSymbol: String, val minorSymbol: String) {
    FeetInches(12, "'", "\""),
    PoundsOunces(16, " lb ", " oz")
}

data class ConversionUnit(
    val id: String,
    @StringRes val nameRes: Int,
    val symbol: String,
    val category: UnitCategory,
    val factorToBase: Double = 1.0,
    val compound: CompoundFormat? = null
)

/**
 * Motor de conversión de unidades y formateo numérico puro (sin dependencias de Android Context ni UI).
 */
object UnitConverterEngine {

    private const val SIGNIFICANT_DIGITS = 6
    private const val MAX_FRACTION_DIGITS = 6

    val lengthUnits: List<ConversionUnit> = listOf(
        ConversionUnit("m", R.string.unit_meter, "m", UnitCategory.Length, 1.0),
        ConversionUnit("cm", R.string.unit_centimeter, "cm", UnitCategory.Length, 0.01),
        ConversionUnit("mm", R.string.unit_millimeter, "mm", UnitCategory.Length, 0.001),
        ConversionUnit("km", R.string.unit_kilometer, "km", UnitCategory.Length, 1000.0),
        ConversionUnit("ft_in", R.string.unit_feet_inches, "ft·in", UnitCategory.Length, 0.0254, CompoundFormat.FeetInches),
        ConversionUnit("in", R.string.unit_inch, "in", UnitCategory.Length, 0.0254),
        ConversionUnit("ft", R.string.unit_foot, "ft", UnitCategory.Length, 0.3048),
        ConversionUnit("yd", R.string.unit_yard, "yd", UnitCategory.Length, 0.9144),
        ConversionUnit("mi", R.string.unit_mile, "mi", UnitCategory.Length, 1609.344)
    )

    val weightUnits: List<ConversionUnit> = listOf(
        ConversionUnit("kg", R.string.unit_kilogram, "kg", UnitCategory.Weight, 1.0),
        ConversionUnit("g", R.string.unit_gram, "g", UnitCategory.Weight, 0.001),
        ConversionUnit("mg", R.string.unit_milligram, "mg", UnitCategory.Weight, 0.000001),
        ConversionUnit("t", R.string.unit_ton, "t", UnitCategory.Weight, 1000.0),
        ConversionUnit("lb_oz", R.string.unit_pounds_ounces, "lb·oz", UnitCategory.Weight, 0.028349523125, CompoundFormat.PoundsOunces),
        ConversionUnit("lb", R.string.unit_pound, "lb", UnitCategory.Weight, 0.45359237),
        ConversionUnit("oz", R.string.unit_ounce, "oz", UnitCategory.Weight, 0.028349523125)
    )

    val temperatureUnits: List<ConversionUnit> = listOf(
        ConversionUnit("c", R.string.unit_celsius, "°C", UnitCategory.Temperature),
        ConversionUnit("f", R.string.unit_fahrenheit, "°F", UnitCategory.Temperature),
        ConversionUnit("k", R.string.unit_kelvin, "K", UnitCategory.Temperature)
    )

    val timeUnits: List<ConversionUnit> = listOf(
        ConversionUnit("s", R.string.unit_second, "s", UnitCategory.Time, 1.0),
        ConversionUnit("min", R.string.unit_minute, "min", UnitCategory.Time, 60.0),
        ConversionUnit("h", R.string.unit_hour, "h", UnitCategory.Time, 3600.0),
        ConversionUnit("d", R.string.unit_day, "d", UnitCategory.Time, 86400.0)
    )

    val allUnits: List<ConversionUnit> = lengthUnits + weightUnits + temperatureUnits + timeUnits

    fun unit(id: String): ConversionUnit = allUnits.first { it.id == id }

    fun getUnitsForCategory(category: UnitCategory): List<ConversionUnit> {
        return when (category) {
            UnitCategory.Length -> lengthUnits
            UnitCategory.Weight -> weightUnits
            UnitCategory.Temperature -> temperatureUnits
            UnitCategory.Time -> timeUnits
        }
    }

    /**
     * Unidad de origen al entrar a cada categoría: la más usada en países métricos,
     * desde donde suele partir la conversión.
     */
    fun defaultUnitFor(category: UnitCategory): ConversionUnit {
        return when (category) {
            UnitCategory.Length -> unit("cm")
            UnitCategory.Weight -> unit("kg")
            UnitCategory.Temperature -> unit("c")
            UnitCategory.Time -> unit("h")
        }
    }

    /**
     * Destino más probable para cada origen: el equivalente del otro sistema (cm → pies +
     * pulgadas, kg → lb) o, en tiempo, la unidad vecina. Se muestra primero y destacado.
     */
    private val primaryTargetIds: Map<String, String> = mapOf(
        "m" to "ft", "cm" to "ft_in", "mm" to "in", "km" to "mi",
        "ft_in" to "cm", "in" to "cm", "ft" to "m", "yd" to "m", "mi" to "km",
        "kg" to "lb", "g" to "oz", "mg" to "g", "t" to "lb",
        "lb_oz" to "kg", "lb" to "kg", "oz" to "g",
        "c" to "f", "f" to "c", "k" to "c",
        "s" to "min", "min" to "h", "h" to "min", "d" to "h"
    )

    /** Unidades de destino para [source]: primero la más probable, después el resto en orden. */
    fun targetsFor(source: ConversionUnit): List<ConversionUnit> {
        val others = getUnitsForCategory(source.category).filter { it != source }
        val primary = primaryTargetIds[source.id]?.let(::unit) ?: return others
        return listOf(primary) + others.filter { it != primary }
    }

    /**
     * Convierte [value] expresado en [from] a la unidad [to].
     */
    fun convert(value: Double, from: ConversionUnit, to: ConversionUnit): Double {
        if (from == to) return value
        if (from.category != to.category) return value

        return when (from.category) {
            UnitCategory.Temperature -> convertTemperature(value, from.symbol, to.symbol)
            else -> {
                val inBase = value * from.factorToBase
                inBase / to.factorToBase
            }
        }
    }

    private fun convertTemperature(value: Double, fromSymbol: String, toSymbol: String): Double {
        val inCelsius = when (fromSymbol) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> value
        }
        return when (toSymbol) {
            "°C" -> inCelsius
            "°F" -> (inCelsius * 9.0 / 5.0) + 32.0
            "K" -> inCelsius + 273.15
            else -> inCelsius
        }
    }

    /**
     * Formatea un número con hasta [SIGNIFICANT_DIGITS] cifras significativas, usando los
     * separadores de [locale]. Con [grouping] en false no agrega separador de miles, para que
     * el texto se pueda volver a parsear o pegar en otra app.
     */
    fun formatNumber(value: Double, locale: Locale, grouping: Boolean = true): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        if (value == 0.0) return "0"

        val symbols = DecimalFormatSymbols.getInstance(locale)
        val absVal = abs(value)
        if (absVal >= 1e9 || absVal < 1e-4) {
            return DecimalFormat("0.####E0", symbols).format(value)
        }

        val integerDigits = floor(log10(absVal)).toInt() + 1
        val df = DecimalFormat(if (grouping) "#,##0" else "0", symbols).apply {
            maximumFractionDigits = (SIGNIFICANT_DIGITS - integerDigits).coerceIn(0, MAX_FRACTION_DIGITS)
            roundingMode = RoundingMode.HALF_UP
        }
        return normalizeNegativeZero(df.format(value))
    }

    /**
     * Formatea [value], expresado en [unit], para mostrarlo. Las unidades compuestas se muestran
     * como `5'10.9"` o `7 lb 4 oz`, con la parte menor redondeada a un decimal.
     */
    fun formatValue(value: Double, unit: ConversionUnit, locale: Locale, grouping: Boolean = true): String {
        val compound = unit.compound ?: return formatNumber(value, locale, grouping)
        if (value.isNaN() || value.isInfinite()) return "—"

        val absMinor = abs(value)
        if (absMinor >= 1e9) {
            return (formatNumber(value / compound.minorPerMajor, locale, grouping) + compound.majorSymbol).trimEnd()
        }

        // Redondeo en décimas de la unidad menor antes de separar, para que 5'11.96" pase a 6'0".
        val tenths = (absMinor * 10).roundToLong()
        val tenthsPerMajor = compound.minorPerMajor * 10L
        val major = tenths / tenthsPerMajor
        val minor = (tenths % tenthsPerMajor) / 10.0

        val symbols = DecimalFormatSymbols.getInstance(locale)
        val majorText = DecimalFormat(if (grouping) "#,##0" else "0", symbols).format(major)
        val minorText = DecimalFormat("0.#", symbols).format(minor)
        val sign = if (value < 0 && tenths > 0) "-" else ""
        return "$sign$majorText${compound.majorSymbol}$minorText${compound.minorSymbol}".trim()
    }

    private fun normalizeNegativeZero(text: String): String = if (text == "-0") "0" else text
}
