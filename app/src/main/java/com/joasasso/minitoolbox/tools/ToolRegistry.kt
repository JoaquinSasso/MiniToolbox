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
            svgResId = R.drawable.bubble_level
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
            name = R.string.tool_truco_score,
            screen = Screen.TrucoScoreboard,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_scoreboards,
            summary = R.string.sum_tool_truco_score,
            svgResId = R.drawable.truco_square
        ),
        Tool(
            name = R.string.tool_unit_converter,
            screen = Screen.UnitConverter,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_calculator,
            summary = R.string.sum_tool_unit_converter,
            svgResId = R.drawable.unit_conversor
        ),
        Tool(
            name = R.string.tool_pomodoro_timer,
            screen = Screen.PomodoroList,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_others,
            summary = R.string.sum_tool_pomodoro_timer,
            svgResId = R.drawable.timer,
            metricsKey = "pomodoro"
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
            name = R.string.tool_water_reminder,
            screen = Screen.Water,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_habits,
            summary = R.string.sum_tool_water_reminder,
            svgResId = R.drawable.water_drop
        ),
        Tool(
            name = R.string.tool_option_selector,
            screen = Screen.SelectorWheel,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_random,
            summary = R.string.sum_tool_option_selector,
            svgResId = R.drawable.fortune_wheel
        ),
        Tool(
            name = R.string.tool_guess_flag,
            screen = Screen.GuessFlag,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_guess_flag,
            svgResId = R.drawable.flag
        ),
        Tool(
            name = R.string.tool_expense_splitter,
            screen = Screen.Meetings,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_others,
            summary = R.string.sum_tool_expense_splitter,
            svgResId = R.drawable.expense_splitter
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
            name = R.string.tool_scoreboard,
            screen = Screen.Scoreboard,
            category = ToolCategory.Herramientas,
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
            svgResId = R.drawable.ar_ruler
        ),
        Tool(
            name = R.string.about_title,
            screen = Screen.About,
            category = ToolCategory.Favoritos,
            subCategory = R.string.subcategory_generator
        ),
        Tool(
            name = R.string.tool_minesweeper,
            screen = Screen.Minesweeper,
            category = ToolCategory.Herramientas,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_minesweeper,
            svgResId = R.drawable.bomb
        )
    )
}
