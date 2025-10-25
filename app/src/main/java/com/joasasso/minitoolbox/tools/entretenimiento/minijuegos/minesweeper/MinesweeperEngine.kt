package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper

import java.util.BitSet
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object MinesEngine {

    data class Config(val rows: Int, val cols: Int, val mines: Int)

    /**
     * Game Status: Ready, InProgress, Won, Lost
     */
    enum class Status { Ready, InProgress, Won, Lost }
/**
 *SingleSafe: Reveals only the selected cell

 *CrossSafe: Reveals a cross with the selected cell in the center

 * Square3x3: Reveals a 3x3 square with the selected cell in the center
 **/
    enum class FirstTapPolicy { SingleSafe, CrossSafe, Square3x3 }

    /** Max profundity that will be revealed at the first tap (0 = only the tapped cell, 1 ≈ a cross around the tapped cell, 2 ≈ 5×5) **/
    private const val MAX_ZERO_DEPTH = 12

    data class Board(
        val rows: Int,
        val cols: Int,
        val mines: Int,
        val seed: Long,
        val firstTapIndex: Int = -1,
        val firstTapPolicy: FirstTapPolicy = FirstTapPolicy.CrossSafe, // <- default recomendado
        val mineBits: BitSet = BitSet(rows * cols),
        val revealed: BitSet = BitSet(rows * cols),
        val flags: BitSet = BitSet(rows * cols),
        val numbers: IntArray = IntArray(rows * cols),
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
    val EASY = Config(12, 9, 24)
    val MEDIUM = Config(16, 11, 38)
    val HARD = Config(20, 13, 60)

    fun newBoard(config: Config, seed: Long = Random.nextLong(),
                 policy: FirstTapPolicy = FirstTapPolicy.CrossSafe): Board =
        Board(config.rows, config.cols, config.mines, seed, firstTapPolicy = policy)

    fun firstTap(board: Board, index: Int): Board {
        val mines = generateMines(board.rows, board.cols, board.mines, board.seed, index, board.firstTapPolicy)
        val numbers = computeNumbers(board.rows, board.cols, mines)
        return board.copy(
            firstTapIndex = index,
            mineBits = mines,
            numbers = numbers,
            status = Status.InProgress
        )
    }

    fun toggleFlag(board: Board, index: Int): Board {
        // No permitir antes del primer toque
        if (board.firstTapIndex == -1) return board
        if (board.status == Status.Lost || board.status == Status.Won) return board
        if (board.revealed[index]) return board

        val flags = board.flags.clone() as BitSet
        val placing = !flags[index]

        // Tope: no permitir más banderas que minas
        if (placing && flags.cardinality() >= board.mines) return board

        flags.flip(index)
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

        // Flood fill limitado por profundidad de ceros
        val revealed = board.revealed.clone() as BitSet

        // Cola con (índice, depth). El depth cuenta capas de ceros desde el toque inicial.
        data class Node(val idx: Int, val depth: Int)
        val queue = ArrayDeque<Node>()
        queue.add(Node(index, 0))

        while (queue.isNotEmpty()) {
            val (i, d) = queue.removeFirst()
            if (revealed[i]) continue
            if (board.flags[i]) continue   // no revelar banderas
            revealed.set(i)

            val num = board.numbers[i]
            // Si esta celda es un cero y todavía podemos expandir ceros, encolamos vecinos
            if (num == 0 && d < MAX_ZERO_DEPTH) {
                neighbors(board, i).forEach { n ->
                    if (!revealed[n]) queue.add(Node(n, d + 1))
                }
            } else if (num == 0) {
                // En el borde: no expandimos más, pero revelamos los vecinos NUMÉRICOS directos
                neighbors(board, i).forEach { n ->
                    if (!revealed[n] && !board.flags[n]) {
                        // Revelar números adyacentes al borde, sin seguir expandiendo
                        if (board.numbers[n] > 0 || !board.mineBits[n]) {
                            revealed.set(n)
                        }
                    }
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

     fun computeNumbers(rows: Int, cols: Int, mines: BitSet): IntArray {
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

    private fun generateMines(
        rows: Int, cols: Int, count: Int, seed: Long, firstTap: Int, policy: FirstTapPolicy
    ): BitSet {
        val total = rows * cols
        val safe = safeZone(rows, cols, firstTap, policy)
        val pool = IntArray(total - safe.size)
        var p = 0
        for (i in 0 until total) if (!safe.contains(i)) pool[p++] = i

        val rnd = Random(seed xor firstTap.toLong())
        for (i in pool.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = pool[i]; pool[i] = pool[j]; pool[j] = tmp
        }
        return BitSet(total).apply { for (i in 0 until count) set(pool[i]) }
    }

    private fun safeZone(rows: Int, cols: Int, index: Int, policy: FirstTapPolicy): IntArray {
        val r = index / cols
        val c = index % cols
        val list = ArrayList<Int>(9)
        fun add(rr: Int, cc: Int) {
            if (rr in 0 until rows && cc in 0 until cols) list.add(rr * cols + cc)
        }
        when (policy) {
            FirstTapPolicy.SingleSafe -> add(r, c)
            FirstTapPolicy.CrossSafe -> {
                add(r, c)
                add(r - 1, c); add(r + 1, c); add(r, c - 1); add(r, c + 1)
            }
            FirstTapPolicy.Square3x3 -> {
                for (rr in r - 1..r + 1) for (cc in c - 1..c + 1) add(rr, cc)
            }
        }
        return list.toIntArray()
    }
}
