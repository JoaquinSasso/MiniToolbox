package com.joasasso.minitoolbox.pro

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joasasso.minitoolbox.utils.pro.ProSource
import com.joasasso.minitoolbox.utils.pro.ProState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.proDataStore by preferencesDataStore(name = "pro_prefs")

object ProPrefs {
    private val KEY_IS_PRO = booleanPreferencesKey("is_pro")
    private val KEY_SOURCE = stringPreferencesKey("pro_source")
    private val KEY_SINCE  = longPreferencesKey("pro_since")

    suspend fun setPro(context: Context, isPro: Boolean, source: ProSource, since: Long) {
        context.proDataStore.edit { p ->
            p[KEY_IS_PRO] = isPro
            p[KEY_SOURCE] = source.name
            p[KEY_SINCE]  = since
        }
    }

    fun flow(context: Context) = context.proDataStore.data.map { p ->
        ProState(
            isPro = p[KEY_IS_PRO] ?: false,
            source = runCatching {
                ProSource.valueOf(
                    p[KEY_SOURCE] ?: ProSource.None.name
                )
            }.getOrDefault(ProSource.None),
            sinceMillis = p[KEY_SINCE]
        )
    }.distinctUntilChanged()
}
