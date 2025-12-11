// ProSilentInitializer.kt
package com.joasasso.minitoolbox.utils.pro

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Inicialización / restauración silenciosa del estado PRO.
 *
 * - Se crea un BillingClientWrapper interno.
 * - Se llama a initialize(), que a su vez hace:
 *      - queryProductDetails()
 *      - checkAndRestorePurchases()
 * - ProRepository.setProStatus(...) se actualiza según las compras reales.
 *
 * Se usa un objeto singleton para que solo se inicialice una vez por proceso.
 */
object ProSilentInitializer {

    // Scope en background solo para billing
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Para evitar inicializar varias veces
    @Volatile
    private var initialized = false

    // Referencia al wrapper interno
    private var billingWrapper: BillingClientWrapper? = null

    /**
     * Llamar una vez al inicio de la app (por ejemplo en MainActivity.onCreate).
     *
     * @param context Cualquier contexto; se convierte internamente a applicationContext.
     * @param inappProductId El productId de tu compra PRO (ej: getString(R.string.billing_pro_id)).
     */
    fun init(context: Context, inappProductId: String) {
        if (initialized) return
        initialized = true

        val appContext = context.applicationContext

        billingWrapper = BillingClientWrapper(
            context = appContext,
            scope = scope,
            inappId = inappProductId
        ).also { wrapper ->
            // Esto conecta el BillingClient y llama a:
            //  - queryProductDetails()
            //  - checkAndRestorePurchases()
            wrapper.initialize()
        }
    }

    /**
     * Llamar si querés cerrar explícitamente el BillingClient al destruir la Activity principal.
     * No es estrictamente obligatorio, pero es prolijo.
     */
    fun destroy() {
        billingWrapper?.destroy()
        billingWrapper = null
        initialized = false
    }
}
