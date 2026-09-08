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
}
