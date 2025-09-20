// Utils/ConsentGeo.kt
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform

fun isEeaOrUk(context: Context): Boolean {
    val info = UserMessagingPlatform.getConsentInformation(context)
    // Si Google UMP indica que el consentimiento es requerido, asumimos EEA/UK
    return info.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
    // Alternativa adicional:
    // return info.privacyOptionsRequirementStatus ==
    //        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
}
