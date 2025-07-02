// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.BrowseGallery
import androidx.compose.material.icons.filled.BrowserNotSupported
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TypeSpecimen
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import com.joasasso.minitoolbox.nav.Screen


object ToolRegistry {
    val tools = listOf(
        Tool(
            name = "Generador de Colores",
            screen = Screen.RandomColor,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.ColorLens
        ),
        Tool(
            name = "Generador de equipos",
            screen = Screen.GroupSelector,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.Groups
        ),
        Tool(
            name = "Cara o Cruz",
            screen = Screen.CoinFlip,
            category = ToolCategory.Entretenimiento,
            subCategory = "Juegos",
            icon = Icons.Filled.Casino
        ),
        Tool(
            name = "Conversor Decimal / Binario",
            screen = Screen.DecimalBinaryConverter,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.Code
        ),
        Tool(
            name = "Conversor Texto / Binario",
            screen = Screen.TextBinaryConverter,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.TextFields
        ),
        Tool(
            name = "Anotador Truco",
            screen = Screen.TrucoScoreBoard,
            category = ToolCategory.Entretenimiento,
            subCategory = "Juegos",
            icon = Icons.Filled.BrowserNotSupported
        ),
        Tool(
            name = "Calculadora de Edad",
            screen = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            subCategory = "Fechas",
            icon = Icons.Filled.CalendarMonth
    ),
        Tool(
            name = "Signo del Zodíaco",
            screen = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            subCategory = "Fechas",
            icon = Icons.Filled.DateRange
    ),
        Tool(
            name = "Temporizador Pomodoro",
            screen = Screen.Pomodoro,
            category = ToolCategory.Organizacion,
            subCategory = "Temporizadores",
            icon = Icons.Filled.Timer
    ),
        Tool(
            name = "Nivel Burbuja",
            screen = Screen.BubbleLevel,
            category = ToolCategory.Herramientas,
            subCategory = "Instrumentos",
            icon = Icons.Filled.BubbleChart
    ),
        Tool(
            name = "Calculadora de Porcentaje",
            screen = Screen.Porcentaje,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.Percent
    ),
        Tool(
            name = "Conversor de Horas 12h / 24h",
            screen = Screen.ConversorHoras,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.BrowseGallery
    ),
        Tool(
            name = "Calculadora de IMC",
            screen = Screen.CalculadoraDeIMC,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.Scale
    ),
        Tool(
            name = "Conversor Romano / Decimal",
            screen = Screen.ConversorRomanos,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.TypeSpecimen
        ),

        Tool(
            name = "Conversor de Unidades",
            screen = Screen.ConversorUnidades,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            icon = Icons.Filled.SwapHoriz
        ),
        Tool(
            name = "Generador de Contraseñas",
            screen = Screen.GeneradorContrasena,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.Password
        ),
        Tool(
            name = "Sugeridor de Actividades",
            screen = Screen.SugeridorActividades,
            category = ToolCategory.Organizacion,
            subCategory = "Hábitos y Actividades",
            icon = Icons.Filled.Lightbulb
        ),
        Tool(
            name = "Generador de Nombres",
            screen = Screen.GeneradorNombres,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.Person
    ),
        Tool(
            name = "Generador de QR",
            screen = Screen.GeneradorQR,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.QrCode
    ),
        Tool(
            name = "Generador de QR de Contacto (vCard)",
            screen = Screen.GeneradorVCard,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.Filled.Contacts
        ),
        Tool(
            name = "Generador de Lorem Ipsum",
            screen = Screen.LoremIpsum,
            category = ToolCategory.Herramientas,
            subCategory = "Generadores",
            icon = Icons.AutoMirrored.Filled.ShortText

        ),
        Tool(
            name = "Regla",
            screen = Screen.Regla,
            category = ToolCategory.Herramientas,
            subCategory = "Instrumentos",
            icon = Icons.Filled.Straighten
        ),
        Tool(
            name = "Medidor de Luz",
            screen = Screen.MedidorLuz,
            category = ToolCategory.Herramientas,
            subCategory = "Instrumentos",
            icon = Icons.Filled.WbSunny
        ),
        Tool(
            name = "Linterna",
            screen = Screen.Linterna,
            category = ToolCategory.Herramientas,
            subCategory = "Instrumentos",
            icon = Icons.Filled.FlashlightOn

        ),
        Tool(
            name = "Seguimiento de Hábitos",
            screen = Screen.Rachas,
            category = ToolCategory.Organizacion,
            subCategory = "Hábitos y Actividades",
            icon = Icons.Filled.Timelapse

        ),
        Tool(
            name = "Recordatorio de Beber Agua",
            screen = Screen.Agua,
            category = ToolCategory.Organizacion,
            subCategory = "Hábitos y Actividades",
            icon = Icons.Filled.WaterDrop
        ),
        Tool(
            name = "Cuenta regresiva de días",
            screen = Screen.TiempoHasta,
            category = ToolCategory.Informacion,
            subCategory = "Fechas",
            icon = Icons.Filled.HourglassBottom
        )
    )
}
