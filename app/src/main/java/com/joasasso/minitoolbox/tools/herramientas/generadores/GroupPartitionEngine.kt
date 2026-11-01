package com.joasasso.minitoolbox.tools.herramientas.generadores

/**
 * Modo de división para el generador de grupos:
 * - [BY_TEAM_COUNT]: El usuario define cuántos equipos desea formar en total (ej. 2, 3, 4 o 5 equipos).
 *   Los participantes se reparten de forma equitativa (diferencia máxima de 1 integrante entre equipos).
 * - [BY_TEAM_SIZE]: El usuario define cuántas personas debe tener cada equipo completo (ej. de a 2, 3, 4 o 5).
 *   Si el total no es múltiplo exacto, el resto forma un equipo adicional con las personas sobrantes.
 */
enum class PartitionMode {
    BY_TEAM_COUNT,
    BY_TEAM_SIZE
}

data class TeamGroupSummary(
    val teamCount: Int,
    val memberCount: Int
)

sealed interface PartitionResult<out T> {
    data class InsufficientParticipants(val minRequired: Int) : PartitionResult<Nothing>
    data class Success<T>(
        /** Asignación de cada elemento a un índice de equipo (0..teamSizes.size - 1) */
        val assignments: Map<T, Int>,
        /** Cantidad de integrantes en cada equipo resultante */
        val teamSizes: List<Int>
    ) : PartitionResult<T>
}

object GroupPartitionEngine {

    /**
     * Devuelve el mínimo de participantes requeridos para computar el reparto.
     */
    fun getMinRequired(mode: PartitionMode, targetValue: Int): Int {
        if (targetValue <= 0) return 2
        return when (mode) {
            PartitionMode.BY_TEAM_COUNT -> targetValue
            PartitionMode.BY_TEAM_SIZE -> targetValue
        }
    }

    /**
     * Calcula la lista de tamaños de equipos para [totalCount] participantes según [mode] y [targetValue].
     * Retorna lista vacía si [totalCount] es inferior al mínimo requerido.
     */
    fun calculateTeamSizes(totalCount: Int, mode: PartitionMode, targetValue: Int): List<Int> {
        val minRequired = getMinRequired(mode, targetValue)
        if (totalCount < minRequired || targetValue <= 0) {
            return emptyList()
        }

        return when (mode) {
            PartitionMode.BY_TEAM_COUNT -> {
                val base = totalCount / targetValue
                val remainder = totalCount % targetValue
                List(targetValue) { index ->
                    if (index < remainder) base + 1 else base
                }
            }
            PartitionMode.BY_TEAM_SIZE -> {
                val fullTeams = totalCount / targetValue
                val remainder = totalCount % targetValue
                if (remainder == 0) {
                    List(fullTeams) { targetValue }
                } else {
                    List(fullTeams) { targetValue } + listOf(remainder)
                }
            }
        }
    }

    /**
     * Agrupa los tamaños de equipos para generar resúmenes legibles.
     * Ejemplo: [3, 3, 2] -> [TeamGroupSummary(2, 3), TeamGroupSummary(1, 2)]
     */
    fun summarizeTeamSizes(teamSizes: List<Int>): List<TeamGroupSummary> {
        if (teamSizes.isEmpty()) return emptyList()
        val countsBySize = linkedMapOf<Int, Int>()
        for (size in teamSizes) {
            countsBySize[size] = (countsBySize[size] ?: 0) + 1
        }
        return countsBySize.map { (size, count) ->
            TeamGroupSummary(teamCount = count, memberCount = size)
        }
    }

    /**
     * Asigna los elementos a los equipos calculados.
     */
    fun <T> partition(
        items: List<T>,
        mode: PartitionMode,
        targetValue: Int,
        shuffler: (List<T>) -> List<T> = { it.shuffled() }
    ): PartitionResult<T> {
        val minRequired = getMinRequired(mode, targetValue)
        if (items.size < minRequired) {
            return PartitionResult.InsufficientParticipants(minRequired)
        }

        val teamSizes = calculateTeamSizes(items.size, mode, targetValue)
        if (teamSizes.isEmpty()) {
            return PartitionResult.InsufficientParticipants(minRequired)
        }

        val shuffled = shuffler(items)
        val assignments = mutableMapOf<T, Int>()
        var currentIndex = 0

        teamSizes.forEachIndexed { teamIdx, size ->
            for (i in 0 until size) {
                assignments[shuffled[currentIndex]] = teamIdx
                currentIndex++
            }
        }

        return PartitionResult.Success(
            assignments = assignments,
            teamSizes = teamSizes
        )
    }
}
