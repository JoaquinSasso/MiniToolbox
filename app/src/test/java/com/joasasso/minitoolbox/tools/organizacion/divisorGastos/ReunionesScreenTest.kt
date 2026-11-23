package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.reunionesDataStore
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ReunionesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var vm: ReunionesViewModel? = null

    @Before
    fun setUp() = runTest {
        app.reunionesDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
    }

    @Test
    fun dialogAyuda_noSeMuestraAlAbrirPantalla() {
        val model = ReunionesViewModel(app).also { vm = it }

        compose.setContent {
            MaterialTheme {
                ReunionesScreen(
                    onBack = {},
                    onCrearReunion = {},
                    onReunionClick = {},
                    vm = model
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.expense_help_title)).assertDoesNotExist()
    }

    @Test
    fun dialogAyuda_seMuestraAlTocarBotonInfoYSeCierraAlConfirmar() {
        val model = ReunionesViewModel(app).also { vm = it }

        compose.setContent {
            MaterialTheme {
                ReunionesScreen(
                    onBack = {},
                    onCrearReunion = {},
                    onReunionClick = {},
                    vm = model
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Información").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.expense_help_title)).assertIsDisplayed()

        compose.onNodeWithText(app.getString(R.string.close)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.expense_help_title)).assertDoesNotExist()
    }

    @Test
    fun crearReunionScreen_dialogAyuda_noSeMuestraAlAbrirPantalla() {
        compose.setContent {
            MaterialTheme {
                CrearReunionScreen(
                    onBack = {},
                    onReunionCreada = {}
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.create_meeting_help_title)).assertDoesNotExist()
    }

    @Test
    fun crearReunionScreen_dialogAyuda_seMuestraAlTocarBotonInfoYSeCierraAlConfirmar() {
        compose.setContent {
            MaterialTheme {
                CrearReunionScreen(
                    onBack = {},
                    onReunionCreada = {}
                )
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Información").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.create_meeting_help_title)).assertIsDisplayed()

        compose.onNodeWithText(app.getString(R.string.close)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.create_meeting_help_title)).assertDoesNotExist()
    }
}
