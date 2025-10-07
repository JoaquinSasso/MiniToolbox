package com.joasasso.minitoolbox.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.joasasso.minitoolbox.R

fun Context.openPrivacyUrl(url: String) {
    val uri = url.toUri()
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .build()
            .launchUrl(this, uri)
    }.onFailure {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_navigator_available), Toast.LENGTH_SHORT).show()
        }
    }
}
