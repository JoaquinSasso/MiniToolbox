package com.joasasso.minitoolbox.utils.pro

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.pro.ProPrefs
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BillingRepository(
        appContext = app.applicationContext,
        inappId = app.getString(R.string.billing_pro_inapp),
        subId = app.getString(R.string.billing_pro_sub) // o null si no usas subs
    )

    // Flujo de ProState desde DataStore
    private val stored = ProPrefs.flow(app.applicationContext)

    // Flujo de eventos en vivo desde Billing (compras/restore)
    private val live: SharedFlow<ProState> = repo.entitlementEvents

    // Estado combinado: si llega un evento live, persistimos y lo exponemos
    val proState: StateFlow<ProState> = merge(
        stored,
        live.onEach { s ->
            viewModelScope.launch {
                ProPrefs.setPro(
                    app.applicationContext,
                    isPro = s.isPro,
                )
            }
        }
    ).stateIn(viewModelScope, SharingStarted.Companion.Eagerly, ProState(isPro = false))

    init {
        // Restaurar al iniciar la app
        viewModelScope.launch { repo.restoreEntitlement() }
    }

    suspend fun queryProducts() = repo.queryProductDetails()
    fun buy(activity: Activity, details: ProductDetails) =
        repo.launchPurchase(activity, details)

    fun restore() { viewModelScope.launch { repo.restoreEntitlement() } }
}