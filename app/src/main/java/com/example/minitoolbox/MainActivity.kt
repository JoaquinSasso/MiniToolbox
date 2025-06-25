// app/src/main/java/com/example/minitoolbox/MainActivity.kt
package com.example.minitoolbox

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.minitoolbox.ui.theme.MiniToolboxTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniToolboxTheme {
                // 1. Creamos el NavController
                val navController = rememberNavController()
                // 2. Leemos el extra enviado desde la notificación
                val startRoute = intent?.getStringExtra("startRoute")
                // 3. Si es "pomodoro", navegamos allí apenas arranque
                LaunchedEffect(startRoute) {
                    if (startRoute == "pomodoro") {
                        navController.navigate("pomodoro") {
                            // opcional: popUpTo para limpiar la pila
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }
                // 4. Tu Surface + NavGraph exactamente como antes
                Surface(color = MaterialTheme.colorScheme.background) {
                    MiniToolboxNavGraph(navController = navController)
                }
            }
        }
    }
}
