package com.joasasso.minitoolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiniToolboxTheme {
                val navController = rememberNavController()
                val startRoute = intent?.getStringExtra("startRoute")

                LaunchedEffect(startRoute) {
                    if (Screen.isValidRoute(startRoute)) {
                        navController.navigate(startRoute!!) {
                            popUpTo(0) { inclusive = false }
                        }
                    }
                }
                MiniToolboxNavGraph(navController = navController)
            }
        }
    }
}
