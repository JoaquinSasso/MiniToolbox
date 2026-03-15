package com.joasasso.minitoolbox.utils.pro

import android.content.Context
import android.content.Intent
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.nav.Screen

fun paywallIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        // Necesario para abrir Actividades desde fuera (como un Widget)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("startRoute", Screen.Pro.route) // O putExtra("startRoute", "pro")
    }
}
