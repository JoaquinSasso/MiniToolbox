package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.MinesStore
import com.joasasso.minitoolbox.data.SavedGame
import com.joasasso.minitoolbox.utils.vibrate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.BitSet
import kotlin.random.Random

enum class Level { EASY, MEDIUM, HARD, CUSTOM }
enum class InputMode { Reveal, Flag }

data class MinesUiState(
    val board: MinesEngine.Board,
    val flagsLeft: Int,
    val elapsedMs: Long,
    val inputMode: InputMode
)

class MinesViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<MinesUiState?>(null)
    val state: StateFlow<MinesUiState?> = _state

    private var tickJob: Job? = null
    private var startEpoch: Long = 0L
    private var elapsed: Long = 0L
    private var level: Level = Level.EASY
    private var custom: MinesEngine.Config = MinesEngine.Config(12, 12, 20)

    init {
        viewModelScope.launch {
            // Último nivel elegido
            val (lvl, c) = MinesStore.loadLastLevel(app)
            level = Level.valueOf(lvl)
            c?.let { custom = MinesEngine.Config(it.first, it.second, it.third) }

            // Intentar restaurar partida
            val saved = MinesStore.load(app)
            if (saved != null) {
                val cfg = MinesEngine.Config(saved.rows, saved.cols, saved.mines)

                // Política guardada o fallback
                val policy = runCatching { MinesEngine.FirstTapPolicy.valueOf(saved.policy) }
                    .getOrElse { MinesEngine.FirstTapPolicy.Square3x3 }

                // Siempre arrancamos con un board "vacío" pero con misma seed/policy
                var board = MinesEngine.newBoard(cfg, saved.seed, policy = policy)

                // Si ya hubo primer toque y tenemos minas guardadas, NO regeneres:
                if (saved.firstTapIndex >= 0 && saved.minesB64.isNotEmpty()) {
                    val mines = MinesStore.base64ToBitSet(saved.minesB64)
                    val numbers = MinesEngine.computeNumbers(cfg.rows, cfg.cols, mines)
                    board = board.copy(
                        firstTapIndex = saved.firstTapIndex,
                        mineBits = mines,
                        numbers = numbers,
                        revealed = MinesStore.base64ToBitSet(saved.revealedB64),
                        flags = MinesStore.base64ToBitSet(saved.flagsB64),
                        status = MinesEngine.Status.valueOf(saved.status),
                        explodedIndex = saved.explodedIndex
                    )
                } else {
                    // Fallback para saves antiguos sin minesB64: regenerar coherente con policy y seed
                    if (saved.firstTapIndex >= 0) {
                        board = MinesEngine.firstTap(board, saved.firstTapIndex)
                    }
                    board = board.copy(
                        revealed = MinesStore.base64ToBitSet(saved.revealedB64),
                        flags = MinesStore.base64ToBitSet(saved.flagsB64),
                        status = MinesEngine.Status.valueOf(saved.status),
                        explodedIndex = saved.explodedIndex
                    )
                }

                startEpoch = saved.startEpochMs
                elapsed = saved.elapsedMs
                publish(board, InputMode.Reveal)
                startTickerIfNeeded(board.status)
            } else {
                // Nueva
                newGame(level, fromUser = false)
            }
        }
    }

    fun onToggleMode() {
        val s = _state.value ?: return
        val next = if (s.inputMode == InputMode.Reveal) InputMode.Flag else InputMode.Reveal
        _state.value = s.copy(inputMode = next)
    }

    fun onCellTap(index: Int) {
        val s = _state.value ?: return
        var b = s.board
        b = if (s.inputMode == InputMode.Flag) {
            MinesEngine.toggleFlag(b, index)
        } else {
            MinesEngine.reveal(b, index)
        }
        afterBoardChange(b)
    }

    fun onCellLongPress(index: Int) {
        val s = _state.value ?: return
        afterBoardChange(MinesEngine.toggleFlag(s.board, index))
    }

    fun onChord(index: Int) {
        val s = _state.value ?: return
        afterBoardChange(MinesEngine.chord(s.board, index))
    }

    fun onNewGame() {
        newGame(level, fromUser = true)
    }

    fun onPickLevel(newLevel: Level, customConfig: MinesEngine.Config? = null) {
        level = newLevel
        customConfig?.let { custom = it }
        viewModelScope.launch {
            MinesStore.saveLastLevel(getApplication(), level.name,
                if (level == Level.CUSTOM) custom.rows else null,
                if (level == Level.CUSTOM) custom.cols else null,
                if (level == Level.CUSTOM) custom.mines else null
            )
        }
        newGame(level, fromUser = true)
    }

    // ----- Internals -----

    private fun newGame(level: Level, fromUser: Boolean) {
        val cfg = when (level) {
            Level.EASY -> MinesEngine.EASY
            Level.MEDIUM -> MinesEngine.MEDIUM
            Level.HARD -> MinesEngine.HARD
            Level.CUSTOM -> custom
        }
        val board = MinesEngine.newBoard(cfg, seed = Random.nextLong(), policy = MinesEngine.FirstTapPolicy.Square3x3)
        elapsed = 0L
        startEpoch = System.currentTimeMillis()
        publish(board, InputMode.Reveal)
        if (fromUser) {
            viewModelScope.launch { MinesStore.clear(getApplication()) }
        }
    }

    private fun publish(board: MinesEngine.Board, mode: InputMode? = null) {
        val flagsLeft = board.mines - board.flags.cardinality()
        val st = MinesUiState(board, flagsLeft, elapsedMs(), mode ?: _state.value?.inputMode ?: InputMode.Reveal)
        _state.value = st
        // Guardar snapshot
        viewModelScope.launch { saveSnapshot(board) }
    }

    private fun afterBoardChange(board: MinesEngine.Board) {
        publish(board)
        startTickerIfNeeded(board.status)
        if (board.status == MinesEngine.Status.Lost) {
            val app = getApplication<Application>()
            vibrate(app, duration = 400, amplitude = 255)
        }
    }

    private fun startTickerIfNeeded(status: MinesEngine.Status) {
        tickJob?.cancel()
        if (status == MinesEngine.Status.InProgress || status == MinesEngine.Status.Ready) {
            if (startEpoch == 0L) startEpoch = System.currentTimeMillis()
            tickJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _state.value?.let { s ->
                        _state.value = s.copy(elapsedMs = elapsedMs())
                    }
                }
            }
        }
    }

    private fun elapsedMs(): Long {
        val st = _state.value?.board?.status ?: MinesEngine.Status.Ready
        return when (st) {
            MinesEngine.Status.Ready -> elapsed
            MinesEngine.Status.InProgress -> (System.currentTimeMillis() - startEpoch) + elapsed
            MinesEngine.Status.Won, MinesEngine.Status.Lost -> elapsed
        }
    }

    private suspend fun saveSnapshot(board: MinesEngine.Board) {
        val app = getApplication<Application>()
        val s = SavedGame(
            rows = board.rows,
            cols = board.cols,
            mines = board.mines,
            seed = board.seed,
            firstTapIndex = board.firstTapIndex,
            status = board.status.name,
            revealedB64 = board.revealed.toBase64(),
            flagsB64 = board.flags.toBase64(),
            startEpochMs = startEpoch,
            elapsedMs = when (board.status) {
                MinesEngine.Status.Ready, MinesEngine.Status.InProgress -> System.currentTimeMillis() - startEpoch
                else -> elapsed
            },
            explodedIndex = board.explodedIndex,
            policy = board.firstTapPolicy.name,
            minesB64 = if (board.firstTapIndex >= 0) board.mineBits.toBase64() else ""
        )
        MinesStore.save(app, s)
        if (board.status == MinesEngine.Status.Won || board.status == MinesEngine.Status.Lost) {
            elapsed = s.elapsedMs
        }
    }

    fun getCurrentLevel(): Level = level

    // Extensions
    private fun BitSet.toBase64(): String = MinesStore.run { this@toBase64.toBase64() }
}
