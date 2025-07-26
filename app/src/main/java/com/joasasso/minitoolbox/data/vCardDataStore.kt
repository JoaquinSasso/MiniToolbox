package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("qr_contacto_prefs")

object QrContactoPrefs {
    val NOMBRE    = stringPreferencesKey("nombre")
    val TELEFONO  = stringPreferencesKey("telefono")
    val EMAIL     = stringPreferencesKey("email")
}

class QrContactoDataStore(private val context: Context) {
    val datos: Flow<QrContacto> = context.dataStore.data.map { prefs ->
        QrContacto(
            nombre    = prefs[QrContactoPrefs.NOMBRE]    ?: "",
            telefono  = prefs[QrContactoPrefs.TELEFONO]  ?: "",
            email     = prefs[QrContactoPrefs.EMAIL]     ?: "",
        )
    }

    suspend fun guardar(datos: QrContacto) {
        context.dataStore.edit { prefs ->
            prefs[QrContactoPrefs.NOMBRE]    = datos.nombre
            prefs[QrContactoPrefs.TELEFONO]  = datos.telefono
            prefs[QrContactoPrefs.EMAIL]     = datos.email
        }
    }

    suspend fun limpiar() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

data class QrContacto(
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
)
