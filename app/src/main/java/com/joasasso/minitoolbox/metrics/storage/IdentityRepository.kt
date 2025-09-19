package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.INSTANCE_HASH_HEX
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.INSTANCE_ID
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.INSTANCE_SALT_B64
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

data class Identity(val id: String, val hashHex: String)

class IdentityRepository(private val context: Context) {

    suspend fun ensureIdentity(): Identity {
        val ds = context.metricsDataStore
        val prefs = try { ds.data.first() } catch (_: Throwable) { emptyPreferences() }

        var id = prefs[INSTANCE_ID]
        var saltB64 = prefs[INSTANCE_SALT_B64]
        var hashHex = prefs[INSTANCE_HASH_HEX]

        if (id == null || saltB64 == null || hashHex == null) {
            id = id ?: UUID.randomUUID().toString()
            val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val newSaltB64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
            // Hash = SHA-256( bytes(id) + salt )
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(id.toByteArray(Charsets.UTF_8) + saltBytes)
            val newHashHex = digest.joinToString("") { "%02x".format(it) }

            ds.edit { e ->
                e[INSTANCE_ID] = id
                e[INSTANCE_SALT_B64] = newSaltB64
                e[INSTANCE_HASH_HEX] = newHashHex
            }
            saltB64 = newSaltB64
            hashHex = newHashHex
        }
        return Identity(id = id, hashHex = hashHex)
    }

    suspend fun getHash(): String {
        val prefs = context.metricsDataStore.data.first()
        return prefs[INSTANCE_HASH_HEX] ?: ensureIdentity().hashHex
    }
}
