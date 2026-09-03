package com.joasasso.minitoolbox.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el cálculo local de retención.
 *
 * Toda la lógica que decide en qué categoría cae un dispositivo vive acá y es pura, así
 * que se puede testear sin Android. Eso importa porque los cortes de los buckets son un
 * contrato: una vez publicados, cambiarlos parte la serie histórica en dos.
 */
class RetentionBucketsTest {

    @Test
    fun `los cortes de intensidad son los publicados`() {
        val expected = mapOf(
            0 to "d1", 1 to "d1",
            2 to "d2_3", 3 to "d2_3",
            4 to "d4_7", 7 to "d4_7",
            8 to "d8_14", 14 to "d8_14",
            15 to "d15_28", 28 to "d15_28"
        )
        for ((days, bucket) in expected) {
            assertEquals("intensidad con $days días activos", bucket, RetentionBuckets.intensityBucket(days))
        }
    }

    @Test
    fun `los cortes de antiguedad son los publicados`() {
        val expected = mapOf(
            0 to "age0_6", 6 to "age0_6",
            7 to "age7_29", 29 to "age7_29",
            30 to "age30_89", 89 to "age30_89",
            90 to "age90_179", 179 to "age90_179",
            180 to "age180p", 900 to "age180p"
        )
        for ((days, bucket) in expected) {
            assertEquals("antigüedad de $days días", bucket, RetentionBuckets.ageBucket(days))
        }
    }

    @Test
    fun `la cardinalidad del cruce esta acotada y cumple el contrato de claves`() {
        val keys = mutableSetOf<String>()
        for (age in listOf(0, 6, 7, 29, 30, 89, 90, 179, 180, 400)) {
            for (active in 0..28) {
                val key = RetentionBuckets.key(age, active)
                keys += key
                assertTrue(
                    "'$key' no cumple el contrato del backend",
                    MetricsContract.isValidKey(key)
                )
            }
        }
        assertEquals("5 tramos de antigüedad x 5 de intensidad", 25, keys.size)
    }

    @Test
    fun `la ventana cubre 28 dias inclusive`() {
        assertEquals("2026-08-07", RetentionBuckets.windowStart("2026-09-03"))
    }

    @Test
    fun `daysBetween cuenta dias calendario`() {
        assertEquals(33, RetentionBuckets.daysBetween("2026-08-01", "2026-09-03"))
        assertEquals(0, RetentionBuckets.daysBetween("2026-09-03", "2026-09-03"))
    }

    @Test
    fun `una fecha ilegible cuenta como dispositivo nuevo`() {
        // Caso conservador: sin historial legible, el dispositivo no debe aparecer
        // como veterano y contaminar los tramos altos.
        assertEquals(0, RetentionBuckets.daysBetween("basura", "2026-09-03"))
        assertEquals("age0_6", RetentionBuckets.ageBucket(RetentionBuckets.daysBetween("", "2026-09-03")))
    }

    @Test
    fun `los dias fuera de la ventana se podan`() {
        val previous = listOf("2026-07-01", "2026-08-10", "2026-09-02")
        val updated = RetentionBuckets.updateActiveDays(previous, "2026-09-03")

        assertTrue("un día de julio no puede seguir en la ventana", "2026-07-01" !in updated)
        assertTrue("2026-08-10" in updated)
        assertTrue("el día de hoy siempre entra", "2026-09-03" in updated)
    }

    @Test
    fun `marcar el mismo dia dos veces no cambia nada`() {
        // El worker o un reinicio pueden disparar el cálculo más de una vez por día;
        // si sumara, la intensidad quedaría inflada.
        val first = RetentionBuckets.updateActiveDays(emptyList(), "2026-09-03")
        val second = RetentionBuckets.updateActiveDays(first, "2026-09-03")

        assertEquals(listOf("2026-09-03"), first)
        assertEquals(first, second)
    }

    @Test
    fun `no se aceptan dias futuros`() {
        val updated = RetentionBuckets.updateActiveDays(listOf("2027-01-01"), "2026-09-03")
        assertTrue("una fecha futura indicaría reloj cambiado", "2027-01-01" !in updated)
    }

    @Test
    fun `un dispositivo nuevo que abre todos los dias sube de tramo`() {
        var days = emptyList<String>()
        // Simula ocho días consecutivos desde el 2026-09-01.
        for (d in 1..8) {
            days = RetentionBuckets.updateActiveDays(days, "2026-09-%02d".format(d))
        }
        assertEquals(8, days.size)
        assertEquals("d8_14", RetentionBuckets.intensityBucket(days.size))
        assertEquals("age0_6", RetentionBuckets.ageBucket(RetentionBuckets.daysBetween("2026-09-01", "2026-09-07")))
        assertEquals("age7_29", RetentionBuckets.ageBucket(RetentionBuckets.daysBetween("2026-09-01", "2026-09-08")))
    }
}