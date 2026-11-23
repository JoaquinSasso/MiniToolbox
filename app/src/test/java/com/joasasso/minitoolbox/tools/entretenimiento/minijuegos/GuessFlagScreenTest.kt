package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.FlagCountry
import com.joasasso.minitoolbox.data.FlagGameRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class GuessFlagScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var vm: GuessFlagViewModel? = null

    private val fakeRepo = object : FlagGameRepository {
        override fun bestScore(context: Context): Flow<Int> = flowOf(0)
        override suspend fun recordScore(context: Context, score: Int) = Unit
    }

    private val testCountries = listOf(
        FlagCountry("ar", "Argentina", "Argentina"),
        FlagCountry("br", "Brasil", "Brazil"),
        FlagCountry("cl", "Chile", "Chile"),
        FlagCountry("uy", "Uruguay", "Uruguay")
    )

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
    }

    @Test
    fun dialogAyuda_noSeMuestraAlAbrirPantalla() {
        val model = GuessFlagViewModel(app, fakeRepo, testCountries).also { vm = it }

        compose.setContent {
            MaterialTheme {
                AdivinaBanderaScreen(onBack = {}, viewModel = model)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.flag_info_title)).assertDoesNotExist()
    }

    @Test
    fun dialogAyuda_seMuestraAlTocarBotonInfoYSeCierraAlConfirmar() {
        val model = GuessFlagViewModel(app, fakeRepo, testCountries).also { vm = it }

        compose.setContent {
            MaterialTheme {
                AdivinaBanderaScreen(onBack = {}, viewModel = model)
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Información").performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.flag_info_title)).assertIsDisplayed()

        compose.onNodeWithText(app.getString(R.string.close)).performClick()
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.flag_info_title)).assertDoesNotExist()
    }
}
