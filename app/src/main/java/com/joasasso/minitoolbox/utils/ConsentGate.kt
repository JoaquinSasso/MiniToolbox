package com.joasasso.minitoolbox.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext


// AdsInit.kt (crea este archivo en, por ej., app/src/main/java/.../ads/)
object AdsManager {
    private val _isReady = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isReady = _isReady as kotlinx.coroutines.flow.StateFlow<Boolean>

    fun canRequestAds(context: android.content.Context): Boolean {
        val info = com.google.android.ump.UserMessagingPlatform.getConsentInformation(context)
        return info.canRequestAds()
    }

    fun initialize(context: android.content.Context) {
        if (_isReady.value) return
        if (!canRequestAds(context)) return
        com.google.android.gms.ads.MobileAds.initialize(context) {
            _isReady.value = true
        }
    }
}

// Opcional para debuggeo del formulario UMP (reemplazá el hash por el de tu device)
private fun debugConsentParams(context: android.content.Context)
        : com.google.android.ump.ConsentRequestParameters {
    val builder = com.google.android.ump.ConsentRequestParameters.Builder()
//     Descomenta para forzar geografía EEA y test device:
     val debug = com.google.android.ump.ConsentDebugSettings.Builder(context)
         .setDebugGeography(
             com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
         )
         .addTestDeviceHashedId("5DC3A233719626CDADED71699AD49B77")
         .build()
     builder.setConsentDebugSettings(debug)
    return builder.build()
}

@Composable
fun ConsentGate() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val ran = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity == null || ran.value) return@LaunchedEffect
        ran.value = true

        val consentInfo = com.google.android.ump.UserMessagingPlatform.getConsentInformation(activity)
        val params = debugConsentParams(activity)

        // Siempre actualizamos estado de consentimiento al iniciar
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Mostrar formulario si hace falta (requiere Activity)
                com.google.android.ump.UserMessagingPlatform
                    .loadAndShowConsentFormIfRequired(activity) {
                        // Al cerrar el formulario (o si no hacía falta), inicializamos Ads si se puede
                        if (consentInfo.canRequestAds()) {
                            AdsManager.initialize(activity.applicationContext)
                        }
                    }
                // Puede que ya se pueda pedir anuncios sin necesidad de formulario
                if (consentInfo.canRequestAds()) {
                    AdsManager.initialize(activity.applicationContext)
                }
            },
            { _ /*error*/ ->
                // Si falla, igual intentamos inicializar si ya había consentimiento previo
                if (consentInfo.canRequestAds()) {
                    AdsManager.initialize(activity.applicationContext)
                }
            }
        )
    }
}

// Helper para obtener Activity desde un Context
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

