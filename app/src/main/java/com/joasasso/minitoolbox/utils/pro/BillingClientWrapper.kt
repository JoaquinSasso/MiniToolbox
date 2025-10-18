// BillingClientWrapper.kt
package com.joasasso.minitoolbox.utils.pro

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingClientWrapper(
    private val context: Context,
    private val scope: CoroutineScope,
    private val inappId: String
) {
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _purchaseStatus = MutableStateFlow<PurchaseStatus>(PurchaseStatus.Idle)
    val purchaseStatus: StateFlow<PurchaseStatus> = _purchaseStatus.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (p in purchases) {
                scope.launch { processPurchase(p) }
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseStatus.value = PurchaseStatus.Idle
        } else {
            _purchaseStatus.value = PurchaseStatus.Failed("Error: ${result.debugMessage}")
        }
    }

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    fun initialize() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "BillingClient conectado.")
                    queryProductDetails()
                    checkAndRestorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d("Billing", "BillingClient desconectado.")
            }
        })
    }

    private fun isOurProduct(purchase: Purchase): Boolean =
        purchase.products.contains(inappId)

    private fun queryProductDetails() = scope.launch {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(inappId)
                .setProductType(ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        } else {
            _purchaseStatus.value = PurchaseStatus.Failed("No se pudo obtener el producto (${result})")
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val product = _productDetails.value ?: run {
            _purchaseStatus.value = PurchaseStatus.Failed("Producto no disponible")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
        _purchaseStatus.value = PurchaseStatus.InProgress
    }

    private suspend fun processPurchase(purchase: Purchase) {
        if (!isOurProduct(purchase)) return

        if (purchase.purchaseState == PurchaseState.PURCHASED) {
            // Persistimos PRO para offline
            ProRepository.setProStatus(context, true)

            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams)
            }
            _purchaseStatus.value = PurchaseStatus.Success
        }
        // (Si está pending, Play nos notificara luego; acá no marcamos nada)
    }

    fun checkAndRestorePurchases() = scope.launch {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val active = result.purchasesList.firstOrNull { p ->
                isOurProduct(p) && p.purchaseState == PurchaseState.PURCHASED
            }
            if (active != null) {
                processPurchase(active)
            } else {
                // No hay compras activas -> marcá NO PRO si querés forzar el estado al restaurar:
                ProRepository.setProStatus(context, false)
            }
        }
    }

    fun destroy() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}

sealed class PurchaseStatus {
    object Idle : PurchaseStatus()
    object InProgress : PurchaseStatus()
    object Success : PurchaseStatus()
    data class Failed(val message: String) : PurchaseStatus()
}
