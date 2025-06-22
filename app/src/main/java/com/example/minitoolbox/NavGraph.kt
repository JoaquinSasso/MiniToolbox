// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.example.minitoolbox

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minitoolbox.tools.ToolRegistry
import com.example.minitoolbox.tools.calculadoras.*
import com.example.minitoolbox.tools.generadores.*
import com.example.minitoolbox.tools.juegos.*
import com.example.minitoolbox.nav.Screen

@Composable
fun MiniToolboxNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Categories.route
    ) {
        composable(Screen.Categories.route) {
            CategoriesScreen(
                tools       = ToolRegistry.tools,
                onToolClick = { tool ->
                    // Navegamos usando la ruta definida en Screen, no el objeto Tool
                    navController.navigate(tool.screen.route)
                }
            )
        }
        composable(Screen.RandomColor.route) {
            RandomColorGeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GroupSelector.route) {
            GroupSelectorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CoinFlip.route) {
            CoinFlipScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DecimalBinaryConverter.route) {
            DecimalBinaryConverterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TextBinaryConverter.route) {
            TextBinaryConverterScreen(onBack = { navController.popBackStack() })
        }
    }
}
