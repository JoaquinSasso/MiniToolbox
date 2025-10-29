// ProScreen.kt
package com.joasasso.minitoolbox.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.WebAssetOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.utils.pro.BillingClientWrapper
import com.joasasso.minitoolbox.utils.pro.PurchaseStatus

@Composable
fun ProScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inappId = remember { context.getString(R.string.billing_pro_id) }

    val billingWrapper = remember {
        BillingClientWrapper(
            context = context,
            scope = scope,
            inappId = inappId
        )
    }

    LaunchedEffect(Unit) { billingWrapper.initialize() }
    DisposableEffect(Unit) { onDispose { billingWrapper.destroy() } }

    val productDetails by billingWrapper.productDetails.collectAsState()
    val purchaseStatus by billingWrapper.purchaseStatus.collectAsState()

    LaunchedEffect(purchaseStatus) {
        when (purchaseStatus) {
            is PurchaseStatus.Success -> {
                Toast.makeText(context, context.getString(R.string.pro_toast_thanks), Toast.LENGTH_SHORT).show()
                onBack()
            }
            is PurchaseStatus.Failed -> {
                Toast.makeText(context, (purchaseStatus as PurchaseStatus.Failed).message, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    val productPrice = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
    val isPurchasing = purchaseStatus is PurchaseStatus.InProgress

    Scaffold(
        topBar = { TopBarReusable(title = stringResource(R.string.pro_screen_title), onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = stringResource(R.string.pro_feature_icon),
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text(text = stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pro_description),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))

            BenefitItem(icon = Icons.Default.WebAssetOff,
                title = stringResource(R.string.pro_benefit_ads_title),
                description = stringResource(R.string.pro_benefit_ads_desc))
            BenefitItem(icon = Icons.Default.LockOpen,
                title = stringResource(R.string.pro_benefit_tools_title),
                description = stringResource(R.string.pro_benefit_tools_desc))
            BenefitItem(icon = Icons.Default.Widgets,
                title = stringResource(R.string.pro_benefit_widgets_title_short),
                description = stringResource(R.string.pro_benefit_widgets_desc_short))
            BenefitItem(icon = Icons.Default.AllInclusive,
                title = stringResource(R.string.pro_benefit_lifetime_title),
                description = stringResource(R.string.pro_benefit_lifetime_desc))
            BenefitItem(icon = Icons.Default.Favorite,
                title = stringResource(R.string.pro_benefit_support_title),
                description = stringResource(R.string.pro_benefit_support_desc))

            Spacer(Modifier.weight(1f))

//            Button(  //TODO Descomentar cuando se implemente el modo PRO completamente
//                onClick = {
//                    val activity = context.findActivity()
//                    if (activity != null) {
//                        billingWrapper.launchPurchaseFlow(activity)
//                    } else {
//                        Toast.makeText(context, context.getString(R.string.pro_toast_warning_unable_to_buy), Toast.LENGTH_SHORT).show()
//                    }
//                },
//                modifier = Modifier.fillMaxWidth(),
//                enabled = !isPurchasing && productDetails != null
//            ) {
//                if (isPurchasing) {
//                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                } else {
//                    Text(stringResource(R.string.pro_buy_button, productPrice.ifBlank { "—" }))
//                }
//            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text(stringResource(R.string.pro_buy_button_disabled))
            }

//            Spacer(Modifier.height(8.dp)) //TODO Descomentar cuando se implemente el modo PRO completamente
//
//            TextButton(onClick = { billingWrapper.checkAndRestorePurchases() }) {
//                Text(stringResource(R.string.pro_restore_button))
//            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun BenefitItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
