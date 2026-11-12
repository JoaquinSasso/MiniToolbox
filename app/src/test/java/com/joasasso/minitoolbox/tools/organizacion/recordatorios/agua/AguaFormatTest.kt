package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AguaFormatTest {

    private val es = Locale.forLanguageTag("es-AR")

    @Test
    fun litros_quitaCerosSobrantesYUsaSeparadorDelIdioma() {
        assertEquals("2", AguaFormat.litros(2000, es))
        assertEquals("1,25", AguaFormat.litros(1250, es))
        assertEquals("0,5", AguaFormat.litros(500, es))
        assertEquals("1.25", AguaFormat.litros(1250, Locale.US))
    }

    @Test
    fun litros_redondeaAlCentilitro() {
        assertEquals("2", AguaFormat.litros(1995, es))
        assertEquals("0,29", AguaFormat.litros(285, es))
    }

    @Test
    fun usaLitros_desdeUnLitro() {
        assertFalse(AguaFormat.usaLitros(999))
        assertTrue(AguaFormat.usaLitros(1000))
    }
}
