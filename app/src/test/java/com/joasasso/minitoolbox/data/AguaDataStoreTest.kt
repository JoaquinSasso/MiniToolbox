package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AguaDataStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.aguaDataStore.edit { it.clear() }
    }

    @Test
    fun guardarAguaHoy_persisteYRecuperaConsumoDeHoy() = runTest {
        assertEquals(0, context.flujoAguaHoy().first())

        context.guardarAguaHoy(1750)

        assertEquals(1750, context.flujoAguaHoy().first())
    }

    @Test
    fun guardarAguaFecha_persisteYRecuperaConsumoDeFechaPasada() = runTest {
        val fechaPasada = LocalDate.of(2026, 8, 15)
        assertEquals(0, context.flujoAguaFecha(fechaPasada).first())

        context.guardarAguaFecha(fechaPasada, 2500)

        assertEquals(2500, context.flujoAguaFecha(fechaPasada).first())
    }

    @Test
    fun guardarAguaFecha_actualizaConsumoExistente() = runTest {
        val fecha = LocalDate.of(2026, 9, 1)
        context.guardarAguaFecha(fecha, 1000)
        assertEquals(1000, context.flujoAguaFecha(fecha).first())

        context.guardarAguaFecha(fecha, 2250)
        assertEquals(2250, context.flujoAguaFecha(fecha).first())
    }

    @Test
    fun flujoAguaHoy_alCambiarDeDia_reiniciaEnCeroSinArrastrarConsumoAnterior() = runTest {
        val dia1 = LocalDate.of(2026, 10, 1)
        val dia2 = LocalDate.of(2026, 10, 2)
        val fakeDateFlow = kotlinx.coroutines.flow.MutableStateFlow(dia1)

        context.guardarAguaFecha(dia1, 1800)

        val flujo = context.flujoAguaHoy(fakeDateFlow)
        assertEquals(1800, flujo.first())

        // Rollover de medianoche: cambia el día
        fakeDateFlow.value = dia2

        // Debe emitir 0 para el nuevo día en lugar de arrastrar 1800
        assertEquals(0, flujo.first())

        // Al registrar consumo en el nuevo día, solo tiene lo nuevo
        context.guardarAguaFecha(dia2, 250)
        assertEquals(250, flujo.first())

        // El registro del día anterior se mantiene intacto
        assertEquals(1800, context.flujoAguaFecha(dia1).first())
    }
}
