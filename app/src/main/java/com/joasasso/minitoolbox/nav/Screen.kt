package com.joasasso.minitoolbox.nav

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Jerarquía sellada de destinos de navegación Type-Safe con soporte de serialización.
 */
@Serializable
sealed class Screen {
    abstract val route: String

    // --- Utility / non-measured
    @Serializable
    @SerialName("categories")
    data object Categories : Screen() {
        override val route: String get() = "categories"
    }

    @Serializable
    @SerialName("about")
    data object About : Screen() {
        override val route: String get() = "about"
    }

    @Serializable
    @SerialName("dev_metrics")
    data object DevMetrics : Screen() {
        override val route: String get() = "dev_metrics"
    }

    @Serializable
    @SerialName("pro")
    data object Pro : Screen() {
        override val route: String get() = "pro"
    }

    // --- Tools
    @Serializable
    @SerialName("group_selector")
    data object GroupSelector : Screen() {
        override val route: String get() = "group_selector"
    }

    @Serializable
    @SerialName("coin_flip")
    data object CoinFlip : Screen() {
        override val route: String get() = "coin_flip"
    }

    @Serializable
    @SerialName("decimal_binary")
    data object DecimalBinary : Screen() {
        override val route: String get() = "decimal_binary"
    }

    @Serializable
    @SerialName("truco_scoreboard")
    data object TrucoScoreboard : Screen() {
        override val route: String get() = "truco_scoreboard"
    }

    @Serializable
    @SerialName("age_calculator")
    data object AgeCalculator : Screen() {
        override val route: String get() = "age_calculator"
    }

    @Serializable
    @SerialName("zodiac_sign")
    data object ZodiacSign : Screen() {
        override val route: String get() = "zodiac_sign"
    }

    @Serializable
    @SerialName("pomodoro_list")
    data object PomodoroList : Screen() {
        override val route: String get() = "pomodoro_list"
    }

    @Serializable
    @SerialName("pomodoro_detail")
    data class PomodoroDetail(val timerId: String) : Screen() {
        override val route: String get() = "pomodoro/detail/$timerId"

        companion object {
            const val ARG = "timerId"
            fun createRoute(timerId: String) = "pomodoro/detail/$timerId"
        }
    }

    @Serializable
    @SerialName("bubble_level")
    data object BubbleLevel : Screen() {
        override val route: String get() = "bubble_level"
    }

    @Serializable
    @SerialName("percentage")
    data object Percentage : Screen() {
        override val route: String get() = "percentage"
    }

    @Serializable
    @SerialName("unit_converter")
    data object UnitConverter : Screen() {
        override val route: String get() = "unit_converter"
    }

    @Serializable
    @SerialName("password_generator")
    data object PasswordGenerator : Screen() {
        override val route: String get() = "password_generator"
    }

    @Serializable
    @SerialName("qr_generator")
    data object QrGenerator : Screen() {
        override val route: String get() = "qr_generator"
    }

    @Serializable
    @SerialName("ruler")
    data object Ruler : Screen() {
        override val route: String get() = "ruler"
    }

    @Serializable
    @SerialName("light_meter")
    data object LightMeter : Screen() {
        override val route: String get() = "light_meter"
    }

    @Serializable
    @SerialName("flashlight")
    data object Flashlight : Screen() {
        override val route: String get() = "flashlight"
    }

    @Serializable
    @SerialName("water")
    data object Water : Screen() {
        override val route: String get() = "water"
    }

    @Serializable
    @SerialName("water_stats")
    data object WaterStats : Screen() {
        override val route: String get() = "water_stats"
    }

    @Serializable
    @SerialName("countdown")
    data object Countdown : Screen() {
        override val route: String get() = "countdown"
    }

    @Serializable
    @SerialName("countries_info")
    data object CountriesInfo : Screen() {
        override val route: String get() = "countries_info"
    }

    @Serializable
    @SerialName("selector_wheel")
    data object SelectorWheel : Screen() {
        override val route: String get() = "selector_wheel"
    }

    @Serializable
    @SerialName("guess_flag")
    data object GuessFlag : Screen() {
        override val route: String get() = "guess_flag"
    }

    @Serializable
    @SerialName("meetings")
    data object Meetings : Screen() {
        override val route: String get() = "meetings"
    }

    @Serializable
    @SerialName("meeting_create")
    data object MeetingCreate : Screen() {
        override val route: String get() = "meeting_create"
    }

    @Serializable
    @SerialName("meeting_detail")
    data class MeetingDetail(val reunionId: String) : Screen() {
        override val route: String get() = "meeting_detail/$reunionId"
    }

    @Serializable
    @SerialName("expense_edit")
    data class ExpenseEdit(val reunionId: String, val gastoId: String) : Screen() {
        override val route: String get() = "expense_edit/$reunionId/$gastoId"
    }

    @Serializable
    @SerialName("expense_add")
    data class ExpenseAdd(val reunionId: String) : Screen() {
        override val route: String get() = "expense_add/$reunionId"
    }

