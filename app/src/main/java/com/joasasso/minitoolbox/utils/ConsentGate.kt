package com.joasasso.minitoolbox.utils
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.*

object AdsManager {
    private val _ready = mutableStateOf(false)
    val isReady: State<Boolean> get() = _ready
    fun initialize(ctx: Context) {
        if (_ready.value) return
        MobileAds.initialize(ctx) { _ready.value = true }
    }
}

@Composable
fun ConsentGate(debugEEA: Boolean = true, testDeviceHash: String? = null) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val ran = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity == null || ran.value) return@LaunchedEffect
        ran.value = true

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().apply {
            if (debugEEA) {
                val dbg = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                testDeviceHash?.let { dbg.addTestDeviceHashedId(it) }
                setConsentDebugSettings(dbg.build())
            }
        }.build()

        // 1) Actualizar estado
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 2) Mostrar formulario si hace falta (solo Activity)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // 3) Al cerrar (o si no hacía falta), si se puede pedir anuncios, inicializar SDK
                    if (consentInfo.canRequestAds()) {
                        AdsManager.initialize(activity.applicationContext)
                    }
                }
                // Si no hacía falta formulario pero ya se puede pedir ads
                if (consentInfo.canRequestAds()) {
                    AdsManager.initialize(activity.applicationContext)
                }
            },
            { _ /*error*/ ->
                // Si falló el update, igual inicializar si ya había consentimiento previo
                if (consentInfo.canRequestAds()) {
                    AdsManager.initialize(activity.applicationContext)
                }
            }
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
