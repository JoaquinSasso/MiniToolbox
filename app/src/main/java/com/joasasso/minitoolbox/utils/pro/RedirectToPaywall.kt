package com.joasasso.minitoolbox.utils.pro

import android.content.Context
import android.content.Intent
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.nav.Screen

fun paywallIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        // Desde widget: nueva task y reusar Activity si ya está arriba
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        putExtra("startRoute", Screen.Pro.route)
    }
