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
    // Añade aquí nuevos objetos para cada herramienta…
}
