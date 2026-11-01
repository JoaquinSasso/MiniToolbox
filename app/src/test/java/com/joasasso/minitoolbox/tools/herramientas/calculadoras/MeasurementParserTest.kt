package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementParserTest {

    private val cm = UnitConverterEngine.unit("cm")
    private val kg = UnitConverterEngine.unit("kg")
    private val feetInches = UnitConverterEngine.unit("ft_in")
    private val poundsOunces = UnitConverterEngine.unit("lb_oz")

    private fun valid(raw: String, default: ConversionUnit = cm): ParsedInput.Valid {
        val result = MeasurementParser.parse(raw, default)
        return result as? ParsedInput.Valid ?: throw AssertionError("'$raw' debería ser válido y dio $result")
    }

    private fun invalid(raw: String, default: ConversionUnit = cm): ParsedInput.Invalid {
        val result = MeasurementParser.parse(raw, default)
        return result as? ParsedInput.Invalid ?: throw AssertionError("'$raw' debería ser inválido y dio $result")
    }

    private fun assertQuantity(raw: String, expectedValue: Double, expectedUnitId: String, default: ConversionUnit = cm) {
        val quantity = valid(raw, default).quantity
        assertEquals("unidad de '$raw'", expectedUnitId, quantity.unit.id)
        assertEquals("valor de '$raw'", expectedValue, quantity.value, 1e-9)
    }

    // -------------------------------------------------------------------------
    // Vacío e incompleto
    // -------------------------------------------------------------------------

    @Test
    fun entradaVacia_oSoloSeparadores_esEmpty() {
        listOf("", "   ", "-", " - ", ".", ",").forEach {
            assertEquals("'$it'", ParsedInput.Empty, MeasurementParser.parse(it, cm))
        }
    }

    // -------------------------------------------------------------------------
    // Números sin unidad
    // -------------------------------------------------------------------------

    @Test
    fun numeroSolo_usaUnidadPorDefecto() {
        assertQuantity("180", 180.0, "cm")
        assertQuantity(" 180 ", 180.0, "cm")
        assertFalse(valid("180").explicitUnit)
    }

    @Test
    fun separadorDecimal_aceptaComaYPunto() {
        assertQuantity("2,5", 2.5, "cm")
        assertQuantity("2.5", 2.5, "cm")
        assertQuantity(".5", 0.5, "cm")
        assertQuantity("5.", 5.0, "cm")
    }

    @Test
    fun signoNegativo_seAplicaAlValor() {
        assertQuantity("-10", -10.0, "cm")
        assertQuantity("−10", -10.0, "cm")
        assertQuantity("-2,5", -2.5, "cm")
    }

    @Test
    fun numeroSolo_conUnidadCompuestaPorDefecto_esLaParteMayor() {
        assertQuantity("5", 60.0, "ft_in", feetInches)
        assertQuantity("7", 112.0, "lb_oz", poundsOunces)
    }

    @Test
    fun dosNumeros_conUnidadCompuestaPorDefecto_sonLasDosPartes() {
        assertQuantity("5 11", 71.0, "ft_in", feetInches)
        assertQuantity("7 4", 116.0, "lb_oz", poundsOunces)
        assertFalse(valid("5 11", feetInches).explicitUnit)
    }

    // -------------------------------------------------------------------------
    // Número con unidad
    // -------------------------------------------------------------------------

    @Test
    fun numeroConUnidad_detectaLaUnidad() {
        assertQuantity("180 cm", 180.0, "cm", kg)
        assertQuantity("180cm", 180.0, "cm", kg)
        assertQuantity("70 kg", 70.0, "kg")
        assertQuantity("70KG", 70.0, "kg")
        assertQuantity("30 °C", 30.0, "c")
        assertQuantity("30°c", 30.0, "c")
        assertQuantity("30 ºC", 30.0, "c")
        assertQuantity("86 F", 86.0, "f")
        assertQuantity("2,5 h", 2.5, "h")
        assertQuantity("2,5 horas", 2.5, "h")
        assertQuantity("90 min", 90.0, "min")
        assertQuantity("5 ft", 5.0, "ft")
        assertQuantity("5 pies", 5.0, "ft")
        assertQuantity("12 in", 12.0, "in")
        assertQuantity("7 lb", 7.0, "lb")
        assertQuantity("4 oz", 4.0, "oz")
        assertTrue(valid("180 cm", kg).explicitUnit)
    }

    @Test
    fun marcaDePies_sola_esPiesYPulgadas() {
        assertQuantity("5'", 60.0, "ft_in")
        assertQuantity("5.5'", 66.0, "ft_in")
        assertTrue(valid("5'").explicitUnit)
    }

    @Test
    fun marcaDePulgadas_sola_esPulgadas() {
        assertQuantity("71\"", 71.0, "in")
        assertQuantity("11,5\"", 11.5, "in")
    }

    // -------------------------------------------------------------------------
    // Pies y pulgadas: formatos pedidos
    // -------------------------------------------------------------------------

    @Test
    fun piesYPulgadas_todosLosFormatosPedidos() {
        listOf("5'11", "5'11\"", "5' 11\"", "5 ft 11 in", "5ft11in").forEach { raw ->
            assertQuantity(raw, 71.0, "ft_in")
            assertTrue("'$raw' debería marcar unidad explícita", valid(raw).explicitUnit)
        }
    }

    @Test
    fun piesYPulgadas_pulgadasConDecimales() {
        assertQuantity("5'11.5\"", 71.5, "ft_in")
        assertQuantity("5'11,5\"", 71.5, "ft_in")
        assertQuantity("5'11,5", 71.5, "ft_in")
    }

    @Test
    fun piesYPulgadas_variantesTipograficasYDePalabras() {
        assertQuantity("5’11”", 71.0, "ft_in")
        assertQuantity("5′11″", 71.0, "ft_in")
        assertQuantity("5'11''", 71.0, "ft_in")
        assertQuantity("5 feet 11 inches", 71.0, "ft_in")
        assertQuantity("5 pies 11 pulgadas", 71.0, "ft_in")
        assertQuantity("5 ft 11", 71.0, "ft_in")
        assertQuantity("5'0\"", 60.0, "ft_in")
        assertQuantity("0'11\"", 11.0, "ft_in")
    }

    @Test
    fun piesYPulgadas_negativo() {
        assertQuantity("-5'11", -71.0, "ft_in")
    }

    @Test
    fun piesYPulgadas_ignoraLaUnidadPorDefecto() {
        assertQuantity("5'11", 71.0, "ft_in", kg)
    }

    // -------------------------------------------------------------------------
    // Libras y onzas
    // -------------------------------------------------------------------------

    @Test
    fun librasYOnzas_formatos() {
        listOf("7 lb 4 oz", "7lb4oz", "7 lb 4", "7 lbs 4 oz", "7 libras 4 onzas").forEach { raw ->
            assertQuantity(raw, 116.0, "lb_oz")
        }
        assertQuantity("7 lb 4,5 oz", 116.5, "lb_oz")
    }

    // -------------------------------------------------------------------------
    // Entradas inválidas
    // -------------------------------------------------------------------------

    @Test
    fun pulgadasFueraDeRango_sugiereLaMedidaNormalizada() {
        val result = invalid("5'13")
        assertEquals(InputErrorKind.InchesOutOfRange, result.kind)
        assertEquals("ft_in", result.suggestion!!.unit.id)
        assertEquals(73.0, result.suggestion!!.value, 1e-9)

        assertEquals(InputErrorKind.InchesOutOfRange, invalid("5'12").kind)
        assertEquals(InputErrorKind.InchesOutOfRange, invalid("5 ft 12.5 in").kind)
    }

    @Test
    fun onzasFueraDeRango_sugiereLaMedidaNormalizada() {
        val result = invalid("7 lb 20 oz")
        assertEquals(InputErrorKind.OuncesOutOfRange, result.kind)
        assertEquals(132.0, result.suggestion!!.value, 1e-9)
    }

    @Test
    fun comillasAmbiguas() {
        listOf("5''", "5'11'", "5\"11", "5''11").forEach {
            assertEquals("'$it'", InputErrorKind.AmbiguousQuotes, invalid(it).kind)
        }
    }

    @Test
    fun piesConDecimalesMasPulgadas_esError() {
        assertEquals(InputErrorKind.FractionalFeet, invalid("5.5'3").kind)
        assertEquals(InputErrorKind.FractionalPounds, invalid("7.5 lb 2 oz").kind)
    }

    @Test
    fun textoSinNumero_esFormatoInvalido() {
        listOf("hola", "ft", "'", "\"", "abc def").forEach {
            assertEquals("'$it'", InputErrorKind.InvalidFormat, invalid(it).kind)
        }
    }

    @Test
    fun simbolosNoReconocidos_sonFormatoInvalido() {
        listOf("5+3", "5/2", "5#", "5 - 3").forEach {
            assertEquals("'$it'", InputErrorKind.InvalidFormat, invalid(it).kind)
        }
    }

    @Test
    fun unidadDesconocida_informaLaPalabra() {
        val result = invalid("5 hola")
        assertEquals(InputErrorKind.UnknownUnit, result.kind)
        assertEquals("hola", result.detail)
        assertNull(result.suggestion)
    }

    @Test
    fun numeroMalFormado() {
        listOf("1.2.3", "1,2,3", "1.2,3", "5'1.2.3").forEach {
            assertEquals("'$it'", InputErrorKind.InvalidNumber, invalid(it).kind)
        }
    }

    @Test
    fun combinacionesNoSoportadas() {
        listOf("1 m 20 cm", "1 h 30 min", "5 ft 3 cm", "7 lb 4 in", "7 lb 4\"").forEach {
            assertEquals("'$it'", InputErrorKind.UnsupportedCombination, invalid(it).kind)
        }
    }

    @Test
    fun numerosDeMas_sonFormatoInvalido() {
        assertEquals(InputErrorKind.InvalidFormat, invalid("5 11").kind)
        assertEquals(InputErrorKind.InvalidFormat, invalid("5'11\"3").kind)
        assertEquals(InputErrorKind.InvalidFormat, invalid("5 ft in").kind)
        assertEquals(InputErrorKind.InvalidFormat, invalid("5--3").kind)
    }

    // -------------------------------------------------------------------------
    // Edición del campo
    // -------------------------------------------------------------------------

    @Test
    fun insert_agregaEnElCursorYLoMueveDetras() {
        assertEquals(ConverterInputEditing.Edit("5'", 2), ConverterInputEditing.insert("5", 1, 1, "'"))
        assertEquals(ConverterInputEditing.Edit("5'11", 2), ConverterInputEditing.insert("511", 1, 1, "'"))
        assertEquals(ConverterInputEditing.Edit("'", 1), ConverterInputEditing.insert("", 0, 0, "'"))
    }

    @Test
    fun insert_reemplazaLaSeleccionYToleraIndicesFueraDeRango() {
        assertEquals(ConverterInputEditing.Edit("7 lb ", 5), ConverterInputEditing.insert("7xx", 1, 3, " lb "))
        assertEquals(ConverterInputEditing.Edit("7 lb ", 5), ConverterInputEditing.insert("7xx", 3, 1, " lb "))
        assertEquals(ConverterInputEditing.Edit("5\"", 2), ConverterInputEditing.insert("5", 9, 9, "\""))
    }

    @Test
    fun toggleSign_agregaYQuitaElMenos() {
        assertEquals(ConverterInputEditing.Edit("-30", 3), ConverterInputEditing.toggleSign("30"))
        assertEquals(ConverterInputEditing.Edit("30", 2), ConverterInputEditing.toggleSign("-30"))
        assertEquals(ConverterInputEditing.Edit("-", 1), ConverterInputEditing.toggleSign(""))
    }
}
