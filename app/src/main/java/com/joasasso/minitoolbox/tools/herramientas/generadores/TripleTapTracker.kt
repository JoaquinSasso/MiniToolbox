package com.joasasso.minitoolbox.tools.herramientas.generadores

/**
 * Registra toques rápidos consecutivos para detectar un triple toque (3 toques dentro de una ventana de [maxIntervalMs]).
 *
 * [minSeparationMs] asegura que múltiples dedos apoyados simultáneamente en el mismo instante
 * no se contabilicen erróneamente como toques consecutivos de un solo dedo.
 */
class TripleTapTracker(
    private val maxIntervalMs: Long = 900L,
    private val minSeparationMs: Long = 60L,
    private val requiredTaps: Int = 3
) {
    private val tapTimestamps = mutableListOf<Long>()

    /**
     * Registra un toque ocurrido en [timestampMs].
     * Retorna `true` si con este toque se completa la secuencia de [requiredTaps].
     */
    fun registerTap(timestampMs: Long): Boolean {
        // Elimina toques fuera de la ventana de tiempo
        tapTimestamps.removeAll { timestampMs - it > maxIntervalMs }

        // Evita contar toques simultáneos de distintos dedos en el mismo milisegundo
        val lastTap = tapTimestamps.lastOrNull()
        if (lastTap != null && timestampMs - lastTap < minSeparationMs) {
            return false
        }

        tapTimestamps.add(timestampMs)

        if (tapTimestamps.size >= requiredTaps) {
            tapTimestamps.clear()
            return true
        }
        return false
    }

    fun reset() {
        tapTimestamps.clear()
    }
}
