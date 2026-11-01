package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UnitConverterEngineTest {

    // -------------------------------------------------------------------------
    // Longitud
    // -------------------------------------------------------------------------

    @Test
    fun convertLength_metricBasics() {
        val m = UnitConverterEngine.lengthUnits.first { it.id == "m" }
        val cm = UnitConverterEngine.lengthUnits.first { it.id == "cm" }
        val mm = UnitConverterEngine.lengthUnits.first { it.id == "mm" }
        val km = UnitConverterEngine.lengthUnits.first { it.id == "km" }

        assertEquals(100.0, UnitConverterEngine.convert(1.0, m, cm), 1e-6)
        assertEquals(1000.0, UnitConverterEngine.convert(1.0, m, mm), 1e-6)
        assertEquals(0.001, UnitConverterEngine.convert(1.0, m, km), 1e-6)
        assertEquals(1000.0, UnitConverterEngine.convert(1.0, km, m), 1e-6)
        assertEquals(1.0, UnitConverterEngine.convert(100.0, cm, m), 1e-6)
    }

    @Test
    fun convertLength_imperialConversions() {
        val m = UnitConverterEngine.lengthUnits.first { it.id == "m" }
        val inch = UnitConverterEngine.lengthUnits.first { it.id == "in" }
        val ft = UnitConverterEngine.lengthUnits.first { it.id == "ft" }
        val mi = UnitConverterEngine.lengthUnits.first { it.id == "mi" }

        assertEquals(0.0254, UnitConverterEngine.convert(1.0, inch, m), 1e-6)
        assertEquals(0.3048, UnitConverterEngine.convert(1.0, ft, m), 1e-6)
        assertEquals(1609.344, UnitConverterEngine.convert(1.0, mi, m), 1e-3)
    }

    @Test
    fun convert_sameUnitReturnsOriginalValue() {
        val m = UnitConverterEngine.lengthUnits.first { it.id == "m" }
        assertEquals(42.5, UnitConverterEngine.convert(42.5, m, m), 1e-9)
    }

    // -------------------------------------------------------------------------
    // Peso / Masa
    // -------------------------------------------------------------------------

    @Test
    fun convertWeight_metricAndImperial() {
        val kg = UnitConverterEngine.weightUnits.first { it.id == "kg" }
        val g = UnitConverterEngine.weightUnits.first { it.id == "g" }
        val lb = UnitConverterEngine.weightUnits.first { it.id == "lb" }
        val oz = UnitConverterEngine.weightUnits.first { it.id == "oz" }

        assertEquals(1000.0, UnitConverterEngine.convert(1.0, kg, g), 1e-6)
        assertEquals(1.0, UnitConverterEngine.convert(1000.0, g, kg), 1e-6)
        assertEquals(0.45359237, UnitConverterEngine.convert(1.0, lb, kg), 1e-6)
        assertEquals(16.0, UnitConverterEngine.convert(1.0, lb, oz), 1e-3)
    }

    // -------------------------------------------------------------------------
    // Temperatura
    // -------------------------------------------------------------------------

    @Test
    fun convertTemperature_celsiusFahrenheitKelvin() {
        val c = UnitConverterEngine.temperatureUnits.first { it.id == "c" }
        val f = UnitConverterEngine.temperatureUnits.first { it.id == "f" }
        val k = UnitConverterEngine.temperatureUnits.first { it.id == "k" }

        // 0 °C = 32 °F = 273.15 K
        assertEquals(32.0, UnitConverterEngine.convert(0.0, c, f), 1e-6)
        assertEquals(273.15, UnitConverterEngine.convert(0.0, c, k), 1e-6)

        // 100 °C = 212 °F = 373.15 K
        assertEquals(212.0, UnitConverterEngine.convert(100.0, c, f), 1e-6)
        assertEquals(373.15, UnitConverterEngine.convert(100.0, c, k), 1e-6)

        // -40 °C = -40 °F
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, c, f), 1e-6)
        assertEquals(-40.0, UnitConverterEngine.convert(-40.0, f, c), 1e-6)

        // Inversiones
        assertEquals(0.0, UnitConverterEngine.convert(32.0, f, c), 1e-6)
        assertEquals(0.0, UnitConverterEngine.convert(273.15, k, c), 1e-6)
    }

    // -------------------------------------------------------------------------
    // Tiempo
    // -------------------------------------------------------------------------

    @Test
    fun convertTime_basics() {
        val s = UnitConverterEngine.timeUnits.first { it.id == "s" }
        val min = UnitConverterEngine.timeUnits.first { it.id == "min" }
        val h = UnitConverterEngine.timeUnits.first { it.id == "h" }
        val d = UnitConverterEngine.timeUnits.first { it.id == "d" }

        assertEquals(60.0, UnitConverterEngine.convert(1.0, min, s), 1e-6)
        assertEquals(3600.0, UnitConverterEngine.convert(1.0, h, s), 1e-6)
        assertEquals(24.0, UnitConverterEngine.convert(1.0, d, h), 1e-6)
        assertEquals(0.5, UnitConverterEngine.convert(30.0, min, h), 1e-6)
    }

    @Test
    fun convertCompound_usaLaUnidadMenorComoValor() {
        val ftIn = UnitConverterEngine.unit("ft_in")
        val cm = UnitConverterEngine.unit("cm")
        val lbOz = UnitConverterEngine.unit("lb_oz")
        val kg = UnitConverterEngine.unit("kg")

        // 5'11" = 71 in = 180.34 cm
        assertEquals(180.34, UnitConverterEngine.convert(71.0, ftIn, cm), 1e-9)
        assertEquals(70.866142, UnitConverterEngine.convert(180.0, cm, ftIn), 1e-6)
        // 7 lb 4 oz = 116 oz
        assertEquals(3.288544, UnitConverterEngine.convert(116.0, lbOz, kg), 1e-6)
    }

    @Test
    fun defaultUnitFor_cadaCategoria() {
        assertEquals("cm", UnitConverterEngine.defaultUnitFor(UnitCategory.Length).id)
        assertEquals("kg", UnitConverterEngine.defaultUnitFor(UnitCategory.Weight).id)
        assertEquals("c", UnitConverterEngine.defaultUnitFor(UnitCategory.Temperature).id)
        assertEquals("h", UnitConverterEngine.defaultUnitFor(UnitCategory.Time).id)
    }

    @Test
    fun idsDeUnidad_sonUnicos() {
        val ids = UnitConverterEngine.allUnits.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    // -------------------------------------------------------------------------
    // Formateo de números
    // -------------------------------------------------------------------------

    private val us = Locale.US
    private val es = Locale.forLanguageTag("es-AR")

    @Test
    fun formatNumber_casosComunes() {
        assertEquals("0", UnitConverterEngine.formatNumber(0.0, us))
        assertEquals("1", UnitConverterEngine.formatNumber(1.0, us))
        assertEquals("1.5", UnitConverterEngine.formatNumber(1.5, us))
        assertEquals("1,000", UnitConverterEngine.formatNumber(1000.0, us))
        assertEquals("12,345.7", UnitConverterEngine.formatNumber(12345.67, us))
        assertEquals("—", UnitConverterEngine.formatNumber(Double.NaN, us))
        assertEquals("—", UnitConverterEngine.formatNumber(Double.POSITIVE_INFINITY, us))
    }

    @Test
    fun formatNumber_seisCifrasSignificativas() {
        assertEquals("154.324", UnitConverterEngine.formatNumber(154.3235835, us))
        assertEquals("180.34", UnitConverterEngine.formatNumber(180.34, us))
        assertEquals("0.123457", UnitConverterEngine.formatNumber(0.1234567, us))
        assertEquals("1,609.34", UnitConverterEngine.formatNumber(1609.344, us))
        assertEquals("123,457", UnitConverterEngine.formatNumber(123456.7, us))
    }

    @Test
    fun formatNumber_respetaElLocale() {
        assertEquals("2,5", UnitConverterEngine.formatNumber(2.5, es))
        assertEquals("1.609,34", UnitConverterEngine.formatNumber(1609.344, es))
        assertEquals("1609,34", UnitConverterEngine.formatNumber(1609.344, es, grouping = false))
        assertEquals("1609.34", UnitConverterEngine.formatNumber(1609.344, us, grouping = false))
    }

    @Test
    fun formatNumber_negativos() {
        assertEquals("-40", UnitConverterEngine.formatNumber(-40.0, us))
        assertEquals("-273.15", UnitConverterEngine.formatNumber(-273.15, us))
    }

    @Test
    fun formatNumber_notacionCientificaParaExtremos() {
        assertEquals("1E-5", UnitConverterEngine.formatNumber(0.00001, us))
        assertEquals("2.5E10", UnitConverterEngine.formatNumber(2.5e10, us))
    }

    // -------------------------------------------------------------------------
    // Formateo de unidades compuestas
    // -------------------------------------------------------------------------

    private val ftIn = UnitConverterEngine.unit("ft_in")
    private val lbOz = UnitConverterEngine.unit("lb_oz")

    @Test
    fun formatValue_piesYPulgadas() {
        assertEquals("5'11\"", UnitConverterEngine.formatValue(71.0, ftIn, us))
        assertEquals("5'10.9\"", UnitConverterEngine.formatValue(70.866142, ftIn, us))
        assertEquals("5'10,9\"", UnitConverterEngine.formatValue(70.866142, ftIn, es))
        assertEquals("0'0\"", UnitConverterEngine.formatValue(0.0, ftIn, us))
        assertEquals("-5'11\"", UnitConverterEngine.formatValue(-71.0, ftIn, us))
    }

    @Test
    fun formatValue_piesYPulgadas_redondeoAcarreaAlPie() {
        // 71.96 in → 72.0 en décimas → 6'0", no 5'12"
        assertEquals("6'0\"", UnitConverterEngine.formatValue(71.96, ftIn, us))
        assertEquals("5'11.9\"", UnitConverterEngine.formatValue(71.94, ftIn, us))
    }

    @Test
    fun formatValue_piesYPulgadas_agrupaLosPies() {
        // 1 milla = 5280 ft
        assertEquals("5,280'0\"", UnitConverterEngine.formatValue(63360.0, ftIn, us))
        assertEquals("5280'0\"", UnitConverterEngine.formatValue(63360.0, ftIn, us, grouping = false))
    }

    @Test
    fun formatValue_librasYOnzas() {
        assertEquals("7 lb 4 oz", UnitConverterEngine.formatValue(116.0, lbOz, us))
        assertEquals("154 lb 5,2 oz", UnitConverterEngine.formatValue(2469.177, lbOz, es))
        assertEquals("8 lb 0 oz", UnitConverterEngine.formatValue(127.97, lbOz, us))
    }

    @Test
    fun formatValue_unidadSimple_esElNumero() {
        assertEquals("180.34", UnitConverterEngine.formatValue(180.34, UnitConverterEngine.unit("cm"), us))
    }

    @Test
    fun formatValue_compuestoSeVuelveAParsear() {
        listOf(70.866142, 71.0, 116.0, 63360.0).forEach { value ->
            listOf(ftIn, lbOz).forEach { unit ->
                listOf(us, es).forEach { locale ->
                    val text = UnitConverterEngine.formatValue(value, unit, locale, grouping = false)
                    val parsed = MeasurementParser.parse(text, UnitConverterEngine.unit("cm")) as ParsedInput.Valid
                    assertEquals(text, unit.id, parsed.quantity.unit.id)
                    assertEquals(text, value, parsed.quantity.value, 0.05)
                }
            }
        }
    }

    @Test
    fun targetsFor_ponePrimeroElDestinoMasProbable() {
        fun firstTarget(id: String) = UnitConverterEngine.targetsFor(UnitConverterEngine.unit(id)).first().id
        assertEquals("ft_in", firstTarget("cm"))
        assertEquals("cm", firstTarget("ft_in"))
        assertEquals("lb", firstTarget("kg"))
        assertEquals("f", firstTarget("c"))
        assertEquals("c", firstTarget("f"))
        assertEquals("min", firstTarget("h"))
    }

    @Test
    fun targetsFor_cubreTodaLaCategoriaSinElOrigenNiRepetidos() {
        UnitConverterEngine.allUnits.forEach { source ->
            val targets = UnitConverterEngine.targetsFor(source)
            val expected = UnitConverterEngine.getUnitsForCategory(source.category).filter { it != source }
            assertEquals(source.id, expected.toSet(), targets.toSet())
            assertEquals(source.id, expected.size, targets.size)
            assertTrue(source.id, targets.all { it.category == source.category })
        }
    }
}
