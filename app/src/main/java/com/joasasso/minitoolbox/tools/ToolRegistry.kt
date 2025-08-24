// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.joasasso.minitoolbox.tools
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.nav.Screen


object ToolRegistry {
    val tools = listOf(
        Tool(
            name = R.string.tool_color_generator,
            screen = Screen.RandomColor,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.colorgenerator
        ),
        Tool(
            name = R.string.tool_team_generator,
            screen = Screen.GroupSelector,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.teamselector
        ),
        Tool(
            name = R.string.tool_coin_flip,
            screen = Screen.CoinFlip,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            svgResId = R.drawable.coin
        ),
        Tool(
            name = R.string.tool_decimal_binary,
            screen = Screen.DecimalBinaryConverter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.decimal_binary
        ),
        Tool(
            name = R.string.tool_text_binary,
            screen = Screen.TextBinaryConverter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.binary
        ),
        Tool(
            name = R.string.tool_truco_score,
            screen = Screen.TrucoScoreBoard,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_scoreboards,
            svgResId = R.drawable.truco_square
        ),
        Tool(
            name = R.string.tool_age_calculator,
            screen = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            svgResId = R.drawable.age_calculator
    ),
        Tool(
            name = R.string.tool_zodiac_sign,
            screen = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            svgResId = R.drawable.zodiac_sign
    ),
        Tool(
            name = R.string.tool_bubble_level,
            screen = Screen.BubbleLevel,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.bubble_level
    ),
        Tool(
            name = R.string.tool_percentage,
            screen = Screen.Porcentaje,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.percentage
    ),
        Tool(
            name = R.string.tool_hour_converter,
            screen = Screen.ConversorHoras,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.clock_conversor
    ),
        Tool(
            name = R.string.tool_bmi_calculator,
            screen = Screen.CalculadoraDeIMC,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.scale
    ),
        Tool(
            name = R.string.tool_roman_converter,
            screen = Screen.ConversorRomanos,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.roman_pillar
        ),

        Tool(
            name = R.string.tool_unit_converter,
            screen = Screen.ConversorUnidades,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.unit_conversor
        ),
        Tool(
            name = R.string.tool_password_generator,
            screen = Screen.GeneradorContrasena,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.password
        ),
        Tool(
            name = R.string.tool_activity_suggester,
            screen = Screen.SugeridorActividades,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            svgResId = R.drawable.activity_suggester
        ),
        Tool(
            name = R.string.tool_pomodoro_timer,
            screen = Screen.Pomodoro,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            svgResId = R.drawable.timer
        ),
        Tool(
            name = R.string.tool_name_generator,
            screen = Screen.GeneradorNombres,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.id_card
    ),
        Tool(
            name = R.string.tool_qr_generator,
            screen = Screen.GeneradorQR,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.qr_code
    ),
        Tool(
            name = R.string.tool_qr_vcard,
            screen = Screen.GeneradorVCard,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.vcard
        ),
        Tool(
            name = R.string.tool_lorem_ipsum,
            screen = Screen.LoremIpsum,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            svgResId = R.drawable.lorem_ipsum

        ),
        Tool(
            name = R.string.tool_ruler,
            screen = Screen.Regla,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.ruler
        ),
        Tool(
            name = R.string.tool_light_meter,
            screen = Screen.MedidorLuz,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.light_meter
        ),
        Tool(
            name = R.string.tool_flashlight,
            screen = Screen.Linterna,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.flashlight

        ),
        Tool(
            name = R.string.tool_habit_tracker,
            screen = Screen.Rachas,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            svgResId = R.drawable.habits

        ),
        Tool(
            name = R.string.tool_water_reminder,
            screen = Screen.Agua,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            svgResId = R.drawable.water_drop
        ),
        Tool(
            name = R.string.tool_day_countdown,
            screen = Screen.TiempoHasta,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            svgResId = R.drawable.countdown
        ),
        Tool(
            name = R.string.tool_country_info,
            screen = Screen.PaisesInfo,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_general,
            svgResId = R.drawable.countries_info
        ),
        Tool(
            name = R.string.tool_option_selector,
            screen = Screen.RuletaSelectora,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            svgResId = R.drawable.fortune_wheel
    ),
        Tool(
            name = R.string.tool_guess_flag,
            screen = Screen.AdivinaBandera,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            svgResId = R.drawable.flag
    ),
        Tool(
            name = R.string.tool_expense_splitter,
            screen = Screen.Reuniones,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            svgResId = R.drawable.expense_splitter
        ),
        Tool(
            name = R.string.tool_dice,
            screen = Screen.Dados,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            svgResId = R.drawable.dice
        ),
        Tool(
            name = R.string.tool_quick_math,
            screen = Screen.CalculosRapidos,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            svgResId = R.drawable.quick_math
        ),
        Tool(
            name = R.string.tool_basic_phrases,
            screen = Screen.Frases,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_general,
            svgResId = R.drawable.phrases
        ),
        Tool(
            name = R.string.tool_multiverse_me,
            screen = Screen.MiYoDelMultiverso,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            svgResId = R.drawable.planet_ringed
        ),
        Tool(
            name = R.string.tool_guess_capital,
            screen = Screen.AdivinaCapital,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            svgResId = R.drawable.capitol
        ),
        Tool(
            name = R.string.tool_compass,
            screen = Screen.Brujula,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.compass
        ),
        Tool(
            name = R.string.tool_todo_list,
            screen = Screen.ToDo,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            svgResId = R.drawable.todo_list
        ),
        Tool(
            name = R.string.tool_event_tracker,
            screen = Screen.Eventos,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            svgResId = R.drawable.events
    ),
        Tool(
            name = R.string.tool_compound_interest,
            screen = Screen.InteresCompuesto,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            svgResId = R.drawable.interes_compuesto
        ),
        Tool(
            name = R.string.tool_scoreboard,
            screen = Screen.Scoreboard,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_scoreboards,
            svgResId = R.drawable.scoreboard
        ),
        Tool(
            name = R.string.tool_magnifier,
            screen = Screen.Magnifier,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.magnifier
        ),
        Tool(
            name = R.string.tool_ar_ruler,
            screen = Screen.ARRuler,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            svgResId = R.drawable.ar_ruler
        )
    )
}
