// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.example.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.*          // ya lo tienes
import com.example.minitoolbox.nav.Screen
import androidx.compose.material.icons.filled.*



object ToolRegistry {
    val tools = listOf(
        Tool(
            name = "Generador de Colores",
            screen = Screen.RandomColor,
            category = ToolCategory.Generadores, // <--- Debe ser exactamente este objeto
            icon = Icons.Filled.ColorLens
        ),
        Tool(
            name = "Selector de Grupos",
            screen = Screen.GroupSelector,
            category = ToolCategory.Generadores, // <--- Igual aquí
            icon = Icons.Filled.Groups
        ),
        Tool(
            name = "Cara o Cruz",
            screen = Screen.CoinFlip,
            category = ToolCategory.Juegos,
            icon = Icons.Filled.Casino
        ),

        // Nueva herramienta:
        Tool(
            name = "Conversor Decimal / Binario",
            screen = Screen.DecimalBinaryConverter,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.Code
        ),
        Tool(
            name = "Conversor Texto / Binario",
            screen = Screen.TextBinaryConverter,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.TextFields
        ),
        Tool(
            name = "Anotador Truco",
            screen = Screen.TrucoScoreBoard,
            category = ToolCategory.Juegos,
            icon = Icons.Filled.BrowserNotSupported
        ),
        Tool(
            name = "Calculadora de Edad",
            screen = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            icon = Icons.Filled.CalendarMonth
    ),
        Tool(
            name = "Signo del Zodíaco",
            screen = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            icon = Icons.Filled.DateRange
    ),
        Tool(
            name = "Temporizador Pomodoro",
            screen = Screen.Pomodoro,
            category = ToolCategory.Recordatorios,
            icon = Icons.Filled.Timer
    ),
        Tool(
            name = "Nivel Burbuja",
            screen = Screen.BubbleLevel,
            category = ToolCategory.Herramientas,
            icon = Icons.Filled.BubbleChart
    ),
        Tool(
            name = "Calculadora de Porcentaje",
            screen = Screen.Porcentaje,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.Percent
    ),
        Tool(
            name = "Conversor de Horas",
            screen = Screen.ConversorHoras,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.BrowseGallery
    ),
        Tool(
            name = "Calculadora de IMC",
            screen = Screen.CalculadoraDeIMC,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.Scale
    ),
        Tool(
            name = "Conversor Romano / Arábigo",
            screen = Screen.ConversorRomanos,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.TypeSpecimen
        ),

        Tool(
            name = "Conversor de Unidades",
            screen = Screen.ConversorUnidades,
            category = ToolCategory.Calculadoras,
            icon = Icons.Filled.SwapHoriz
        ),
        Tool(
            name = "Generador de Contraseñas",
            screen = Screen.GeneradorContrasena,
            category = ToolCategory.Generadores,
            icon = Icons.Filled.Password
        ),
        Tool(
            name = "Sugeridor de Actividades",
            screen = Screen.SugeridorActividades,
            category = ToolCategory.Generadores,
            icon = Icons.Filled.Lightbulb
        ),
        Tool(
            name = "Generador de Nombres",
            screen = Screen.GeneradorNombres,
            category = ToolCategory.Generadores,
            icon = Icons.Filled.Person
    ),
        Tool(
            name = "Generador de QR",
            screen = Screen.GeneradorQR,
            category = ToolCategory.Generadores,
            icon = Icons.Filled.QrCode
    ),
        Tool(
            name = "Generador de QR de Contacto (vCard)",
            screen = Screen.GeneradorVCard,
            category = ToolCategory.Generadores,
            icon = Icons.Filled.Contacts
        ),
        Tool(
            name = "Generador de Lorem Ipsum",
            screen = Screen.LoremIpsum,
            category = ToolCategory.Generadores,
            icon = Icons.AutoMirrored.Filled.ShortText

        ),
        Tool(
            name = "Regla",
            screen = Screen.Regla,
            category = ToolCategory.Herramientas,
            icon = Icons.Filled.Straighten
        ),
        Tool(
            name = "Medidor de Luz",
            screen = Screen.MedidorLuz,
            category = ToolCategory.Herramientas,
            icon = Icons.Filled.WbSunny
        ),
        Tool(
            name = "Linterna",
            screen = Screen.Linterna,
            category = ToolCategory.Herramientas,
            icon = Icons.Filled.FlashlightOn

        ),
        Tool(
            name = "Contador de dias en Racha",
            screen = Screen.Rachas,
            category = ToolCategory.Recordatorios,
            icon = Icons.Filled.Timelapse

        ),
        Tool(
            name = "Recordatorio de Beber Agua",
            screen = Screen.Agua,
            category = ToolCategory.Recordatorios,
            icon = Icons.Filled.WaterDrop
        )
    )
}
