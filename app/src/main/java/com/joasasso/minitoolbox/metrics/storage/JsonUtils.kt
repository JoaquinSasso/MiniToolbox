package com.joasasso.minitoolbox.metrics.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object JsonUtils {
    val gson = Gson()

    inline fun <reified K, reified V> fromJsonMap(json: String?): MutableMap<K, V> {
        if (json.isNullOrBlank()) return mutableMapOf()
        val type = object : TypeToken<MutableMap<K, V>>() {}.type
        return gson.fromJson(json, type) ?: mutableMapOf()
    }

    fun <K, V> toJsonMap(map: Map<K, V>): String {
        return gson.toJson(map)
    }
}
