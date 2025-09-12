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
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

@Composable
fun ConsentGate() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val ran = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity == null || ran.value) return@LaunchedEffect
        ran.value = true

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consentInfo.canRequestAds()) {
                        AdsManager.initialize(activity.applicationContext)
                    }
                }
                if (consentInfo.canRequestAds()) {
                    AdsManager.initialize(activity.applicationContext)
                }
            },
            { _ ->
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
