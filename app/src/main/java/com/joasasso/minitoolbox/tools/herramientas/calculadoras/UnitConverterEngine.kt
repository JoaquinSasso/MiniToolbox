package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.annotation.StringRes
import com.joasasso.minitoolbox.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class UnitCategory(@StringRes val titleRes: Int) {
    Length(R.string.unit_type_length),
    Weight(R.string.unit_type_weight),
    Temperature(R.string.unit_type_temperature),
    Time(R.string.unit_type_time)
}

data class ConversionUnit(
    val id: String,
    @StringRes val nameRes: Int,
    val symbol: String,
    val category: UnitCategory,
    val factorToBase: Double = 1.0
)

data class UnitConversionItem(
    val unit: ConversionUnit,
    val rawValue: Double,
    val formattedValue: String
)

/**
 * Motor de conversión de unidades y formateo numérico puro (sin dependencias de Android Context ni UI).
 */
object UnitConverterEngine {

    val lengthUnits: List<ConversionUnit> = listOf(
        ConversionUnit("m", R.string.unit_meter, "m", UnitCategory.Length, 1.0),
        ConversionUnit("cm", R.string.unit_centimeter, "cm", UnitCategory.Length, 0.01),
        ConversionUnit("mm", R.string.unit_millimeter, "mm", UnitCategory.Length, 0.001),
        ConversionUnit("km", R.string.unit_kilometer, "km", UnitCategory.Length, 1000.0),
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

    fun getUnitsForCategory(category: UnitCategory): List<ConversionUnit> {
        return when (category) {
            UnitCategory.Length -> lengthUnits
            UnitCategory.Weight -> weightUnits
            UnitCategory.Temperature -> temperatureUnits
            UnitCategory.Time -> timeUnits
        }
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
     * Formatea un número para visualización en pantalla con hasta 6 decimales significativos,
     * eliminando ceros redundantes a la derecha y usando notación científica para valores extremos.
     */
    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        if (abs(value) == 0.0) return "0"

        val absVal = abs(value)
        if (absVal >= 1e9 || absVal < 1e-4) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("0.####E0", symbols)
            return df.format(value)
        }

        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,##0.######", symbols)
        return df.format(value)
    }

    /**
     * Formato numérico limpio sin separadores de miles, apto para copiar al portapapeles.
     */
    fun formatCleanRaw(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        if (abs(value) == 0.0) return "0"

        val absVal = abs(value)
        if (absVal >= 1e9 || absVal < 1e-4) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("0.####E0", symbols)
            return df.format(value)
        }

        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("0.######", symbols)
        return df.format(value)
    }

    /**
     * Calcula la conversión de [value] a todas las unidades de la misma categoría.
     */
    fun convertAll(value: Double, from: ConversionUnit): List<UnitConversionItem> {
        val units = getUnitsForCategory(from.category)
        return units.map { targetUnit ->
            val res = convert(value, from, targetUnit)
            UnitConversionItem(
                unit = targetUnit,
                rawValue = res,
                formattedValue = formatNumber(res)
            )
        }
    }

    /**
     * Sanitiza y parsea el texto de entrada a Double, admitiendo coma o punto decimal.
     */
    fun parseInput(rawInput: String): Double? {
        val cleaned = rawInput.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull()
    }
}
