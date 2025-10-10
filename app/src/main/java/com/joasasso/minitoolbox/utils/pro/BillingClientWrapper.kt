package com.joasasso.minitoolbox.utils.pro

// --- INICIO MODIFICACIÓN ---
// Importamos la clase necesaria que ahora sí está disponible en la v8.0.0
// --- FIN MODIFICACIÓN ---
import android.app.Activity
import android.content.Context
import android.util.Log
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
    private val scope: CoroutineScope
) {
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _purchaseStatus = MutableStateFlow<PurchaseStatus>(PurchaseStatus.Idle)
    val purchaseStatus: StateFlow<PurchaseStatus> = _purchaseStatus.asStateFlow()

    companion object {
        private const val PRO_SKU = "minitoolbox_pro_lifetime"
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                scope.launch { processPurchase(purchase) }
            }
        } else {
            _purchaseStatus.value = PurchaseStatus.Failed("Error: ${billingResult.debugMessage}")
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
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
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

    private fun queryProductDetails() = scope.launch {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        val result = billingClient.queryProductDetails(params.build())

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _productDetails.value = result.productDetailsList?.firstOrNull()
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val product = _productDetails.value ?: return
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
        // Importante: Aquí se manejan las compras ya completadas.
        // Las compras pendientes llegarán a este listener, pero con un estado diferente.
        // Por ahora, solo procesamos las que ya están confirmadas.
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            ProRepository.setProStatus(context, true)

            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams)
            _purchaseStatus.value = PurchaseStatus.Success
        }
    }

    fun checkAndRestorePurchases() = scope.launch {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
        val purchasesResult = billingClient.queryPurchasesAsync(params.build())
        if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in purchasesResult.purchasesList) {
                if (purchase.products.contains(PRO_SKU) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    processPurchase(purchase)
                }
            }
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}

sealed class PurchaseStatus {
    object Idle : PurchaseStatus()
    object InProgress : PurchaseStatus()
    object Success : PurchaseStatus()
    data class Failed(val message: String) : PurchaseStatus()
}