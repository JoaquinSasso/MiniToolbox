package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.DefaultFlagGameRepository
import com.joasasso.minitoolbox.data.FlagCountries
import com.joasasso.minitoolbox.data.FlagCountry
import com.joasasso.minitoolbox.data.FlagGameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Mensaje bajo el marcador. Un acierto sin récord no muestra nada. */
enum class FlagRoundResult { NEW_RECORD, WRONG, TIMEOUT }

data class GuessFlagUiState(
    val question: FlagCountry? = null,
    val options: List<FlagCountry> = emptyList(),
    /** Opción tocada en la ronda actual; `null` si no se respondió o se acabó el tiempo. */
    val selectedIso: String? = null,
    /** Pausa entre la respuesta (o el fin del tiempo) y la ronda siguiente. */
    val revealing: Boolean = false,
    val score: Int = 0,
    val record: Int = 0,
    val tenthsLeft: Int = GuessFlagViewModel.ROUND_TENTHS,
    val lastResult: FlagRoundResult? = null,
    val isLoading: Boolean = false,
) {
    /** Fondo en rojo: respuesta incorrecta o tiempo agotado. */
    val showError: Boolean get() = revealing && selectedIso != question?.isoCode
}

sealed interface GuessFlagEvent {
    data object Correct : GuessFlagEvent
    data object Wrong : GuessFlagEvent
}

class GuessFlagViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FlagGameRepository = DefaultFlagGameRepository,
    private val countries: List<FlagCountry> = FlagCountries.all,
    private val random: Random = Random.Default,
) : AndroidViewModel(application) {

    // A diferencia de AguaViewModel, el estado no se arma con stateIn(WhileSubscribed): el
    // temporizador y el récord tienen que seguir vigentes aunque no haya UI suscripta, porque
    // answer() decide si hay récord nuevo comparando contra game.value.record.
    private val game = MutableStateFlow(GuessFlagUiState(isLoading = true))
    val uiState: StateFlow<GuessFlagUiState> = game.asStateFlow()

    private val _events = Channel<GuessFlagEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // El temporizador de la ronda o la pausa que lleva a la siguiente; nunca los dos a la vez.
    private var roundJob: Job? = null

    init {
        viewModelScope.launch {
            repository.bestScore(application).collect { best ->
                game.update { it.copy(record = maxOf(it.record, best), isLoading = false) }
            }
        }
        startRound()
    }

    fun answer(isoCode: String) {
        val current = game.value
        val question = current.question ?: return
        if (current.revealing) return
        roundJob?.cancel()

        val correct = isoCode == question.isoCode
        val score = if (correct) current.score + 1 else 0
        val result = when {
            !correct -> FlagRoundResult.WRONG
            score > current.record -> FlagRoundResult.NEW_RECORD
            else -> null
        }
        game.value = current.copy(
            selectedIso = isoCode,
            revealing = true,
            score = score,
            record = maxOf(current.record, score),
            lastResult = result,
        )
        _events.trySend(if (correct) GuessFlagEvent.Correct else GuessFlagEvent.Wrong)
        if (result == FlagRoundResult.NEW_RECORD) {
            viewModelScope.launch { repository.recordScore(getApplication(), score) }
        }
        scheduleNextRound()
    }

    private fun startRound() {
        val round = FlagQuiz.newRound(countries, random)
        game.update {
            it.copy(
                question = round.question,
                options = round.options,
                selectedIso = null,
                revealing = false,
                tenthsLeft = ROUND_TENTHS,
            )
        }
        roundJob = viewModelScope.launch {
            while (game.value.tenthsLeft > 0) {
                delay(TICK_MS)
                game.update { it.copy(tenthsLeft = it.tenthsLeft - 1) }
            }
            game.update {
                it.copy(revealing = true, score = 0, lastResult = FlagRoundResult.TIMEOUT)
            }
            scheduleNextRound()
        }
    }

    private fun scheduleNextRound() {
        roundJob = viewModelScope.launch {
            delay(REVEAL_MS)
            startRound()
        }
    }

    companion object {
        /** Duración de una ronda, en décimas de segundo (10 s). */
        const val ROUND_TENTHS = 100
        const val TICK_MS = 100L
        const val REVEAL_MS = 1000L
    }
}
