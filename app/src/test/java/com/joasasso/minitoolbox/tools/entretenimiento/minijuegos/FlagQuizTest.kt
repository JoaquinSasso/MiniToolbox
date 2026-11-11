package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import com.joasasso.minitoolbox.data.FlagCountries
import com.joasasso.minitoolbox.data.FlagCountry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FlagQuizTest {

    @Test
    fun newRound_cuatroOpcionesDistintasQueIncluyenLaRespuesta() {
        val random = Random(42)
        repeat(500) {
            val round = FlagQuiz.newRound(FlagCountries.all, random)
            assertEquals(FlagQuiz.OPTION_COUNT, round.options.size)
            assertEquals(FlagQuiz.OPTION_COUNT, round.options.map { it.isoCode }.toSet().size)
            assertTrue(round.question in round.options)
        }
    }

    @Test
    fun newRound_conLaMismaSemillaEsDeterminista() {
        val a = FlagQuiz.newRound(FlagCountries.all, Random(7))
        val b = FlagQuiz.newRound(FlagCountries.all, Random(7))
        assertEquals(a, b)
    }

    @Test
    fun newRound_conExactamenteCuatroPaisesLosUsaTodos() {
        val four = FlagCountries.all.take(4)
        val round = FlagQuiz.newRound(four, Random(1))
        assertEquals(four.toSet(), round.options.toSet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun newRound_conMenosDeCuatroPaisesFalla() {
        FlagQuiz.newRound(listOf(FlagCountry("AR", "Argentina", "Argentina")), Random(1))
    }
}
