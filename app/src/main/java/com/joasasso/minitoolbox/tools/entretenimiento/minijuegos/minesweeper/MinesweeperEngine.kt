package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper

import java.util.BitSet
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object MinesEngine {

    data class Config(val rows: Int, val cols: Int, val mines: Int)

    enum class Status { Ready, InProgress, Won, Lost }

    data class Board(
        val rows: Int,
        val cols: Int,
        val mines: Int,
        val seed: Long,
        val firstTapIndex: Int = -1,            // -1 si aún no hubo primer toque
        val mineBits: BitSet = BitSet(rows * cols),
        val revealed: BitSet = BitSet(rows * cols),
        val flags: BitSet = BitSet(rows * cols),
        val numbers: IntArray = IntArray(rows * cols) { 0 },
        val status: Status = Status.Ready,
        val explodedIndex: Int = -1
    ) {
        val totalCells: Int get() = rows * cols
        fun idx(r: Int, c: Int) = r * cols + c
        fun rc(idx: Int): Pair<Int, Int> = idx / cols to idx % cols
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Board

            if (rows != other.rows) return false
            if (cols != other.cols) return false
            if (mines != other.mines) return false
            if (seed != other.seed) return false
            if (firstTapIndex != other.firstTapIndex) return false
            if (explodedIndex != other.explodedIndex) return false
            if (mineBits != other.mineBits) return false
            if (revealed != other.revealed) return false
            if (flags != other.flags) return false
            if (!numbers.contentEquals(other.numbers)) return false
            if (status != other.status) return false
            if (totalCells != other.totalCells) return false

            return true
        }

        override fun hashCode(): Int {
            var result = rows
            result = 31 * result + cols
            result = 31 * result + mines
            result = 31 * result + seed.hashCode()
            result = 31 * result + firstTapIndex
            result = 31 * result + explodedIndex
            result = 31 * result + mineBits.hashCode()
            result = 31 * result + revealed.hashCode()
            result = 31 * result + flags.hashCode()
            result = 31 * result + numbers.contentHashCode()
            result = 31 * result + status.hashCode()
            result = 31 * result + totalCells
            return result
        }
    }

    // Presets
    val EASY = Config(10, 7, 10)
    val MEDIUM = Config(14, 9, 25)
    val HARD = Config(18, 10, 40)

    fun newBoard(config: Config, seed: Long = Random.nextLong()): Board =
        Board(config.rows, config.cols, config.mines, seed)

    fun firstTap(board: Board, index: Int): Board {
        // Genera minas evitando zona segura (celda y sus vecinos)
        val mines = generateMines(board.rows, board.cols, board.mines, board.seed, index)
        val numbers = computeNumbers(board.rows, board.cols, mines)
        return board.copy(
            firstTapIndex = index,
            mineBits = mines,
            numbers = numbers,
            status = Status.InProgress
        )
    }

    fun toggleFlag(board: Board, index: Int): Board {
        if (board.status == Status.Lost || board.status == Status.Won) return board
        if (board.revealed[index]) return board
        val flags = (board.flags.clone() as BitSet).apply {
            if (get(index)) clear(index) else set(index)
        }
        return board.copy(flags = flags)
    }

    fun chord(board: Board, index: Int): Board {
        if (!board.revealed[index]) return board
        val n = board.numbers[index]
        if (n <= 0) return board
        val adj = neighbors(board, index)
        val flagged = adj.count { board.flags[it] }
        if (flagged != n) return board
        var b = board
        for (i in adj) {
            if (!b.flags[i] && !b.revealed[i]) {
                b = reveal(b, i)
                if (b.status == Status.Lost) break
            }
        }
        return b
    }

    fun reveal(board: Board, index: Int): Board {
        if (board.status == Status.Lost || board.status == Status.Won) return board

        // Si es el primer toque, genera minas y continúa
        if (board.firstTapIndex == -1) {
            val primed = firstTap(board, index)
            return reveal(primed, index)
        }

        // No reveles banderas ni celdas ya reveladas
        if (board.flags[index] || board.revealed[index]) return board

        // Mina => perder
        if (board.mineBits[index]) {
            return board.copy(
                revealed = (board.revealed.clone() as BitSet).apply { set(index) },
                status = Status.Lost,
                explodedIndex = index
            )
        }

        // Flood fill de ceros
        val revealed = board.revealed.clone() as BitSet
        val queue = ArrayDeque<Int>()
        queue.add(index)

        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            if (revealed[i]) continue
            revealed.set(i)

            if (board.numbers[i] == 0) {
                neighbors(board, i).forEach { n ->
                    if (!revealed[n] && !board.flags[n]) queue.add(n)
                }
            }
        }

        // ¿Victoria?
        val totalNoMines = board.totalCells - board.mines
        val revealedCount = revealed.cardinality()
        val status =
            if (revealedCount >= totalNoMines) Status.Won else Status.InProgress

        return board.copy(revealed = revealed, status = status)
    }

    // ----- Helpers -----

    private fun neighbors(board: Board, index: Int): IntArray {
        val (r, c) = board.rc(index)
        val out = ArrayList<Int>(8)
        for (rr in max(0, r - 1)..min(board.rows - 1, r + 1)) {
            for (cc in max(0, c - 1)..min(board.cols - 1, c + 1)) {
                val ii = board.idx(rr, cc)
                if (ii != index) out.add(ii)
            }
        }
        return out.toIntArray()
    }

    private fun computeNumbers(rows: Int, cols: Int, mines: BitSet): IntArray {
        val numbers = IntArray(rows * cols)
        fun idx(r: Int, c: Int) = r * cols + c
        fun inB(r: Int, c: Int) = r in 0 until rows && c in 0 until cols
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = idx(r, c)
            if (mines[i]) continue
            var n = 0
            for (rr in r - 1..r + 1) for (cc in c - 1..c + 1) {
                if (rr == r && cc == c) continue
                if (inB(rr, cc) && mines[idx(rr, cc)]) n++
            }
            numbers[i] = n
        }
        return numbers
    }

    private fun generateMines(rows: Int, cols: Int, count: Int, seed: Long, firstTap: Int): BitSet {
        val total = rows * cols
        val safe = safeZone(rows, cols, firstTap)
        val pool = IntArray(total - safe.size)
        var p = 0
        for (i in 0 until total) {
            if (!safe.contains(i)) { pool[p++] = i }
        }
        // Shuffle determinista
        val rnd = Random(seed xor firstTap.toLong())
        for (i in pool.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp
        }
        val bits = BitSet(total)
        for (i in 0 until count) bits.set(pool[i])
        return bits
    }

    private fun safeZone(rows: Int, cols: Int, index: Int): IntArray {
        val r = index / cols
        val c = index % cols
        val out = ArrayList<Int>(9)
        for (rr in max(0, r - 1)..min(rows - 1, r + 1)) {
            for (cc in max(0, c - 1)..min(cols - 1, c + 1)) {
                out.add(rr * cols + cc)
            }
        }
        return out.toIntArray()
    }
}
