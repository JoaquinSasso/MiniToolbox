// app/src/main/java/com/example/minitoolbox/tools/ToolRegistry.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.BrowserNotSupported
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Casino
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
            subCategory = "Aleatorio",
            svgResId = R.drawable.coin
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
            svgResId = R.drawable.binary
        ),
        Tool(
            name = "Anotador Truco",
            screen = Screen.TrucoScoreBoard,
            category = ToolCategory.Entretenimiento,
            subCategory = "Marcadores",
            icon = Icons.Filled.BrowserNotSupported
        ),
        Tool(
            name = "Calculadora de Edad",
            screen = Screen.AgeCalculator,
            category = ToolCategory.Informacion,
            subCategory = "Fechas",
            icon = Icons.Filled.Cake
    ),
        Tool(
            name = "Signo del Zodíaco",
            screen = Screen.ZodiacSign,
            category = ToolCategory.Informacion,
            subCategory = "Fechas",
            svgResId = R.drawable.zodiac_sign
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
            svgResId = R.drawable.bubble_level
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
            svgResId = R.drawable.clock_conversor
    ),
        Tool(
            name = "Calculadora de IMC",
            screen = Screen.CalculadoraDeIMC,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            svgResId = R.drawable.scale
    ),
        Tool(
            name = "Conversor Romano / Decimal",
            screen = Screen.ConversorRomanos,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            svgResId = R.drawable.roman_pillar
        ),

        Tool(
            name = "Conversor de Unidades",
            screen = Screen.ConversorUnidades,
            category = ToolCategory.Herramientas,
            subCategory = "Calculadoras",
            svgResId = R.drawable.unit_conversor
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
            svgResId = R.drawable.id_card
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
            icon = Icons.Filled.PersonAdd
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
            svgResId = R.drawable.habits

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
            icon = Icons.Filled.Timelapse
        ),
        Tool(
            name = "Información de Países",
            screen = Screen.PaisesInfo,
            category = ToolCategory.Informacion,
            subCategory = "General",
            icon = Icons.Filled.TravelExplore
        ),
        Tool(
            name = "Selector de Opciones",
            screen = Screen.RuletaSelectora,
            category = ToolCategory.Entretenimiento,
            subCategory = "Aleatorio",
            svgResId = R.drawable.fortune_wheel
    ),
        Tool(
            name = "Adivina la Bandera",
            screen = Screen.AdivinaBandera,
            category = ToolCategory.Entretenimiento,
            subCategory = "Minijuego",
            icon = Icons.Filled.Flag
    ),
        Tool(
            name = "Divisor de Gastos",
            screen = Screen.Reuniones,
            category = ToolCategory.Organizacion,
            subCategory = "Reuniones",
            icon = Icons.Filled.Receipt
        ),
        Tool(
            name = "Dados (D&D)",
            screen = Screen.Dados,
            category = ToolCategory.Entretenimiento,
            subCategory = "Aleatorio",
            icon = Icons.Filled.Casino
        )
    )
}
