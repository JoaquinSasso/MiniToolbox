package com.joasasso.minitoolbox

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.utils.openPrivacyUrl

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenDevTools: () -> Unit
) {
    val context = LocalContext.current
    val url = stringResource(R.string.privacy_policy_url)

    // Version name segura (sin BuildConfig)
    val versionName by remember {
        mutableStateOf(
            runCatching {
                val pm: PackageManager = context.packageManager
                val pkg = context.packageName
                val info = pm.getPackageInfo(pkg, 0)
                (info.versionName ?: "1.0")
            }.getOrDefault("1.0")
        )
    }

    val flaticonAuthors = stringArrayResource(R.array.about_flaticon_authors)

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.about_title), onBack)
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ===== 1) SOBRE LA APP =====

            ElevatedCard(colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier =  Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(text = stringResource(R.string.about_section_app_title))
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = stringResource(R.string.about_version_label, versionName),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.about_app_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }


            // ===== 2) SOBRE MÍ =====

            ElevatedCard(colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier =  Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(text = stringResource(R.string.about_section_me_title))
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = stringResource(R.string.about_me_blurb),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            // ===== 3) CRÉDITOS =====

            ElevatedCard(colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier =  Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(text = stringResource(R.string.about_section_credits_title))
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    // Uicons
                    Text(stringResource(R.string.about_icons_uicons_line))
                    // Material Icons
                    Text(stringResource(R.string.about_material_icons_line))

                    // Autores Flaticon
                    Text(
                        text = stringResource(R.string.about_flaticon_authors_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    flaticonAuthors.forEach { author ->
                        Text(stringResource(R.string.about_flaticon_line_per_author, author))
                    }

                    // Línea “comodín” por si faltó alguno
                    Text(stringResource(R.string.about_flaticon_and_others))
                }
            }
            HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
            // ===== 4) BOTONES =====

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { context.openPrivacyUrl(url) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_privacy))
                }
                Button(
                    onClick = onOpenLicenses,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_licenses_button))
                }
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = onOpenDevTools,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Developer Metrics Tools")
                    }
                }
            }
            PrivacyOptionsButton()
            HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outlineVariant)
            // Espaciado final
            Spacer(Modifier.height(6.dp))

            // Firma pequeñita
            Text(
                text = stringResource(R.string.about_footer_signature),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
fun PrivacyOptionsButton(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var required by remember { mutableStateOf(false) }

    // Este valor puede cambiar después de ConsentGate; refrescamos al entrar
    LaunchedEffect(Unit) {
        val info = UserMessagingPlatform.getConsentInformation(context)
        required = (info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
    }

    if (required) {
        // Se muestra solo cuando corresponde (EEA/UK o ciertos estados de EE.UU.)
        Button(
            onClick = {
                activity?.let { act ->
                    com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(act) { /* FormError? */ }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.privacy_options_button))
        }
    }
}


private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


