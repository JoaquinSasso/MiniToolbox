package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.FlagGameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Usa un repositorio en memoria y no el DataStore real (que tiene su propio test en
 * FlagGameRepositoryTest): el DataStore escribe en Dispatchers.IO, en tiempo real, y mientras
 * el test lo espera runTest avanzaría el reloj virtual y el temporizador encadenaría rondas.
 * Por el mismo motivo no se usa advanceUntilIdle: las rondas no terminan nunca.
 *
 * Los tests con tiempo virtual usan runVmTest: al terminar el cuerpo, runTest sigue
 * ejecutando las tareas pendientes del scheduler, y el temporizador del ViewModel genera
 * una ronda tras otra. Cancelar viewModelScope en @After llega tarde: corre después de runTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class GuessFlagViewModelTest {

    private class FakeFlagGameRepository(initialBest: Int) : FlagGameRepository {
        val best = MutableStateFlow(initialBest)
        val recorded = mutableListOf<Int>()

        override fun bestScore(context: Context): Flow<Int> = best

        override suspend fun recordScore(context: Context, score: Int) {
            recorded += score
            if (score > best.value) best.value = score
        }
    }

    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: GuessFlagViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun runVmTest(body: suspend TestScope.() -> Unit): TestResult =
        runTest(timeout = 10.seconds) {
            try {
                body()
            } finally {
                vm?.viewModelScope?.cancel()
            }
        }

    private fun crear(best: Int = 0): Pair<GuessFlagViewModel, FakeFlagGameRepository> {
        val repo = FakeFlagGameRepository(best)
        val model = GuessFlagViewModel(app, repo, random = Random(42))
        vm = model
        return model to repo
    }

    private fun GuessFlagViewModel.respuestaCorrecta() = uiState.value.question!!.isoCode

    private fun GuessFlagViewModel.respuestaIncorrecta() =
        uiState.value.options.first { it.isoCode != respuestaCorrecta() }.isoCode

    @Test
    fun init_arrancaRondaConRecordGuardado() {
        val (model, _) = crear(best = 5)
        val state = model.uiState.value

        assertFalse(state.isLoading)
        assertEquals(5, state.record)
        assertEquals(0, state.score)
        assertEquals(FlagQuiz.OPTION_COUNT, state.options.size)
        assertTrue(state.question in state.options)
        assertEquals(GuessFlagViewModel.ROUND_TENTHS, state.tenthsLeft)
        assertFalse(state.revealing)
    }

    @Test
    fun answer_correctaSinRecord_sumaPuntoYEmiteCorrect() = runVmTest {
        val (model, repo) = crear(best = 5)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }

        val iso = model.respuestaCorrecta()
        model.answer(iso)

        val state = model.uiState.value
        assertEquals(GuessFlagEvent.Correct, event.await())
        assertEquals(1, state.score)
        assertEquals(5, state.record)
        assertNull(state.lastResult)
        assertTrue(state.revealing)
        assertEquals(iso, state.selectedIso)
        assertFalse(state.showError)
        assertTrue(repo.recorded.isEmpty())
    }

    @Test
    fun answer_correctaQueSuperaElRecord_loGuarda() {
        val (model, repo) = crear(best = 0)

        model.answer(model.respuestaCorrecta())

        val state = model.uiState.value
        assertEquals(FlagRoundResult.NEW_RECORD, state.lastResult)
        assertEquals(1, state.record)
        assertEquals(listOf(1), repo.recorded)
    }

    @Test
    fun answer_incorrecta_reiniciaPuntajeYEmiteWrong() = runVmTest {
        val (model, repo) = crear(best = 0)
        model.answer(model.respuestaCorrecta())
        // El Correct de la primera respuesta queda en el Channel: se consume para que el
        // siguiente first() reciba el evento de la respuesta incorrecta.
        assertEquals(GuessFlagEvent.Correct, model.events.first())
        advanceTimeBy(GuessFlagViewModel.REVEAL_MS + 1)
        val event = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }

        model.answer(model.respuestaIncorrecta())

        val state = model.uiState.value
        assertEquals(GuessFlagEvent.Wrong, event.await())
        assertEquals(0, state.score)
        assertEquals(1, state.record)
        assertEquals(FlagRoundResult.WRONG, state.lastResult)
        assertTrue(state.showError)
        assertEquals(listOf(1), repo.recorded)
    }

    @Test
    fun answer_duranteLaPausa_seIgnora() {
        val (model, _) = crear()
        val correcta = model.respuestaCorrecta()
        model.answer(correcta)
        val antes = model.uiState.value

        model.answer(model.respuestaIncorrecta())

        assertEquals(antes, model.uiState.value)
    }

    @Test
    fun trasLaPausa_arrancaRondaNueva() = runVmTest {
        val (model, _) = crear()
        model.answer(model.respuestaCorrecta())

        advanceTimeBy(GuessFlagViewModel.REVEAL_MS + 1)

        val state = model.uiState.value
        assertFalse(state.revealing)
        assertNull(state.selectedIso)
        assertEquals(GuessFlagViewModel.ROUND_TENTHS, state.tenthsLeft)
        assertEquals(1, state.score)
    }

    @Test
    fun temporizador_descuentaYAlAgotarseReiniciaElPuntaje() = runVmTest {
        val (model, _) = crear()
        model.answer(model.respuestaCorrecta())
        advanceTimeBy(GuessFlagViewModel.REVEAL_MS + 1)
        val pregunta = model.uiState.value.question

        advanceTimeBy(GuessFlagViewModel.TICK_MS * 30 + 1)
        assertEquals(GuessFlagViewModel.ROUND_TENTHS - 30, model.uiState.value.tenthsLeft)

        advanceTimeBy(GuessFlagViewModel.TICK_MS * 70)
        val state = model.uiState.value
        assertEquals(0, state.tenthsLeft)
        assertEquals(FlagRoundResult.TIMEOUT, state.lastResult)
        assertEquals(0, state.score)
        assertTrue(state.revealing)
        assertTrue(state.showError)
        assertEquals(pregunta, state.question)
    }

    @Test
    fun answer_detieneElTemporizador() = runVmTest {
        val (model, _) = crear()
        advanceTimeBy(GuessFlagViewModel.TICK_MS * 95 + 1)
        model.answer(model.respuestaCorrecta())
        val restante = model.uiState.value.tenthsLeft

        // Pasa el tiempo que quedaba en la ronda, pero no la pausa entera.
        advanceTimeBy(GuessFlagViewModel.TICK_MS * 6)

        val state = model.uiState.value
        assertEquals(restante, state.tenthsLeft)
        assertNotEquals(FlagRoundResult.TIMEOUT, state.lastResult)
        assertEquals(1, state.score)
    }
}
