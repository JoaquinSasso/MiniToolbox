// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.joasasso.minitoolbox.tools
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.nav.Screen


object ToolRegistry {
    val tools = listOf(
        Tool(
            name = R.string.tool_bubble_level,
            screen = Screen.BubbleLevel,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_bubble_level,
            svgResId = R.drawable.bubble_level,
            isPro = true
        ),
        Tool(
            name = R.string.tool_team_generator,
            screen = Screen.GroupSelector,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_team_generator,
            svgResId = R.drawable.teamselector
        ),
        Tool(
            name = R.string.tool_color_generator,
            screen = Screen.RandomColor,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_color_generator,
            svgResId = R.drawable.colorgenerator
        ),
        Tool(
            name = R.string.tool_coin_flip,
            screen = Screen.CoinFlip,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            summary = R.string.sum_tool_coin_flip,
            svgResId = R.drawable.coin
        ),
        Tool(
            name = R.string.tool_decimal_binary,
            screen = Screen.DecimalBinary,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_decimal_binary,
            svgResId = R.drawable.decimal_binary
        ),
        Tool(
            name = R.string.tool_text_binary,
            screen = Screen.TextBinary,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_text_binary,
            svgResId = R.drawable.binary
        ),
        Tool(
            name = R.string.tool_truco_score,
            screen = Screen.TrucoScoreboard,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_scoreboards,
            summary = R.string.sum_tool_truco_score,
            svgResId = R.drawable.truco_square
        ),
        Tool(
            name = R.string.tool_age_calculator,
            screen = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            summary = R.string.sum_tool_age_calculator,
            svgResId = R.drawable.age_calculator
        ),
        Tool(
            name = R.string.tool_zodiac_sign,
            screen = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            summary = R.string.sum_tool_zodiac_sign,
            svgResId = R.drawable.zodiac_sign
        ),
        Tool(
            name = R.string.tool_percentage,
            screen = Screen.Percentage,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_percentage,
            svgResId = R.drawable.percentage
        ),
        Tool(
            name = R.string.tool_hour_converter,
            screen = Screen.TimeConverter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_hour_converter,
            svgResId = R.drawable.clock_conversor
        ),
        Tool(
            name = R.string.tool_bmi_calculator,
            screen = Screen.BmiCalculator,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_bmi_calculator,
            svgResId = R.drawable.scale
        ),
        Tool(
            name = R.string.tool_roman_converter,
            screen = Screen.RomanNumerals,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_roman_converter,
            svgResId = R.drawable.roman_pillar
        ),

        Tool(
            name = R.string.tool_unit_converter,
            screen = Screen.UnitConverter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_unit_converter,
            svgResId = R.drawable.unit_conversor,
            isPro = true
        ),
        Tool(
            name = R.string.tool_password_generator,
            screen = Screen.PasswordGenerator,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_password_generator,
            svgResId = R.drawable.password
        ),
        Tool(
            name = R.string.tool_activity_suggester,
            screen = Screen.ActivitySuggester,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            summary = R.string.sum_tool_activity_suggester,
            svgResId = R.drawable.activity_suggester
        ),
        Tool(
            name = R.string.tool_pomodoro_timer,
            screen = Screen.Pomodoro,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            summary = R.string.sum_tool_pomodoro_timer,
            svgResId = R.drawable.timer
        ),
        Tool(
            name = R.string.tool_name_generator,
            screen = Screen.NameGenerator,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_name_generator,
            svgResId = R.drawable.id_card
        ),
        Tool(
            name = R.string.tool_qr_generator,
            screen = Screen.QrGenerator,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_qr_generator,
            svgResId = R.drawable.qr_code
        ),
        Tool(
            name = R.string.tool_qr_vcard,
            screen = Screen.VcardGenerator,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_qr_vcard,
            svgResId = R.drawable.vcard
        ),
        Tool(
            name = R.string.tool_lorem_ipsum,
            screen = Screen.LoremIpsum,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_lorem_ipsum,
            svgResId = R.drawable.lorem_ipsum
        ),
        Tool(
            name = R.string.tool_ruler,
            screen = Screen.Ruler,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_ruler,
            svgResId = R.drawable.ruler
        ),
        Tool(
            name = R.string.tool_light_meter,
            screen = Screen.LightMeter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_light_meter,
            svgResId = R.drawable.light_meter
        ),
        Tool(
            name = R.string.tool_flashlight,
            screen = Screen.Flashlight,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_flashlight,
            svgResId = R.drawable.flashlight
        ),
        Tool(
            name = R.string.tool_habit_tracker,
            screen = Screen.Streaks,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            summary = R.string.sum_tool_habit_tracker,
            svgResId = R.drawable.habits
        ),
        Tool(
            name = R.string.tool_water_reminder,
            screen = Screen.Water,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            summary = R.string.sum_tool_water_reminder,
            svgResId = R.drawable.water_drop
        ),
        Tool(
            name = R.string.tool_day_countdown,
            screen = Screen.Countdown,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            summary = R.string.sum_tool_day_countdown,
            svgResId = R.drawable.countdown
        ),
        Tool(
            name = R.string.tool_country_info,
            screen = Screen.CountriesInfo,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_general,
            summary = R.string.sum_tool_country_info,
            svgResId = R.drawable.countries_info
        ),
        Tool(
            name = R.string.tool_option_selector,
            screen = Screen.SelectorWheel,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            summary = R.string.sum_tool_option_selector,
            svgResId = R.drawable.fortune_wheel
        ),
        Tool(
            name = R.string.tool_guess_flag,
            screen = Screen.GuessFlag,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_guess_flag,
            svgResId = R.drawable.flag
        ),
        Tool(
            name = R.string.tool_expense_splitter,
            screen = Screen.Meetings,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            summary = R.string.sum_tool_expense_splitter,
            svgResId = R.drawable.expense_splitter,
            isPro = true
        ),
        Tool(
            name = R.string.tool_dice,
            screen = Screen.Dice,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            summary = R.string.sum_tool_dice,
            svgResId = R.drawable.dice
        ),
        Tool(
            name = R.string.tool_quick_math,
            screen = Screen.QuickCalcs,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_quick_math,
            svgResId = R.drawable.quick_math
        ),
        Tool(
            name = R.string.tool_basic_phrases,
            screen = Screen.Quotes,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_general,
            summary = R.string.sum_tool_basic_phrases,
            svgResId = R.drawable.phrases
        ),
        Tool(
            name = R.string.tool_multiverse_me,
            screen = Screen.MultiverseMe,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_multiverse_me,
            svgResId = R.drawable.planet_ringed
        ),
        Tool(
            name = R.string.tool_guess_capital,
            screen = Screen.GuessCapital,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_guess_capital,
            svgResId = R.drawable.capitol
        ),
        Tool(
            name = R.string.tool_compass,
            screen = Screen.Compass,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_compass,
            svgResId = R.drawable.compass
        ),
        Tool(
            name = R.string.tool_todo_list,
            screen = Screen.Todo,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            summary = R.string.sum_tool_todo_list,
            svgResId = R.drawable.todo_list
        ),
        Tool(
            name = R.string.tool_event_tracker,
            screen = Screen.Events,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            summary = R.string.sum_tool_event_tracker,
            svgResId = R.drawable.events
        ),
        Tool(
            name = R.string.tool_compound_interest,
            screen = Screen.CompoundInterest,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_compound_interest,
            svgResId = R.drawable.interes_compuesto
        ),
        Tool(
            name = R.string.tool_scoreboard,
            screen = Screen.Scoreboard,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_scoreboards,
            summary = R.string.sum_tool_scoreboard,
            svgResId = R.drawable.scoreboard
        ),
        Tool(
            name = R.string.tool_magnifier,
            screen = Screen.Magnifier,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_magnifier,
            svgResId = R.drawable.magnifier
        ),
        Tool(
            name = R.string.tool_ar_ruler,
            screen = Screen.ArRuler,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            summary = R.string.sum_tool_ar_ruler,
            svgResId = R.drawable.ar_ruler,
            isPro = true
        ),
        Tool(
            name = R.string.tool_noise_generator,
            screen = Screen.Noise,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            summary = R.string.sum_tool_noise_generator,
            svgResId = R.drawable.noise_generator
        ),
        Tool(
            name = R.string.about_title,
            screen = Screen.About,
            category = ToolCategory.Favoritos,
            subCategory = R.string.subcategory_generator
        )
    )
}