    @Serializable
    @SerialName("dice")
    data object Dice : Screen() {
        override val route: String get() = "dice"
    }

    @Serializable
    @SerialName("quick_calcs")
    data object QuickCalcs : Screen() {
        override val route: String get() = "quick_calcs"
    }

    @Serializable
    @SerialName("basic_phrases")
    data object BasicPhrases : Screen() {
        override val route: String get() = "basic_phrases"
    }

    @Serializable
    @SerialName("multiverse_me")
    data object MultiverseMe : Screen() {
        override val route: String get() = "multiverse_me"
    }

    @Serializable
    @SerialName("guess_capital")
    data object GuessCapital : Screen() {
        override val route: String get() = "guess_capital"
    }

    @Serializable
    @SerialName("compass")
    data object Compass : Screen() {
        override val route: String get() = "compass"
    }

    @Serializable
    @SerialName("todo")
    data object Todo : Screen() {
        override val route: String get() = "todo"
    }

    @Serializable
    @SerialName("scoreboard")
    data object Scoreboard : Screen() {
        override val route: String get() = "scoreboard"
    }

    @Serializable
    @SerialName("magnifier")
    data object Magnifier : Screen() {
        override val route: String get() = "magnifier"
    }

    @Serializable
    @SerialName("ar_ruler")
    data object ArRuler : Screen() {
        override val route: String get() = "ar_ruler"
    }

    @Serializable
    @SerialName("minesweeper")
    data object Minesweeper : Screen() {
        override val route: String get() = "minesweeper"
    }

    companion object {
        const val EXTRA_START_ROUTE_JSON = "startRouteJson"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun toJson(screen: Screen): String = json.encodeToString<Screen>(screen)

        fun fromJson(jsonString: String?): Screen? {
            if (jsonString.isNullOrBlank()) return null
            return runCatching { json.decodeFromString<Screen>(jsonString) }.getOrNull()
        }

        fun fromRouteString(route: String?): Screen? {
            if (route.isNullOrBlank()) return null
            val base = route.substringBefore('?').substringBefore('#')

            if (base.startsWith("pomodoro/detail/")) {
                val timerId = base.removePrefix("pomodoro/detail/")
                if (timerId.isNotBlank()) return PomodoroDetail(timerId)
            }
            if (base.startsWith("pomodoro_detail/")) {
                val timerId = base.removePrefix("pomodoro_detail/")
                if (timerId.isNotBlank()) return PomodoroDetail(timerId)
            }
            if (base.startsWith("meeting_detail/")) {
                val id = base.removePrefix("meeting_detail/")
                if (id.isNotBlank()) return MeetingDetail(id)
            }
            if (base.startsWith("expense_add/")) {
                val id = base.removePrefix("expense_add/")
                if (id.isNotBlank()) return ExpenseAdd(id)
            }
            if (base.startsWith("expense_edit/")) {
                val parts = base.removePrefix("expense_edit/").split('/')
                if (parts.size >= 2) return ExpenseEdit(parts[0], parts[1])
            }

            return when (base) {
                "categories" -> Categories
                "about" -> About
                "dev_metrics", "dev/metrics" -> DevMetrics
                "pro" -> Pro
                "group_selector" -> GroupSelector
                "coin_flip" -> CoinFlip
                "decimal_binary" -> DecimalBinary
                "truco_scoreboard" -> TrucoScoreboard
                "age_calculator" -> AgeCalculator
                "zodiac_sign" -> ZodiacSign
                "pomodoro", "pomodoro_list" -> PomodoroList
                "pomodoro_detail" -> PomodoroDetail("")
                "bubble_level" -> BubbleLevel
                "percentage" -> Percentage
                "unit_converter" -> UnitConverter
                "password_generator" -> PasswordGenerator
                "qr_generator" -> QrGenerator
                "ruler" -> Ruler
                "light_meter" -> LightMeter
                "flashlight" -> Flashlight
                "water" -> Water
                "water_stats" -> WaterStats
                "countdown" -> Countdown
                "countries_info" -> CountriesInfo
                "selector_wheel" -> SelectorWheel
                "guess_flag" -> GuessFlag
                "meetings" -> Meetings
                "meeting_create" -> MeetingCreate
                "meeting_detail" -> MeetingDetail("")
                "expense_add" -> ExpenseAdd("")
                "expense_edit" -> ExpenseEdit("", "")
                "dice" -> Dice
                "quick_calcs" -> QuickCalcs
                "basic_phrases", "quotes" -> BasicPhrases
                "multiverse_me" -> MultiverseMe
                "guess_capital" -> GuessCapital
                "compass" -> Compass
                "todo" -> Todo
                "scoreboard" -> Scoreboard
                "magnifier" -> Magnifier
                "ar_ruler" -> ArRuler
                "minesweeper" -> Minesweeper
                else -> null
            }
        }

        fun isValidRoute(route: String?): Boolean = fromRouteString(route) != null
    }
}
