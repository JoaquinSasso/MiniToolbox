package com.example.minitoolbox.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minitoolbox.CategoriesScreen
import com.example.minitoolbox.tools.generadores.RandomColorGeneratorScreen
@Composable
fun MiniToolboxNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "categories"
    ) {
        composable("categories") {
            CategoriesScreen(
                onToolSelected = { tool ->
                    when (tool) {
                        "Generador de colores" -> navController.navigate("random_color_generator")
                        else -> {} // Agrega más rutas según lo necesites
                    }
                }
            )
        }

        composable("random_color_generator") {
            RandomColorGeneratorScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
