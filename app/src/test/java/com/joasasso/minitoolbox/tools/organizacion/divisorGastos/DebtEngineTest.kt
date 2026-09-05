package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import com.joasasso.minitoolbox.data.Gasto
import com.joasasso.minitoolbox.data.Reunion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtEngineTest {

    // -------------------------------------------------------------------------
    // Tests de parseTextToCents
    // -------------------------------------------------------------------------

    @Test
    fun parseTextToCents_validaCasosBasicos() {
        assertEquals(1000L, DebtEngine.parseTextToCents("10"))
        assertEquals(0L, DebtEngine.parseTextToCents("0"))
        assertEquals(1050L, DebtEngine.parseTextToCents("10.5"))
        assertEquals(1050L, DebtEngine.parseTextToCents("10,5"))
        assertEquals(1050L, DebtEngine.parseTextToCents("10.50"))
        assertEquals(1005L, DebtEngine.parseTextToCents("10.05"))
        assertEquals(1005L, DebtEngine.parseTextToCents("10,05"))
        assertEquals(150099L, DebtEngine.parseTextToCents("1500.99"))
        assertEquals(150099L, DebtEngine.parseTextToCents("  1500,99  "))
    }

    @Test
    fun parseTextToCents_redondeaTercerDecimal() {
        assertEquals(1056L, DebtEngine.parseTextToCents("10.556"))
        assertEquals(1055L, DebtEngine.parseTextToCents("10.554"))
    }

    @Test
    fun parseTextToCents_rechazaEntradasInvalidas() {
        assertNull(DebtEngine.parseTextToCents(""))
        assertNull(DebtEngine.parseTextToCents("   "))
        assertNull(DebtEngine.parseTextToCents("abc"))
        assertNull(DebtEngine.parseTextToCents("10.5.5"))
        assertNull(DebtEngine.parseTextToCents("10..5"))
    }

    // -------------------------------------------------------------------------
    // Tests de liquidación de deudas
    // -------------------------------------------------------------------------

    @Test
    fun divisionExactaDosPersonas() {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Cena",
            consumidoPor = mapOf("Juan" to 1, "Pedro" to 1),
            aportesCentavos = mapOf("Juan" to 10000L), // $100.00
            aportesIndividuales = mapOf("Juan" to 100.0)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Asado",
            fecha = 123456789L,
            integrantes = listOf("Juan", "Pedro"),
            gastos = listOf(gasto)
        )

        val liquidacion = DebtEngine.calcularLiquidacion(reunion)
        assertEquals(1, liquidacion.size)
        val t = liquidacion.first()
        assertEquals("Pedro", t.deudor)
        assertEquals("Juan", t.acreedor)
        assertEquals(5000L, t.montoCentavos) // $50.00
        assertEquals(50.0, t.monto, 0.001)
    }

    @Test
    fun divisionInexactaTresPersonas_ceroCentavosPerdidos() {
        // $100 entre 3 personas: 10000 / 3 = 3333 residuo 1
        // Un consumidor debe 3334, los otros dos 3333.
        // Total consumido: 3334 + 3333 + 3333 = 10000L exactos.
        val gasto = Gasto(
            id = "g1",
            descripcion = "Pizza",
            consumidoPor = mapOf("Juan" to 1, "Pedro" to 1, "Maria" to 1),
            aportesCentavos = mapOf("Juan" to 10000L),
            aportesIndividuales = mapOf("Juan" to 100.0)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Pizzas",
            fecha = 123456789L,
            integrantes = listOf("Juan", "Pedro", "Maria"),
            gastos = listOf(gasto)
        )

        val liquidacion = DebtEngine.calcularLiquidacion(reunion)

        // Juan pagó 10000L.
        // Deudas totales saldadas a Juan deben sumar exactamente 6666L (lo que le deben) o 6667L según distribución.
        val totalCobrado = liquidacion.filter { it.acreedor == "Juan" }.sumOf { it.montoCentavos }
        val totalPagadoPorJuan = 10000L
        val shareJuan = 3334L // Primer integrante recibe el centavo de residuo
        assertEquals(totalPagadoPorJuan - shareJuan, totalCobrado)

        // Verificamos que no haya transacciones sobrantes ni valores negativos
        assertTrue(liquidacion.all { it.montoCentavos > 0L })
    }

    @Test
    fun pagosCruzadosSeCompensan() {
        // Juan paga $60 para Juan y Pedro (Pedro debe $30)
        // Pedro paga $40 para Juan y Pedro (Juan debe $20)
        // Balance neto: Pedro debe $10 a Juan
        val g1 = Gasto(
            id = "g1",
            descripcion = "Bebidas",
            consumidoPor = mapOf("Juan" to 1, "Pedro" to 1),
            aportesCentavos = mapOf("Juan" to 6000L)
        )
        val g2 = Gasto(
            id = "g2",
            descripcion = "Comida",
            consumidoPor = mapOf("Juan" to 1, "Pedro" to 1),
            aportesCentavos = mapOf("Pedro" to 4000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Salida",
            fecha = 123456789L,
            integrantes = listOf("Juan", "Pedro"),
            gastos = listOf(g1, g2)
        )

        val liquidacion = DebtEngine.calcularLiquidacion(reunion)
        assertEquals(1, liquidacion.size)
        val t = liquidacion.first()
        assertEquals("Pedro", t.deudor)
        assertEquals("Juan", t.acreedor)
        assertEquals(1000L, t.montoCentavos) // $10.00
    }

    @Test
    fun reunionEquilibrada_sinDeudas() {
        // Cada uno paga su consumo
        val g1 = Gasto(
            id = "g1",
            descripcion = "Gasto 1",
            consumidoPor = mapOf("Juan" to 1),
            aportesCentavos = mapOf("Juan" to 5000L)
        )
        val g2 = Gasto(
            id = "g2",
            descripcion = "Gasto 2",
            consumidoPor = mapOf("Pedro" to 1),
            aportesCentavos = mapOf("Pedro" to 5000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Equilibrada",
            fecha = 123456789L,
            integrantes = listOf("Juan", "Pedro"),
            gastos = listOf(g1, g2)
        )

        val liquidacion = DebtEngine.calcularLiquidacion(reunion)
        assertTrue(liquidacion.isEmpty())
    }

    @Test
    fun compatibilidadDatosHistoricosSinAportesCentavos() {
        // Simula datos antiguos que solo tienen aportesIndividuales (Double)
        val gastoViejo = Gasto(
            id = "g_old",
            descripcion = "Viejo",
            consumidoPor = mapOf("Ana" to 1, "Beto" to 1),
            aportesIndividuales = mapOf("Ana" to 75.50)
            // aportesCentavos está vacío por defecto
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Histórica",
            fecha = 123456789L,
            integrantes = listOf("Ana", "Beto"),
            gastos = listOf(gastoViejo)
        )

        assertEquals(7550L, gastoViejo.totalEnCentavos())
        assertEquals(7550L, gastoViejo.aporteEnCentavos("Ana"))
        assertEquals(0L, gastoViejo.aporteEnCentavos("Beto"))

        val liquidacion = DebtEngine.calcularLiquidacion(reunion)
        assertEquals(1, liquidacion.size)
        val t = liquidacion.first()
        assertEquals("Beto", t.deudor)
        assertEquals("Ana", t.acreedor)
        assertEquals(3775L, t.montoCentavos) // $37.75
    }
}
