package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AguaMathTest {

    @Test
    fun objetivoAlcanzado_sieteVasosDe285mlParaMetaDe2000ml_esTrue() {
        // Caso reportado: 2000 / 7 = 285.71 -> usuario configura 285 ml y toma 7 vasos (1995 ml)
        assertTrue(AguaMath.objetivoAlcanzado(consumidoML = 1995, objetivoML = 2000, mlPorVaso = 285))
    }

    @Test
    fun objetivoAlcanzado_seisVasosDe285mlParaMetaDe2000ml_esFalse() {
        // 6 * 285 = 1710 ml (1.71 L < 2.00 L)
        assertFalse(AguaMath.objetivoAlcanzado(consumidoML = 1710, objetivoML = 2000, mlPorVaso = 285))
    }

    @Test
    fun objetivoAlcanzado_sieteVasosDe280mlParaMetaDe2000ml_esFalse() {
        // 280 ml no proviene de dividir 2000 en 7, y 7 * 280 = 1960 ml (1.96 L < 2.00 L)
        assertFalse(AguaMath.objetivoAlcanzado(consumidoML = 1960, objetivoML = 2000, mlPorVaso = 280))
    }

    @Test
    fun objetivoAlcanzado_redondeoACentilitrosAlcanzaMeta_esTrue() {
        // 1995 ml se formatea como "2.00 L", igual que 2000 ml
        assertTrue(AguaMath.objetivoAlcanzado(consumidoML = 1995, objetivoML = 2000, mlPorVaso = 250))
        // 1994 ml se formatea como "1.99 L", menor que 2000 ml con vaso estándar de 250 ml
        assertFalse(AguaMath.objetivoAlcanzado(consumidoML = 1994, objetivoML = 2000, mlPorVaso = 250))
    }

    @Test
    fun objetivoAlcanzado_divisionTruncadaConMasVasos_esTrueAlCompletarVasos() {
        // 2000 / 11 = 181.81 -> truncado a 181 ml; 11 * 181 = 1991 ml
        assertTrue(AguaMath.objetivoAlcanzado(consumidoML = 1991, objetivoML = 2000, mlPorVaso = 181))
        assertFalse(AguaMath.objetivoAlcanzado(consumidoML = 1810, objetivoML = 2000, mlPorVaso = 181))
    }

    @Test
    fun calcularMlPorVasoSegunVasos_redondeaYRespetaLimites() {
        assertEquals(286, AguaMath.calcularMlPorVasoSegunVasos(objetivoML = 2000, vasos = 7))
        assertEquals(250, AguaMath.calcularMlPorVasoSegunVasos(objetivoML = 2000, vasos = 8))
        assertEquals(AguaMath.MIN_ML_POR_VASO, AguaMath.calcularMlPorVasoSegunVasos(objetivoML = 2000, vasos = 100))
        assertEquals(AguaMath.MAX_ML_POR_VASO, AguaMath.calcularMlPorVasoSegunVasos(objetivoML = 3000, vasos = 1))
    }

    @Test
    fun calcularVasosObjetivo_noTruncaPorRedondeoDeMlPorVaso() {
        // Con 286 ml (redondeo de 2000 / 7 = 285.71), 2000 / 286 = 6.993 -> debe dar 7 vasos, no 6
        assertEquals(7, AguaMath.calcularVasosObjetivo(objetivoML = 2000, mlPorVaso = 286))
        // Con 285 ml (truncado de 2000 / 7), 2000 / 285 = 7.017 -> debe dar 7 vasos
        assertEquals(7, AguaMath.calcularVasosObjetivo(objetivoML = 2000, mlPorVaso = 285))
    }

    @Test
    fun calcularVasosConsumidos_calculaCorrectamentePara285y286() {
        assertEquals(7, AguaMath.calcularVasosConsumidos(consumidoML = 1995, mlPorVaso = 285))
        assertEquals(7, AguaMath.calcularVasosConsumidos(consumidoML = 2002, mlPorVaso = 286))
        assertEquals(7, AguaMath.calcularVasosConsumidos(consumidoML = 2000, mlPorVaso = 286))
        assertEquals(0, AguaMath.calcularVasosConsumidos(consumidoML = 0, mlPorVaso = 285))
    }
}
