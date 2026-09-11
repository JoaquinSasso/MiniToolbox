package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ReunionesRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.reunionesDataStore.edit { it.clear() }
    }

    @Test
    fun agregarReunion_persisteYRecuperaReunion() = runTest {
        val reunion = Reunion(
            id = "r1",
            nombre = "Asado",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = emptyList()
        )

        ReunionesRepository.agregarReunion(context, reunion)

        val lista = ReunionesRepository.flujoReuniones(context).first()
        assertEquals(1, lista.size)
        assertEquals("r1", lista[0].id)
        assertEquals("Asado", lista[0].nombre)
        assertEquals(listOf("Nico", "Joaco"), lista[0].integrantes)
    }

    @Test
    fun guardarGasto_agregaNuevoGastoYActualizaExistente() = runTest {
        val reunion = Reunion(
            id = "r1",
            nombre = "Cena",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = emptyList()
        )
        ReunionesRepository.agregarReunion(context, reunion)

        val gasto1 = Gasto(
            id = "g1",
            descripcion = "Carne",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 1),
            aportesCentavos = mapOf("Nico" to 5000L)
        )
        ReunionesRepository.guardarGasto(context, "r1", gasto1)

        val listaTrasGasto = ReunionesRepository.flujoReuniones(context).first()
        val rActualizada = listaTrasGasto.first { it.id == "r1" }
        assertEquals(1, rActualizada.gastos.size)
        assertEquals(5000L, rActualizada.gastos[0].totalEnCentavos())

        // Actualizar el mismo gasto
        val gastoModificado = gasto1.copy(
            descripcion = "Carne y Bebida",
            aportesCentavos = mapOf("Nico" to 7000L)
        )
        ReunionesRepository.guardarGasto(context, "r1", gastoModificado)

        val listaTrasEdicion = ReunionesRepository.flujoReuniones(context).first()
        val rEditada = listaTrasEdicion.first { it.id == "r1" }
        assertEquals(1, rEditada.gastos.size)
        assertEquals("Carne y Bebida", rEditada.gastos[0].descripcion)
        assertEquals(7000L, rEditada.gastos[0].totalEnCentavos())
    }

    @Test
    fun eliminarGasto_eliminaGastoDeReunion() = runTest {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Pizza",
            consumidoPor = mapOf("Nico" to 1),
            aportesCentavos = mapOf("Nico" to 2000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Pizzas",
            fecha = 1000L,
            integrantes = listOf("Nico"),
            gastos = listOf(gasto)
        )
        ReunionesRepository.agregarReunion(context, reunion)

        ReunionesRepository.eliminarGasto(context, "r1", "g1")

        val r = ReunionesRepository.flujoReuniones(context).first().first { it.id == "r1" }
        assertTrue(r.gastos.isEmpty())
    }

    @Test
    fun actualizarIntegrante_renombraEnCascadaEnReunionYGastos() = runTest {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Helado",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 1),
            aportesCentavos = mapOf("Nico" to 3000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Postre",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = listOf(gasto)
        )
        ReunionesRepository.agregarReunion(context, reunion)

        ReunionesRepository.actualizarIntegrante(context, "r1", "Nico", "Nicolas")

        val r = ReunionesRepository.flujoReuniones(context).first().first { it.id == "r1" }
        assertTrue(r.integrantes.contains("Nicolas"))
        assertFalse(r.integrantes.contains("Nico"))

        val gastoActualizado = r.gastos.first()
        assertEquals(3000L, gastoActualizado.aportesCentavos["Nicolas"])
        assertNull(gastoActualizado.aportesCentavos["Nico"])
        assertEquals(1, gastoActualizado.consumidoPor["Nicolas"])
        assertNull(gastoActualizado.consumidoPor["Nico"])
    }

    @Test
    fun eliminarIntegrante_eliminaEnCascadaEnReunionYGastos() = runTest {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Bebidas",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 1),
            aportesCentavos = mapOf("Nico" to 1000L, "Joaco" to 2000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Fiesta",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = listOf(gasto)
        )
        ReunionesRepository.agregarReunion(context, reunion)

        ReunionesRepository.eliminarIntegrante(context, "r1", "Joaco")

        val r = ReunionesRepository.flujoReuniones(context).first().first { it.id == "r1" }
        assertEquals(listOf("Nico"), r.integrantes)
        val g = r.gastos.first()
        assertFalse(g.consumidoPor.containsKey("Joaco"))
        assertFalse(g.aportesCentavos.containsKey("Joaco"))
        assertEquals(1000L, g.aportesCentavos["Nico"])
    }

    @Test
    fun agregarIntegrante_agregaSinDuplicar() = runTest {
        val reunion = Reunion(
            id = "r1",
            nombre = "Peli",
            fecha = 1000L,
            integrantes = listOf("Nico"),
            gastos = emptyList()
        )
        ReunionesRepository.agregarReunion(context, reunion)

        ReunionesRepository.agregarIntegrante(context, "r1", "Lucas")
        // Intento de agregar duplicado
        ReunionesRepository.agregarIntegrante(context, "r1", "Lucas")

        val r = ReunionesRepository.flujoReuniones(context).first().first { it.id == "r1" }
        assertEquals(listOf("Nico", "Lucas"), r.integrantes)
    }

    @Test
    fun eliminarReunion_remueveReunionPorId() = runTest {
        val r1 = Reunion(id = "r1", nombre = "R1", fecha = 1L, integrantes = emptyList(), gastos = emptyList())
        val r2 = Reunion(id = "r2", nombre = "R2", fecha = 2L, integrantes = emptyList(), gastos = emptyList())
        ReunionesRepository.agregarReunion(context, r1)
        ReunionesRepository.agregarReunion(context, r2)

        ReunionesRepository.eliminarReunion(context, "r1")

        val lista = ReunionesRepository.flujoReuniones(context).first()
        assertEquals(1, lista.size)
        assertEquals("r2", lista[0].id)
    }
}
