package com.joasasso.minitoolbox.tools

import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.nav.Screen

object ToolRegistry {
    val tools = listOf(
        // --- Medir: Distancia ---
        Tool(
            name = R.string.tool_ar_ruler,
            screen = Screen.ArRuler,
            category = ToolCategory.Medir,
            subCategory = R.string.subcategory_distance,
            summary = R.string.sum_tool_ar_ruler,
            svgResId = R.drawable.ar_ruler
        ),
        Tool(
            name = R.string.tool_ruler,
            screen = Screen.Ruler,
            category = ToolCategory.Medir,
            subCategory = R.string.subcategory_distance,
            summary = R.string.sum_tool_ruler,
            svgResId = R.drawable.ruler
        ),

        // --- Medir: Sensores ---
        Tool(
            name = R.string.tool_bubble_level,
            screen = Screen.BubbleLevel,
            category = ToolCategory.Medir,
            subCategory = R.string.subcategory_sensors,
            summary = R.string.sum_tool_bubble_level,
            svgResId = R.drawable.bubble_level
        ),
        Tool(
            name = R.string.tool_compass,
            screen = Screen.Compass,
            category = ToolCategory.Medir,
            subCategory = R.string.subcategory_sensors,
            summary = R.string.sum_tool_compass,
            svgResId = R.drawable.compass
        ),
        Tool(
            name = R.string.tool_light_meter,
            screen = Screen.LightMeter,
            category = ToolCategory.Medir,
            subCategory = R.string.subcategory_sensors,
            summary = R.string.sum_tool_light_meter,
            svgResId = R.drawable.light_meter
        ),

        // --- Esenciales: Luz y lente ---
        Tool(
            name = R.string.tool_flashlight,
            screen = Screen.Flashlight,
            category = ToolCategory.Esenciales,
            subCategory = R.string.subcategory_light_lens,
            summary = R.string.sum_tool_flashlight,
            svgResId = R.drawable.flashlight
        ),
        Tool(
            name = R.string.tool_magnifier,
            screen = Screen.Magnifier,
            category = ToolCategory.Esenciales,
            subCategory = R.string.subcategory_light_lens,
            summary = R.string.sum_tool_magnifier,
            svgResId = R.drawable.magnifier
        ),

        // --- Esenciales: Conversión ---
        Tool(
            name = R.string.tool_unit_converter,
            screen = Screen.UnitConverter,
            category = ToolCategory.Esenciales,
            subCategory = R.string.subcategory_conversion,
            summary = R.string.sum_tool_unit_converter,
            svgResId = R.drawable.unit_conversor
        ),

        // --- Organizar: Rutinas ---
        Tool(
            name = R.string.tool_pomodoro_timer,
            screen = Screen.PomodoroList,
            category = ToolCategory.Organizar,
            subCategory = R.string.subcategory_routines,
            summary = R.string.sum_tool_pomodoro_timer,
            svgResId = R.drawable.timer,
            metricsKey = "pomodoro"
        ),
        Tool(
            name = R.string.tool_water_reminder,
            screen = Screen.Water,
            category = ToolCategory.Organizar,
            subCategory = R.string.subcategory_routines,
            summary = R.string.sum_tool_water_reminder,
            svgResId = R.drawable.water_drop
        ),

        // --- Organizar: Cuentas ---
        Tool(
            name = R.string.tool_expense_splitter,
            screen = Screen.Meetings,
            category = ToolCategory.Organizar,
            subCategory = R.string.subcategory_money,
            summary = R.string.sum_tool_expense_splitter,
            svgResId = R.drawable.expense_splitter
        ),

        // --- Generar: Códigos ---
        Tool(
            name = R.string.tool_qr_generator,
            screen = Screen.QrGenerator,
            category = ToolCategory.Generar,
            subCategory = R.string.subcategory_codes,
            summary = R.string.sum_tool_qr_generator,
            svgResId = R.drawable.qr_code
        ),

        // --- Generar: Sorteos ---
        Tool(
            name = R.string.tool_team_generator,
            screen = Screen.GroupSelector,
            category = ToolCategory.Generar,
            subCategory = R.string.subcategory_draws,
            summary = R.string.sum_tool_team_generator,
            svgResId = R.drawable.teamselector
        ),
        Tool(
            name = R.string.tool_option_selector,
            screen = Screen.SelectorWheel,
            category = ToolCategory.Generar,
            subCategory = R.string.subcategory_draws,
            summary = R.string.sum_tool_option_selector,
            svgResId = R.drawable.fortune_wheel
        ),

        // --- Jugar: Marcadores ---
        Tool(
            name = R.string.tool_truco_score,
            screen = Screen.TrucoScoreboard,
            category = ToolCategory.Jugar,
            subCategory = R.string.subcategory_scoreboards,
            summary = R.string.sum_tool_truco_score,
            svgResId = R.drawable.truco_square
        ),
        Tool(
            name = R.string.tool_scoreboard,
            screen = Screen.Scoreboard,
            category = ToolCategory.Jugar,
            subCategory = R.string.subcategory_scoreboards,
            summary = R.string.sum_tool_scoreboard,
            svgResId = R.drawable.scoreboard
        ),

        // --- Jugar: Minijuegos ---
        Tool(
            name = R.string.tool_minesweeper,
            screen = Screen.Minesweeper,
            category = ToolCategory.Jugar,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_minesweeper,
            svgResId = R.drawable.bomb
        ),
        Tool(
            name = R.string.tool_guess_flag,
            screen = Screen.GuessFlag,
            category = ToolCategory.Jugar,
            subCategory = R.string.subcategory_minigames,
            summary = R.string.sum_tool_guess_flag,
            svgResId = R.drawable.flag
        ),

        // --- Entrada técnica para continuidad de métricas (fuera del catálogo) ---
        Tool(
            name = R.string.about_title,
            screen = Screen.About,
            category = ToolCategory.Esenciales,
            subCategory = R.string.subcategory_system
        )
    )
}
