package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ToolOnboardingDataStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.toolOnboardingDataStore.edit { it.clear() }
    }

    @Test
    fun flujoOnboardingVisto_inicialmenteDevuelveFalse() = runTest {
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.LIGHT_SENSOR).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.RULER).first())
    }

    @Test
    fun marcarOnboardingVisto_actualizaEstadoATrue() = runTest {
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())

        context.marcarOnboardingVisto(ToolOnboardingKeys.COMPASS)

        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
    }

    @Test
    fun marcarOnboardingVisto_mantieneIndependenciaEntreHerramientas() = runTest {
        context.marcarOnboardingVisto(ToolOnboardingKeys.COMPASS)

        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.LIGHT_SENSOR).first())
    }

    @Test
    fun resetOnboarding_paraUnaHerramienta_soloReiniciaEsaHerramienta() = runTest {
        context.marcarOnboardingVisto(ToolOnboardingKeys.COMPASS)
        context.marcarOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL)

        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())

        context.resetOnboarding(ToolOnboardingKeys.COMPASS)

        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())
    }

    @Test
    fun resetOnboarding_sinParametro_reiniciaTodasLasHerramientas() = runTest {
        context.marcarOnboardingVisto(ToolOnboardingKeys.COMPASS)
        context.marcarOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL)
        context.marcarOnboardingVisto(ToolOnboardingKeys.LIGHT_SENSOR)

        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())
        assertTrue(context.flujoOnboardingVisto(ToolOnboardingKeys.LIGHT_SENSOR).first())

        context.resetOnboarding()

        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.COMPASS).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.BUBBLE_LEVEL).first())
        assertFalse(context.flujoOnboardingVisto(ToolOnboardingKeys.LIGHT_SENSOR).first())
    }
}
