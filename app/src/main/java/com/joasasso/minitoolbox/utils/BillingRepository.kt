package com.joasasso.minitoolbox.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BillingRepository(
    private val appContext: Context,
    private val inappId: String,  // p.ej. context.getString(R.string.billing_pro_inapp)
    private val subId: String?    // p.ej. context.getString(R.string.billing_pro_sub) o null
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient =
        BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams
                    .newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    private val _entitlementEvents = MutableSharedFlow<ProState>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val entitlementEvents: SharedFlow<ProState> = _entitlementEvents

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        if (billingClient.isReady) { cont.resume(true); return@suspendCancellableCoroutine }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() {
                // Se reintentará en próxima llamada
            }
        })
    }

    suspend fun queryProductDetails(): List<ProductDetails> {
        if (!connect()) return emptyList()
        val list = mutableListOf<ProductDetails>()

        // INAPP
        val inappQuery = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(inappId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )).build()
        list += billingClient.queryProductDetails(inappQuery).productDetailsList ?: emptyList()

        // SUBS (opcional)
        if (!subId.isNullOrBlank()) {
            val subsQuery = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(subId!!)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )).build()
            list += billingClient.queryProductDetails(subsQuery).productDetailsList ?: emptyList()
        }
        return list
    }

    fun launchPurchase(activity: Activity, details: ProductDetails) {
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                if (offerToken != null) setOfferToken(offerToken)
            }
            .build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product))
            .build()

        billingClient.launchBillingFlow(activity, params)
    }

    /** Llamalo en onResume o al iniciar app para restaurar. */
    suspend fun restoreEntitlement() {
        if (!connect()) return
        // SUBS
        val subs = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ).purchasesList ?: emptyList()
        // INAPP
        val inapps = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ).purchasesList ?: emptyList()

        handlePurchases(subs + inapps, fromRestore = true)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases, fromRestore = false)
        }
        // Si el usuario cancela o falla, no hacemos nada
    }

    private fun handlePurchases(purchases: List<Purchase>, fromRestore: Boolean) {
        // Buscamos si hay una compra activa, ya sea sub o inapp, de nuestros IDs
        val active = purchases.firstOrNull { p ->
            p.products.any { it == inappId || it == subId }
                    && p.purchaseState == Purchase.PurchaseState.PURCHASED
        } ?: run {
            // No hay compras: emitimos estado no PRO solo si fue un restore explícito
            if (fromRestore) {
                emitPro(false, ProSource.None)
            }
            return
        }

        // Acknowledge si hace falta
        if (!active.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(active.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* no-op */ }
        }

        // Determinar source
        val src = when {
            active.products.contains(inappId) -> ProSource.Lifetime
            active.products.contains(subId)   -> ProSource.Subscription
            else                              -> ProSource.None
        }
        emitPro(true, src)
    }

    private fun emitPro(isPro: Boolean, src: ProSource) {
        _entitlementEvents.tryEmit(
            ProState(isPro = isPro, source = src, sinceMillis = System.currentTimeMillis())
        )
    }
}


