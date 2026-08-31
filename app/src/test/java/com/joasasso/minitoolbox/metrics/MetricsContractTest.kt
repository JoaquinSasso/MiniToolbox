package com.joasasso.minitoolbox.metrics

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica que [MetricsContract] cumple exactamente el mismo contrato que
 * `backend/functions/src/validate.ts`.
 *
 * Las fixtures viven en `metrics-fixtures/keys.json`, en la raíz del repositorio, y las
 * consumen tanto este test como los del backend. Si alguien cambia el regex o la
 * normalización de un lado y no del otro, uno de los dos tests falla.
 *
 * Ese desfasaje es exactamente lo que causó el incidente original: el contrato existía
 * sólo en el backend y el cliente no lo conocía.
 */
class MetricsContractTest {

    private fun fixtures(): JSONObject {
        val stream = javaClass.getResourceAsStream("/keys.json")
            ?: error(
                "No se encontró keys.json en el classpath de tests. " +
                        "Verificar que app/build.gradle.kts incluya ../metrics-fixtures " +
                        "como directorio de recursos del sourceSet de test."
            )
        return JSONObject(stream.bufferedReader().use { it.readText() })
    }

    @Test
    fun `las claves validas de las fixtures pasan el contrato`() {
        val valid = fixtures().getJSONArray("valid")
        for (i in 0 until valid.length()) {
            val key = valid.getString(i)
            assertTrue("'$key' debería ser una clave válida", MetricsContract.isValidKey(key))
        }
    }

    @Test
    fun `la normalizacion coincide con la del backend`() {
        val cases = fixtures().getJSONArray("normalize")
        for (i in 0 until cases.length()) {
            val case = cases.getJSONObject(i)
            val input = case.getString("in")
            val expected = if (case.isNull("out")) null else case.getString("out")

            val actual = MetricsContract.normalizeKey(input)
            assertEquals("normalizeKey(\"$input\")", expected, actual)
        }
    }

    @Test
    fun `toda clave normalizada cumple el contrato`() {
        val cases = fixtures().getJSONArray("normalize")
        for (i in 0 until cases.length()) {
            val input = cases.getJSONObject(i).getString("in")
            val normalized = MetricsContract.normalizeKey(input)
            if (normalized != null) {
                assertTrue(
                    "normalizeKey(\"$input\") devolvió '$normalized', que no cumple KEY_RE",
                    MetricsContract.isValidKey(normalized)
                )
            }
        }
    }

    @Test
    fun `los dias de las fixtures se validan correctamente`() {
        val days = fixtures().getJSONObject("days")

        val valid = days.getJSONArray("valid")
        for (i in 0 until valid.length()) {
            val day = valid.getString(i)
            assertTrue("'$day' debería ser un día válido", MetricsContract.isValidDay(day))
        }

        val invalid = days.getJSONArray("invalid")
        for (i in 0 until invalid.length()) {
            val day = invalid.getString(i)
            assertTrue("'$day' no debería ser un día válido", !MetricsContract.isValidDay(day))
        }
    }

    // -----------------------------------------------------------------
    // Propiedades del saneo de contadores
    // -----------------------------------------------------------------

    @Test
    fun `las colisiones tras normalizar suman en lugar de pisarse`() {
        val result = MetricsContract.sanitizeCounters(
            mapOf(
                "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b" to 3,
                "pomodoro_detail" to 1,
                "water" to 5
            )
        )

        assertEquals(4, result.clean["pomodoro_detail"])
        assertEquals(5, result.clean["water"])
        assertEquals(1, result.fixedKeys)
    }

    @Test
    fun `los contadores negativos se descartan`() {
        val result = MetricsContract.sanitizeCounters(mapOf("water" to 5, "roto" to -2))

        assertEquals(5, result.clean["water"])
        assertNull(result.clean["roto"])
        assertEquals(1, result.droppedKeys)
    }

    @Test
    fun `sanear dos veces no cambia nada la segunda vez`() {
        val first = MetricsContract.sanitizeCounters(
            mapOf("a/b" to 2, "water" to 5, "///" to 9)
        )
        val second = MetricsContract.sanitizeCounters(first.clean)

        assertEquals(first.clean, second.clean)
        assertTrue("la segunda pasada no debería corregir nada", !second.changed)
    }

    @Test
    fun `sanitizeDayNested descarta los dias mal formados`() {
        val (clean, _) = MetricsContract.sanitizeDayNested(
            mapOf(
                "2026-08-30" to mapOf("a/b" to 2),
                "BAD" to mapOf("water" to 9)
            )
        )

        assertEquals(setOf("2026-08-30"), clean.keys)
        assertEquals(2, clean["2026-08-30"]?.get("a_b"))
    }

    @Test
    fun `una clave de 65 caracteres se recorta a 64`() {
        val long = "a".repeat(65)
        val normalized = MetricsContract.normalizeKey(long)

        assertEquals(MetricsContract.MAX_KEY_LEN, normalized?.length)
        assertTrue(MetricsContract.isValidKey(normalized!!))
    }
}