package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

/**
 * Sistema de unidades soportado por la Regla AR.
 */
enum class Units {
    METRIC,
    IMPERIAL;

    fun toggle(): Units = if (this == METRIC) IMPERIAL else METRIC
}

/**
 * Modo de medición: segmento simple (2 puntos) o polilínea continua.
 */
enum class MeasureMode {
    SEGMENT,
    POLYLINE
}

/**
 * Estados del tracking de ARCore y condiciones geométricas.
 */
enum class ArStatus {
    INIT,
    TOO_DARK,
    TOO_FAST,
    NO_FEATURES,
    CAMERA_OFF,
    NO_SURFACE,
    TOO_FAR,
    TOO_CLOSE,
    UNSTABLE,
    ILL_CONDITIONED,
    READY,
    SAMPLING,
    LENS_CHANGED
}

/**
 * Representación inmutable de una medición confirmada.
 * Los puntos locales se expresan en el sistema de coordenadas del ancla asociada.
 */
data class MeasurementRecord(
    val id: Int,
    val locals: List<FloatArray>,
    val segments: List<Float>,
    val total: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MeasurementRecord
        if (id != other.id) return false
        if (total != other.total) return false
        if (segments != other.segments) return false
        if (locals.size != other.locals.size) return false
        for (i in locals.indices) {
            if (!locals[i].contentEquals(other.locals[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + total.hashCode()
        result = 31 * result + segments.hashCode()
        for (arr in locals) {
            result = 31 * result + arr.contentHashCode()
        }
        return result
    }
}

/**
 * Estado observable de la pantalla de Regla AR expuesto por el ViewModel.
 */
data class ArRulerUiState(
    val unitSystem: Units = Units.METRIC,
    val mode: MeasureMode = MeasureMode.SEGMENT,
    val measurements: List<MeasurementRecord> = emptyList(),
    val draftLocals: List<FloatArray> = emptyList(),
    val canUndo: Boolean = false,
    val canFinish: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ArRulerUiState
        if (unitSystem != other.unitSystem) return false
        if (mode != other.mode) return false
        if (measurements != other.measurements) return false
        if (canUndo != other.canUndo) return false
        if (canFinish != other.canFinish) return false
        if (draftLocals.size != other.draftLocals.size) return false
        for (i in draftLocals.indices) {
            if (!draftLocals[i].contentEquals(other.draftLocals[i])) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = unitSystem.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + measurements.hashCode()
        result = 31 * result + canUndo.hashCode()
        result = 31 * result + canFinish.hashCode()
        for (arr in draftLocals) {
            result = 31 * result + arr.contentHashCode()
        }
        return result
    }
}
