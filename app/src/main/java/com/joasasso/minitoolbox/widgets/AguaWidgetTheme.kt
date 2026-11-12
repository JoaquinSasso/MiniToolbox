package com.joasasso.minitoolbox.widgets

import android.os.Build
import androidx.glance.color.ColorProviders
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.material3.ColorProviders
import com.joasasso.minitoolbox.ui.theme.DarkColorScheme
import com.joasasso.minitoolbox.ui.theme.LightColorScheme

/**
 * Colores de los widgets de agua, con el mismo criterio que MiniToolboxTheme: dinámicos desde
 * API 31 y el esquema estático de la app por debajo. Sin esto, en API 28-30 Glance usa su paleta
 * genérica y el widget no se parece a la app.
 */
internal fun aguaWidgetColors(): ColorProviders =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DynamicThemeColorProviders
    } else {
        ColorProviders(light = LightColorScheme, dark = DarkColorScheme)
    }
