package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import kotlin.math.roundToInt

/**
 * Funciones puras para cálculos de vasos, conversión y cumplimiento del objetivo diario de agua.
 */
object AguaMath {

    const val MIN_ML_POR_VASO = 50
    const val MAX_ML_POR_VASO = 1500

    /**
     * Calcula los mililitros por vaso necesarios para cubrir [objetivoML] en [vasos] tomas,
     * redondeando al mililitro entero más cercano dentro de los límites permitidos.
     */
    fun calcularMlPorVasoSegunVasos(objetivoML: Int, vasos: Int): Int {
        val safeVasos = vasos.coerceAtLeast(1)
        return (objetivoML.coerceAtLeast(0).toFloat() / safeVasos)
            .roundToInt()
            .coerceIn(MIN_ML_POR_VASO, MAX_ML_POR_VASO)
    }

    /**
     * Calcula la cantidad de vasos objetivo redondeando al entero más cercano,
     * evitando que divisiones como 2000 / 286 (6.993) se trunquen a 6 vasos.
     */
    fun calcularVasosObjetivo(objetivoML: Int, mlPorVaso: Int): Int {
        val safeVaso = mlPorVaso.coerceAtLeast(1)
        return (objetivoML.coerceAtLeast(0).toFloat() / safeVaso)
            .roundToInt()
            .coerceAtLeast(1)
    }

    /**
     * Calcula la cantidad de vasos consumidos redondeando al entero más cercano.
     */
    fun calcularVasosConsumidos(consumidoML: Int, mlPorVaso: Int): Int {
        val safeVaso = mlPorVaso.coerceAtLeast(1)
        return (consumidoML.coerceAtLeast(0).toFloat() / safeVaso)
            .roundToInt()
    }

    /**
     * Determina si el consumo del día alcanzó el objetivo diario.
     *
     * Contempla tres casos consistentes con lo que muestra la interfaz:
     * 1. El consumo en mililitros alcanza o supera [objetivoML].
     * 2. El valor expresado en litros con dos decimales (centilitros redondeados, ej. 1995 ml -> 2.00 L)
     *    alcanza el objetivo expresado con la misma precisión.
     * 3. El tamaño de vaso [mlPorVaso] proviene de truncar `objetivoML / vasosObjetivo`
     *    (ej. 2000 / 7 = 285 ml) y el usuario completó todos los vasos objetivo (7 * 285 = 1995 ml).
     */
    fun objetivoAlcanzado(consumidoML: Int, objetivoML: Int, mlPorVaso: Int = 250): Boolean {
        if (objetivoML <= 0) return true
        if (consumidoML >= objetivoML) return true

        // Coherencia con el formato "%.2f L" (redondeo al centilitro: 10 ml)
        val centilitrosConsumidos = (consumidoML / 10f).roundToInt()
        val centilitrosObjetivo = (objetivoML / 10f).roundToInt()
        if (centilitrosConsumidos >= centilitrosObjetivo) return true

        // Coherencia cuando mlPorVaso es el cociente entero truncado de objetivoML / vasos
        val safeVaso = mlPorVaso.coerceAtLeast(1)
        val vasosObjetivo = calcularVasosObjetivo(objetivoML, safeVaso)
        if (safeVaso == objetivoML / vasosObjetivo && consumidoML >= vasosObjetivo * safeVaso) {
            return true
        }

        return false
    }
}
