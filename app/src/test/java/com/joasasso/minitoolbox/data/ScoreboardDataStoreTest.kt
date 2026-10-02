package com.joasasso.minitoolbox.data

import Equipo
import MarcadorPrefs
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ScoreboardDataStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("marcador_prefs", Context.MODE_PRIVATE).edit { clear() }
    }

    @Test
    fun loadAll_withLegacyJsonWithoutId_assignsUniqueUuids() {
        val legacyJson = """
            [
                {"nombre":"Equipo Rojo","puntos":10,"color":-65536},
                {"nombre":"Equipo Azul","puntos":5,"color":-16776961}
            ]
        """.trimIndent()
        context.getSharedPreferences("marcador_prefs", Context.MODE_PRIVATE)
            .edit { putString("equipos", legacyJson) }

        val loaded = MarcadorPrefs.loadAll(context)
        assertEquals(2, loaded.size)

        assertEquals("Equipo Rojo", loaded[0].nombre)
        assertEquals(10, loaded[0].puntos)
        assertNotNull(loaded[0].id)
        assertTrue(loaded[0].id.isNotEmpty())

        assertEquals("Equipo Azul", loaded[1].nombre)
        assertEquals(5, loaded[1].puntos)
        assertNotNull(loaded[1].id)
        assertTrue(loaded[1].id.isNotEmpty())

        assertNotEquals(loaded[0].id, loaded[1].id)
    }

    @Test
    fun saveAll_andLoadAll_preservesAssignedId() {
        val original = listOf(
            Equipo(nombre = "Alfa", puntos = 3, color = Color.Red, id = "id-alfa-123"),
            Equipo(nombre = "Beta", puntos = 7, color = Color.Blue, id = "id-beta-456")
        )

        MarcadorPrefs.saveAll(context, original)
        val reloaded = MarcadorPrefs.loadAll(context)

        assertEquals(2, reloaded.size)
        assertEquals("id-alfa-123", reloaded[0].id)
        assertEquals("Alfa", reloaded[0].nombre)
        assertEquals(3, reloaded[0].puntos)
        assertEquals(Color.Red.toArgb(), reloaded[0].color.toArgb())

        assertEquals("id-beta-456", reloaded[1].id)
        assertEquals("Beta", reloaded[1].nombre)
        assertEquals(7, reloaded[1].puntos)
        assertEquals(Color.Blue.toArgb(), reloaded[1].color.toArgb())
    }
}
