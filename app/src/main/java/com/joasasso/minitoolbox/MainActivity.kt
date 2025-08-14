package com.joasasso.minitoolbox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme

class MainActivity : ComponentActivity() {

    private var startRouteState by mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startRouteState = intent?.getStringExtra("startRoute")

        setContent {
            MiniToolboxTheme {
                val navController = rememberNavController()

                LaunchedEffect(startRouteState) {
                    val route = startRouteState
                    if (Screen.isValidRoute(route) && route != Screen.Categories.route) {
                        navController.popBackStack(Screen.Categories.route, inclusive = false)
                        navController.navigate(route!!) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }

                MiniToolboxNavGraph(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRouteState = intent.getStringExtra("startRoute")
    }
}
