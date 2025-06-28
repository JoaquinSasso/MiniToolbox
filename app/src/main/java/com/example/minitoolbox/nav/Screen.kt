package com.example.minitoolbox.nav

import androidx.annotation.StringRes
import com.example.minitoolbox.R

/**
 * Centraliza todas las rutas de navegación y sus títulos.
 */
sealed class Screen(val route: String, @StringRes val titleRes: Int) {
    object Categories    : Screen("categories",       R.string.categories_title)
    object RandomColor   : Screen("random_color_generator", R.string.random_color_title)
    object GroupSelector : Screen("group_selector",   R.string.group_selector_title)
    object CoinFlip      : Screen("coin_flip",        R.string.coin_flip_title)
    object DecimalBinaryConverter       : Screen("decimal_binary_converter", R.string.decimal_binary_converter_title)
    object TextBinaryConverter          : Screen("text_binary_converter", R.string.text_binary_converter_title)
    object TrucoScoreBoard              : Screen("truco_score_board", R.string.truco_score_board_title)
    object AgeCalculator                : Screen("age_calculator", R.string.age_calculator_title)
    object ZodiacSign                   : Screen("zodiac_sign", R.string.zodiac_sign_title)
    object Pomodoro                     : Screen("pomodoro", R.string.pomodoro_title)
    object BubbleLevel                  : Screen("bubble_level", R.string.bubble_level_title)
    object Porcentaje                   : Screen("porcentaje", R.string.porcentaje_title)
    object ConversorHoras               : Screen("conversor_horas", R.string.conversor_horas_title)
    object CalculadoraDeIMC             : Screen("calculadora_de_imc", R.string.calculadora_de_imc_title)
    object ConversorRomanos             : Screen("conversor_romanos", R.string.conversor_romanos_title)
    object ConversorUnidades            : Screen("conversor_unidades", R.string.conversor_unidades_title)
    object GeneradorContrasena          : Screen("generador_contrasena", R.string.generador_contrasena_title)
    object SugeridorActividades        : Screen("sugeridor_actividades", R.string.sugeridor_actividades_title)
    object GeneradorNombres            : Screen("generador_nombres", R.string.generador_nombres_title)
    object GeneradorQR                 : Screen("generador_qr", R.string.generador_qr_title)
    object GeneradorVCard              : Screen("generador_vcard", R.string.generador_vcard_title)
    object LoremIpsum                   : Screen("lorem_ipsum", R.string.lorem_ipsum_title)
    object Regla               : Screen("regla", R.string.regla_title)
    object MedidorLuz               : Screen("medidor_luz", R.string.medidor_luz_title)
    object Linterna               : Screen("linterna", R.string.linterna_title)
    object Rachas               : Screen("rachas", R.string.rachas_title)
    object Agua               : Screen("agua", R.string.agua_title)
    object EstadisticasAgua               : Screen("estadisticas_agua", R.string.estadisticas_agua_title)

    // Añade aquí nuevos objetos para cada herramienta…
}
