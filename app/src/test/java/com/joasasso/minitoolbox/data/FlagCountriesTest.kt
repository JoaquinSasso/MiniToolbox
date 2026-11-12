package com.joasasso.minitoolbox.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlagCountriesTest {

    private val all = FlagCountries.all

    @Test
    fun contieneLos206PaisesDelDatasetAnterior() {
        assertEquals(206, all.size)
    }

    @Test
    fun codigosSonIsoAlfa2YUnicos() {
        all.forEach { assertTrue(it.isoCode, it.isoCode.matches(Regex("[A-Z]{2}"))) }
        assertEquals(all.size, all.map { it.isoCode }.toSet().size)
    }

    // El juego distingue las opciones en pantalla por nombre: dos países con el mismo
    // nombre serían indistinguibles.
    @Test
    fun nombresNoVaciosYUnicosEnCadaIdioma() {
        all.forEach {
            assertTrue(it.isoCode, it.nameEs.isNotBlank() && it.nameEn.isNotBlank())
        }
        assertEquals(all.size, all.map { it.nameEs }.toSet().size)
        assertEquals(all.size, all.map { it.nameEn }.toSet().size)
    }

    @Test
    fun banderaSeArmaConLosIndicadoresRegionalesDelCodigo() {
        // U+1F1E6 (A) + U+1F1F7 (R)
        assertEquals("🇦🇷", FlagCountry("AR", "Argentina", "Argentina").flag)
        all.forEach { country ->
            val codePoints = country.flag.codePoints().toArray()
            assertEquals(country.isoCode, 2, codePoints.size)
            assertTrue(country.isoCode, codePoints.all { it in 0x1F1E6..0x1F1FF })
        }
    }
}
