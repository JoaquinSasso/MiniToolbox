package com.example.minitoolbox

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minitoolbox.CategoriesScreen
import com.example.minitoolbox.tools.generadores.RandomColorGeneratorScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "categories") {
        composable("categories") {
            CategoriesScreen(onToolSelected = { tool ->
                when (tool) {
                    "Generador de colores" -> navController.navigate("random_color_generator")
                    // acá podés agregar más navegación para otras herramientas
                }
            })
        }
        composable("random_color_generator") {
            RandomColorGeneratorScreen(onBack = {
                navController.popBackStack()
            })
        }
    }
}


