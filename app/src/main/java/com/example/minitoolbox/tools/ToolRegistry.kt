// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.example.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*          // ya lo tienes
import com.example.minitoolbox.nav.Screen

object ToolRegistry {
    val tools = listOf(
        Tool(
            name     = "Generador de Colores",
            screen   = Screen.RandomColor,
            category = ToolCategory.Generadores, // <--- Debe ser exactamente este objeto
            icon     = Icons.Filled.ColorLens
        ),
        Tool(
            name     = "Selector de Grupos",
            screen   = Screen.GroupSelector,
            category = ToolCategory.Generadores, // <--- Igual aquí
            icon     = Icons.Filled.Groups
        ),
        Tool(
            name     = "Cara o Cruz",
            screen   = Screen.CoinFlip,
            category = ToolCategory.Juegos,
            icon     = Icons.Filled.Casino
        ),

        // Nueva herramienta:
        Tool(
            name     = "Conversor Decimal / Binario",
            screen   = Screen.DecimalBinaryConverter,
            category = ToolCategory.Calculadoras,
            icon     = Icons.Filled.Code
        ),
        Tool(
            name     = "Conversor Texto / Binario",
            screen   = Screen.TextBinaryConverter,
            category = ToolCategory.Calculadoras,
            icon     = Icons.Filled.TextFields
        ),
        Tool(
            name     = "Anotador Truco",
            screen   = Screen.TrucoScoreBoard,
            category = ToolCategory.Juegos,
            icon     = Icons.Filled.BrowserNotSupported
        ),
        Tool(
            name     = "Calculadora de Edad",
            screen   = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            icon     = Icons.Filled.CalendarMonth
    ),
        Tool(
            name     = "Signo del Zodíaco",
            screen   = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            icon     = Icons.Filled.DateRange
    ),
        Tool(
            name     = "Temporizador Pomodoro",
            screen   = Screen.Pomodoro,
            category = ToolCategory.Recordatorios,
            icon     = Icons.Filled.Timer
    ),
        Tool(
            name     = "Nivel Burbuja",
            screen   = Screen.BubbleLevel,
            category = ToolCategory.Medicion,
            icon     = Icons.Filled.BubbleChart
    )
    )
}
