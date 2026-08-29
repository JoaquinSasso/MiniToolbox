package com.joasasso.minitoolbox.metrics

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Variante release: atestación real vía Play Integrity.
 * Sólo funciona si la app fue instalada desde Google Play.
 */
object AppCheckSetup {
    fun install(context: Context) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
    }
}