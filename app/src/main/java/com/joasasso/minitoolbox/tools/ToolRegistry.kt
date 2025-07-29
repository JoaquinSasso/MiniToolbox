// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.nav.Screen


object ToolRegistry {
    val tools = listOf(
        Tool(
            name = R.string.tool_color_generator,
            screen = Screen.RandomColor,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            icon = Icons.Filled.ColorLens
        ),
        Tool(
            name = R.string.tool_team_generator,
            screen = Screen.GroupSelector,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            icon = Icons.Filled.Groups
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
            icon = Icons.Filled.Code
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
            icon = Icons.Filled.Cake
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
            icon = Icons.Filled.Percent
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
            icon = Icons.Filled.Password
        ),
        Tool(
            name = R.string.tool_activity_suggester,
            screen = Screen.SugeridorActividades,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_habits,
            icon = Icons.Filled.Lightbulb
        ),
        Tool(
            name = R.string.tool_pomodoro_timer,
            screen = Screen.Pomodoro,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            icon = Icons.Filled.Timer
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
            icon = Icons.Filled.QrCode
    ),
        Tool(
            name = R.string.tool_qr_vcard,
            screen = Screen.GeneradorVCard,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            icon = Icons.Filled.PersonAdd
        ),
        Tool(
            name = R.string.tool_lorem_ipsum,
            screen = Screen.LoremIpsum,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_generator,
            icon = Icons.AutoMirrored.Filled.ShortText

        ),
        Tool(
            name = R.string.tool_ruler,
            screen = Screen.Regla,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            icon = Icons.Filled.Straighten
        ),
        Tool(
            name = R.string.tool_light_meter,
            screen = Screen.MedidorLuz,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            icon = Icons.Filled.WbSunny
        ),
        Tool(
            name = R.string.tool_flashlight,
            screen = Screen.Linterna,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_instrument,
            icon = Icons.Filled.FlashlightOn

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
            icon = Icons.Filled.WaterDrop
        ),
        Tool(
            name = R.string.tool_day_countdown,
            screen = Screen.TiempoHasta,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_dates,
            icon = Icons.Filled.Timelapse
        ),
        Tool(
            name = R.string.tool_country_info,
            screen = Screen.PaisesInfo,
            category = ToolCategory.Informacion,
            subCategory = R.string.subcategory_general,
            icon = Icons.Filled.TravelExplore
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
            icon = Icons.Filled.Flag
    ),
        Tool(
            name = R.string.tool_expense_splitter,
            screen = Screen.Reuniones,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            icon = Icons.Filled.Receipt
        ),
        Tool(
            name = R.string.tool_dice,
            screen = Screen.Dados,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_random,
            icon = Icons.Filled.Casino
        ),
        Tool(
            name = R.string.tool_quick_math,
            screen = Screen.CalculosRapidos,
            category = ToolCategory.Entretenimiento,
            subCategory = R.string.subcategory_minigames,
            icon = Icons.Filled.Calculate
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
            svgResId = R.drawable.arrow_north
        ),
        Tool(
            name = R.string.tool_todo_list,
            screen = Screen.ToDo,
            category = ToolCategory.Organizacion,
            subCategory = R.string.subcategory_others,
            icon = Icons.Filled.Checklist
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
            icon = Icons.Filled.Scoreboard
        )
    )
}
