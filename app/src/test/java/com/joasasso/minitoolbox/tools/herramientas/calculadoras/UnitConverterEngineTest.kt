package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

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

    // -------------------------------------------------------------------------
    // Parseo de entrada
    // -------------------------------------------------------------------------

    @Test
    fun parseInput_aceptaPuntoYComa() {
        assertEquals(10.5, UnitConverterEngine.parseInput("10.5")!!, 1e-9)
        assertEquals(10.5, UnitConverterEngine.parseInput("10,5")!!, 1e-9)
        assertEquals(100.0, UnitConverterEngine.parseInput(" 100 ")!!, 1e-9)
        assertEquals(0.0, UnitConverterEngine.parseInput("0")!!, 1e-9)
        assertEquals(-5.2, UnitConverterEngine.parseInput("-5,2")!!, 1e-9)
    }

    @Test
    fun parseInput_rechazaEntradasInvalidas() {
        assertNull(UnitConverterEngine.parseInput(""))
        assertNull(UnitConverterEngine.parseInput("   "))
        assertNull(UnitConverterEngine.parseInput("abc"))
        assertNull(UnitConverterEngine.parseInput("10.5.2"))
    }

    // -------------------------------------------------------------------------
    // Formateo de números
    // -------------------------------------------------------------------------

    @Test
    fun formatNumber_casosComunes() {
        assertEquals("0", UnitConverterEngine.formatNumber(0.0))
        assertEquals("1", UnitConverterEngine.formatNumber(1.0))
        assertEquals("1.5", UnitConverterEngine.formatNumber(1.5))
        assertEquals("1,000", UnitConverterEngine.formatNumber(1000.0))
        assertEquals("12,345.67", UnitConverterEngine.formatNumber(12345.67))
        assertEquals("—", UnitConverterEngine.formatNumber(Double.NaN))
    }

    @Test
    fun formatNumber_notacionCientificaParaExtremos() {
        val formattedSmall = UnitConverterEngine.formatNumber(0.00001)
        // 1E-5
        assertEquals("1E-5", formattedSmall)

        val formattedLarge = UnitConverterEngine.formatNumber(2.5e10)
        assertEquals("2.5E10", formattedLarge)
    }

    @Test
    fun convertAll_devuelveListaCompleta() {
        val m = UnitConverterEngine.lengthUnits.first { it.id == "m" }
        val conversions = UnitConverterEngine.convertAll(1.0, m)

        assertEquals(UnitConverterEngine.lengthUnits.size, conversions.size)
        val cmItem = conversions.find { it.unit.id == "cm" }
        assertNotNull(cmItem)
        assertEquals(100.0, cmItem!!.rawValue, 1e-6)
        assertEquals("100", cmItem.formattedValue)
    }
}
