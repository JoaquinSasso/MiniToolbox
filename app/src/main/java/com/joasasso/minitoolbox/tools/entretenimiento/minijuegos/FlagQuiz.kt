package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import com.joasasso.minitoolbox.data.FlagCountry
import kotlin.random.Random

data class FlagRound(val question: FlagCountry, val options: List<FlagCountry>)

object FlagQuiz {
    const val OPTION_COUNT = 4

    /** Elige [OPTION_COUNT] países distintos, ya mezclados, y uno de ellos como respuesta. */
    fun newRound(countries: List<FlagCountry>, random: Random): FlagRound {
        require(countries.size >= OPTION_COUNT) {
            "Hacen falta al menos $OPTION_COUNT países, hay ${countries.size}"
        }
        val options = countries.shuffled(random).take(OPTION_COUNT)
        return FlagRound(question = options.random(random), options = options)
    }
}
