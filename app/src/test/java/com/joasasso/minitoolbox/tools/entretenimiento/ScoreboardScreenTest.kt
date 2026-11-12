package com.joasasso.minitoolbox.tools.entretenimiento

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ScoreboardScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun dialogAyuda_noSeMuestraAlAbrirPantalla() {
        compose.setContent {
            MaterialTheme {
                MarcadorEquiposScreen(onBack = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.marcador_help_title)).assertDoesNotExist()
    }

    @Test
    fun dialogAyuda_seMuestraAlTocarBotonInfoYSeCierraAlConfirmar() {
        compose.setContent {
            MaterialTheme {
                MarcadorEquiposScreen(onBack = {})
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Información").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.marcador_help_title)).assertIsDisplayed()

        compose.onNodeWithText(app.getString(R.string.close)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.marcador_help_title)).assertDoesNotExist()
    }
}
