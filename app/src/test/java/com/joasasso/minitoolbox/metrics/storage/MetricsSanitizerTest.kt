package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.joasasso.minitoolbox.TestApplication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Verifica [MetricsSanitizer] contra un DataStore real.
 *
 * La lógica pura de normalización ya está cubierta por MetricsContractTest; lo que se
 * prueba acá es el pegamento con el almacenamiento, que es donde estuvo el bug: qué mapas
 * se sanean, si el par acumulado/enviado se trata igual, y qué pasa con el lote congelado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class MetricsSanitizerTest {

    private lateinit var context: Context

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun today(): String = dayFmt.format(Date())

    private fun daysAgo(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return dayFmt.format(cal.time)
    }

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        // El DataStore es un singleton por proceso: se limpia entre tests.
        context.metricsDataStore.edit { it.clear() }
    }

    private suspend fun toolUseMap(): Map<String, Map<String, Int>> =
        JsonUtils.fromDayNestedIntMap(
            context.metricsDataStore.data.first()[MetricsKeys.TOOL_USE_BY_DAY_JSON]
        )

    @Test
    fun `normaliza claves de ruta conservando los conteos`() = runTest {
        val day = today()
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(
                mapOf(
                    day to mapOf(
                        "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b" to 3,
                        "water" to 5
                    )
                )
            )
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val tools = toolUseMap().getValue(day)
        assertEquals(3, tools["pomodoro_detail"])
        assertEquals(5, tools["water"])
        assertEquals("no se debe perder ningún conteo", 8, tools.values.sum())
    }

    @Test
    fun `el delta sigue cuadrando tras sanear acumulado y enviado`() = runTest {
        val day = today()
        val poisoned = "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b"

        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] =
                JsonUtils.toDayNestedIntMap(mapOf(day to mapOf(poisoned to 7)))
            it[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON] =
                JsonUtils.toDayNestedIntMap(mapOf(day to mapOf(poisoned to 4)))
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val prefs = context.metricsDataStore.data.first()
        val acc = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val sent = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])

        val accCount = acc.getValue(day).getValue("pomodoro_detail")
        val sentCount = sent.getValue(day).getValue("pomodoro_detail")

        assertEquals(
            "si el par acumulado/enviado no recibe la misma transformación, el delta se rompe",
            3,
            accCount - sentCount
        )
    }

    @Test
    fun `descarta los dias mal formados sin tocar los validos`() = runTest {
        val day = today()
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(
                mapOf(
                    day to mapOf("water" to 2),
                    "BAD-DAY" to mapOf("water" to 99),
                    "2026-8-3" to mapOf("water" to 99)
                )
            )
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val tools = toolUseMap()
        assertEquals(setOf(day), tools.keys)
        assertEquals(2, tools.getValue(day)["water"])
    }

    @Test
    fun `poda los dias mas viejos que la ventana de retencion`() = runTest {
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(
                mapOf(
                    today() to mapOf("water" to 1),
                    daysAgo(30) to mapOf("water" to 1),
                    daysAgo(120) to mapOf("water" to 1)
                )
            )
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val tools = toolUseMap()
        assertTrue("el día reciente debe sobrevivir", tools.containsKey(today()))
        assertTrue("30 días atrás debe sobrevivir", tools.containsKey(daysAgo(30)))
        assertTrue("120 días atrás debe podarse", !tools.containsKey(daysAgo(120)))
    }

    @Test
    fun `sanear dos veces no cambia nada la segunda vez`() = runTest {
        val day = today()
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(
                mapOf(day to mapOf("a/b" to 2, "water" to 5))
            )
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)
        val afterFirst = toolUseMap()

        val second = MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)
        val afterSecond = toolUseMap()

        assertEquals(afterFirst, afterSecond)
        assertTrue("la segunda pasada no debería corregir nada", !second.changed)
    }

    @Test
    fun `descartar el pendiente conserva el batch id`() = runTest {
        context.metricsDataStore.edit {
            it[MetricsKeys.PENDING_BATCH_ID] = "batch-123"
            it[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = """{"batch_id":"batch-123"}"""
            it[MetricsKeys.PENDING_BATCH_CREATED_AT] = 1_000L
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = true)

        val prefs = context.metricsDataStore.data.first()
        assertEquals(
            "reusar el batch_id evita el doble conteo si el backend sí había persistido el lote",
            "batch-123",
            prefs[MetricsKeys.PENDING_BATCH_ID]
        )
        assertEquals("", prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON])
        assertEquals(0L, prefs[MetricsKeys.PENDING_BATCH_CREATED_AT])
    }

    @Test
    fun `runIfNeeded corre una sola vez por version de esquema`() = runTest {
        val first = MetricsSanitizer.runIfNeeded(context)
        assertNotNull("la primera pasada debe ejecutarse", first)

        val prefs = context.metricsDataStore.data.first()
        assertEquals(MetricsSanitizer.SCHEMA_VERSION, prefs[MetricsKeys.SCHEMA_VERSION])

        val second = MetricsSanitizer.runIfNeeded(context)
        assertNull("la segunda pasada debe ser un no-op", second)
    }

    @Test
    fun `acumula el contador de claves saneadas`() = runTest {
        val day = today()
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(
                mapOf(day to mapOf("a/b" to 1, "c/d" to 1))
            )
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val total = context.metricsDataStore.data.first()[MetricsKeys.SANITIZED_KEYS_TOTAL] ?: 0
        assertEquals(2, total)
    }

    @Test
    fun `sanea tambien los mapas de widgets y anuncios`() = runTest {
        val day = today()
        context.metricsDataStore.edit {
            it[MetricsKeys.WIDGET_USE_BY_DAY_JSON] =
                JsonUtils.toDayNestedIntMap(mapOf(day to mapOf("widget/agua" to 4)))
            it[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON] =
                JsonUtils.toDayNestedIntMap(mapOf(day to mapOf("banner/top" to 2)))
        }

        MetricsSanitizer.sanitizeNow(context, discardPendingPayload = false)

        val prefs = context.metricsDataStore.data.first()
        val widgets = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.WIDGET_USE_BY_DAY_JSON])
        val ads = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])

        assertEquals(4, widgets.getValue(day)["widget_agua"])
        assertEquals(2, ads.getValue(day)["banner_top"])
    }
}