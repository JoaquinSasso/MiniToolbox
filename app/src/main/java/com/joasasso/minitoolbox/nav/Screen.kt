package com.joasasso.minitoolbox.nav

/**
 * Centraliza todas las rutas de navegación y sus títulos.
 */
sealed class Screen(val route: String) {
    object Categories    : Screen("categories")
    object RandomColor   : Screen("random_color_generator")
    object GroupSelector : Screen("group_selector")
    object CoinFlip      : Screen("coin_flip")
    object DecimalBinaryConverter       : Screen("decimal_binary_converter")
    object TextBinaryConverter          : Screen("text_binary_converter")
    object TrucoScoreBoard              : Screen("truco_score_board")
    object AgeCalculator                : Screen("age_calculator")
    object ZodiacSign                   : Screen("zodiac_sign")
    object Pomodoro                     : Screen("pomodoro")
    object BubbleLevel                  : Screen("bubble_level")
    object Porcentaje                   : Screen("porcentaje")
    object ConversorHoras               : Screen("conversor_horas")
    object CalculadoraDeIMC             : Screen("calculadora_de_imc")
    object ConversorRomanos             : Screen("conversor_romanos")
    object ConversorUnidades            : Screen("conversor_unidades")
    object GeneradorContrasena          : Screen("generador_contrasena")
    object SugeridorActividades        : Screen("sugeridor_actividades")
    object GeneradorNombres            : Screen("generador_nombres")
    object GeneradorQR                 : Screen("generador_qr")
    object GeneradorVCard              : Screen("generador_vcard")
    object LoremIpsum                   : Screen("lorem_ipsum")
    object Regla               : Screen("regla")
    object MedidorLuz               : Screen("medidor_luz")
    object Linterna               : Screen("linterna")
    object Rachas               : Screen("rachas")
    object Agua               : Screen("agua")
    object EstadisticasAgua               : Screen("estadisticas_agua")
    object TiempoHasta               : Screen("tiempo_hasta")

    // Añade aquí nuevos objetos para cada herramienta…
}
