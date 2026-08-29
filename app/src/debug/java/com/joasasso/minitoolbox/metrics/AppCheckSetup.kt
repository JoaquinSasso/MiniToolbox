package com.joasasso.minitoolbox.metrics

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Variante debug: usa un token registrado a mano en la consola.
 * Este archivo NO se compila en release.
 */
object AppCheckSetup {
    fun install(context: Context) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
    }
}