package com.joasasso.minitoolbox.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Pide/actualiza consentimiento con UMP y expone:
 *  - canRequestAds: si se pueden solicitar anuncios
 *  - privacyOptionsRequired: si corresponde mostrar el botón de "Opciones de privacidad"
 *
 * Úsalo en MainActivity: ConsentGateProvider { ProStateProvider { ... } }
 */
@Composable
fun ConsentGateProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val ran = rememberSaveable { mutableStateOf(false) }

    // Estado que vamos a proveer a toda la app
    var consentState by remember { mutableStateOf(ConsentState(false, false)) }

    LaunchedEffect(activity) {
        if (activity == null || ran.value) return@LaunchedEffect
        ran.value = true

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        val updateLocalState: () -> Unit = {
            val canRequest = consentInfo.canRequestAds()
            val privacyReq = consentInfo.privacyOptionsRequirementStatus ==
                    PrivacyOptionsRequirementStatus.REQUIRED
            consentState = ConsentState(
                canRequestAds = canRequest,
                privacyOptionsRequired = privacyReq
            )
        }

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Si hace falta, muestra formulario (se cierra solo)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updateLocalState()
                }
                updateLocalState()
            },
            { _ ->
                // En error, igual actualizamos lo que tengamos
                updateLocalState()
            }
        )
    }

    CompositionLocalProvider(LocalConsentState provides consentState) {
        content()
    }
}

/** Helper para mostrar el formulario de "Opciones de privacidad" cuando el botón esté visible. */
@Composable
fun rememberShowPrivacyOptionsAction(): () -> Unit {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    return remember(activity) {
        {
            activity?.let {
                UserMessagingPlatform.showPrivacyOptionsForm(it) {
                    // Al cerrarse, UMP ya habrá actualizado su estado interno si correspondía.
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
