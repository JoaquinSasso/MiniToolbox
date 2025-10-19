package com.joasasso.minitoolbox.nav

/**
 * Centraliza todas las rutas de navegación y sus títulos.
 */
// Rutas normalizadas (en inglés, ASCII, minúsculas con _)
sealed class Screen(val route: String) {

    // --- Utility / non-measured (si querés ocultarlas en métricas)
    object Categories  : Screen("categories")
    object About       : Screen("about")
    object DevMetrics  : Screen("dev_metrics") // antes: "dev/metrics"

    // --- Tools (normalizadas)
    object RandomColor         : Screen("random_color")
    object GroupSelector       : Screen("group_selector")
    object CoinFlip            : Screen("coin_flip")
    object DecimalBinary       : Screen("decimal_binary")
    object TextBinary          : Screen("text_binary")
    object TrucoScoreboard     : Screen("truco_scoreboard")
    object AgeCalculator       : Screen("age_calculator")
    object ZodiacSign          : Screen("zodiac_sign")
    object PomodoroList        : Screen("pomodoro_list")
    object PomodoroDetail : Screen("pomodoro/detail/{timerId}") {
        const val ARG = "timerId"
        fun createRoute(timerId: String) = "pomodoro/detail/$timerId"
    }
    object BubbleLevel         : Screen("bubble_level")
    object Percentage          : Screen("percentage")
    object TimeConverter       : Screen("time_converter")
    object BmiCalculator       : Screen("bmi_calculator")
    object RomanNumerals       : Screen("roman_numerals")
    object UnitConverter       : Screen("unit_converter")
    object PasswordGenerator   : Screen("password_generator")
    object ActivitySuggester   : Screen("activity_suggester")
    object NameGenerator       : Screen("name_generator")
    object QrGenerator         : Screen("qr_generator")
    object VcardGenerator      : Screen("vcard_generator")
    object LoremIpsum          : Screen("lorem_ipsum")
    object Ruler               : Screen("ruler")
    object LightMeter          : Screen("light_meter")
    object Flashlight          : Screen("flashlight")
    object Streaks             : Screen("streaks")
    object Water               : Screen("water")
    object WaterStats          : Screen("water_stats")
    object Countdown           : Screen("countdown")
    object CountriesInfo       : Screen("countries_info")
    object SelectorWheel       : Screen("selector_wheel")
    object GuessFlag           : Screen("guess_flag")
    object MeetingCreate       : Screen("meeting_create")
    object Meetings            : Screen("meetings")
    object MeetingDetail       : Screen("meeting_detail")
    object ExpenseEdit         : Screen("expense_edit")
    object ExpenseAdd          : Screen("expense_add")
    object Dice                : Screen("dice")
    object QuickCalcs          : Screen("quick_calcs")
    object Quotes              : Screen("quotes")
    object MultiverseMe        : Screen("multiverse_me")
    object GuessCapital        : Screen("guess_capital")
    object Compass             : Screen("compass")
    object Todo                : Screen("todo")
    object Events              : Screen("events")
    object CompoundInterest    : Screen("compound_interest")
    object Scoreboard          : Screen("scoreboard")
    object Magnifier           : Screen("magnifier")
    object ArRuler             : Screen("ar_ruler")
    object Pro : Screen("pro")

    object Minesweeper : Screen("minesweeper")

    companion object {
        // Convierte patrones con {param} en Regex que matchea valores reales
        // Acepta: letras, números y símbolos comunes de IDs dentro de un segmento (sin / ? #)
        private const val SEGMENT_REGEX = "[A-Za-z0-9._~+%-]+"

        // "pomodoro/detail/{timerId}"  ->  ^pomodoro/detail/[A-Za-z0-9._~+%-]+(?:\\?.*)?$
        private fun routePatternToRegex(pattern: String): Regex {
            val sb = StringBuilder()
            var i = 0
            while (i < pattern.length) {
                val ch = pattern[i]
                if (ch == '{') {
                    val end = pattern.indexOf('}', startIndex = i + 1)
                    if (end == -1) {
                        // Llave sin cerrar: trata tod0 como literal escapado
                        sb.append(Regex.escape(pattern.substring(i)))
                        break
                    } else {
                        // reemplaza {param} por un segmento de ruta válido
                        sb.append(SEGMENT_REGEX)
                        i = end + 1
                        continue
                    }
                } else {
                    sb.append(Regex.escape(ch.toString()))
                    i++
                }
            }
            return Regex("^$sb(?:\\?.*)?$")
        }

        fun isValidRoute(route: String?): Boolean {
            if (route.isNullOrBlank()) return false
            val patterns = Screen::class.sealedSubclasses
                .mapNotNull { it.objectInstance?.route } // e.g. "pomodoro/detail/{timerId}"
            // Coincidencia exacta o por patrón con {param}
            return patterns.any { p ->
                p == route || routePatternToRegex(p).matches(route)
            }
        }

    }
}

