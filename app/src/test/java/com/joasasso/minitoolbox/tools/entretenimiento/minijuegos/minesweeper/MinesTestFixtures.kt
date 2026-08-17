package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper

import java.util.BitSet

object MinesTestFixtures {

    /**
     * Creates a board with a specific mine layout for testing.
     * This bypasses the random generation.
     */
    fun createFixedBoard(
        rows: Int,
        cols: Int,
        minesIndexes: List<Int>,
        policy: MinesEngine.FirstTapPolicy = MinesEngine.FirstTapPolicy.SingleSafe
    ): MinesEngine.Board {
        val mineBits = BitSet(rows * cols)
        minesIndexes.forEach { mineBits.set(it) }
        
        val numbers = MinesEngine.computeNumbers(rows, cols, mineBits)
        
        return MinesEngine.Board(
            rows = rows,
            cols = cols,
            mines = minesIndexes.size,
            seed = 1234L,
            firstTapIndex = 0, // Mocked as if it already happened
            firstTapPolicy = policy,
            mineBits = mineBits,
            numbers = numbers,
            status = MinesEngine.Status.InProgress
        )
    }

    fun MinesEngine.Board.toAscii(): String {
        val sb = StringBuilder()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val i = idx(r, c)
                val char = when {
                    status == MinesEngine.Status.Lost && explodedIndex == i -> 'X'
                    flags[i] -> 'F'
                    revealed[i] -> if (mineBits[i]) '*' else numbers[i].toString()[0]
                    else -> '.'
                }
                sb.append(char).append(' ')
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}
