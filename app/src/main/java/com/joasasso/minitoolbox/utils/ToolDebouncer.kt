package com.joasasso.minitoolbox.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona el cooldown de acciones por identificador en memoria.
 * Diseñado para evitar duplicados en ráfagas de navegación o clics repetidos.
 *
 * @param cooldownMs Tiempo mínimo entre ejecuciones exitosas en milisegundos.
 * @param clock Proveedor del tiempo actual (inyectable para tests).
 */
class ToolDebouncer(
    private val cooldownMs: Long,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val lastExecutions = ConcurrentHashMap<String, Long>()

    /**
     * Determina si una acción identificada por [id] puede ejecutarse.
     * Si retorna true, marca el momento actual como la última ejecución exitosa.
     *
     * @return true si pasó el tiempo suficiente desde la última ejecución exitosa.
     */
    fun canExecute(id: String): Boolean {
        val now = clock()
        val last = lastExecutions[id]

        return if (last == null || now - last >= cooldownMs) {
            lastExecutions[id] = now
            true
        } else {
            false
        }
    }

    /**
     * Limpia el estado de una herramienta específica o de todas.
     */
    fun reset(id: String? = null) {
        if (id == null) {
            lastExecutions.clear()
        } else {
            lastExecutions.remove(id)
        }
    }
}
