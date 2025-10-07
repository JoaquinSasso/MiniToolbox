package com.joasasso.minitoolbox.metrics.storage

import org.json.JSONObject

object JsonUtils {

    // Map<String, Int>
    fun fromDayIntMap(json: String?): MutableMap<String, Int> {
        val out = mutableMapOf<String, Int>()
        if (json.isNullOrBlank()) return out
        val obj = JSONObject(json)
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            out[k] = obj.optInt(k, 0)
        }
        return out
    }
    fun toDayIntMap(map: Map<String, Int>): String = JSONObject(map as Map<*, *>).toString()

    // Map<String, Map<String, Int>>
    fun fromDayNestedIntMap(json: String?): MutableMap<String, MutableMap<String, Int>> {
        val out = mutableMapOf<String, MutableMap<String, Int>>()
        if (json.isNullOrBlank()) return out
        val obj = JSONObject(json)
        val it = obj.keys()
        while (it.hasNext()) {
            val day = it.next()
            val innerObj = obj.optJSONObject(day) ?: JSONObject()
            val inner = mutableMapOf<String, Int>()
            val it2 = innerObj.keys()
            while (it2.hasNext()) {
                val k = it2.next()
                inner[k] = innerObj.optInt(k, 0)
            }
            out[day] = inner
        }
        return out
    }
    fun toDayNestedIntMap(map: Map<String, Map<String, Int>>): String {
        val root = JSONObject()
        for ((day, inner) in map) root.put(day, JSONObject(inner as Map<*, *>))
        return root.toString()
    }
}
