package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

/**
 * Funciones puras para cálculos de vasos, conversión, estadísticas y cumplimiento del objetivo diario de agua.
 */
object AguaMath {

    const val MIN_ML_POR_VASO = 50
    const val MAX_ML_POR_VASO = 1500
    const val MIN_OBJETIVO_ML = 1000
    const val MAX_OBJETIVO_ML = 5000
    const val PASO_OBJETIVO_ML = 250
    const val MAX_CANTIDADES_RAPIDAS = 3

    /** Intervalos que ofrece la UI para el recordatorio, en minutos. */
    val FRECUENCIAS_MINUTOS = listOf(30, 60, 90, 120, 180)

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Intervalo de [FRECUENCIAS_MINUTOS] más cercano a [minutos]. Las versiones anteriores
     * permitían guardar 150, 210 o 240, que ya no se ofrecen: la UI marca el más próximo.
     * En un empate gana el menor, que es el que avisa más seguido.
     */
    fun frecuenciaMasCercana(minutos: Int): Int =
        FRECUENCIAS_MINUTOS.minBy { kotlin.math.abs(it - minutos) }

    /**
     * true si [minutoDelDia] cae en el horario de recordatorios, con el fin excluido.
     * Si [inicioMin] es mayor que [finMin] el horario cruza la medianoche (por ejemplo 22:00 a 6:00,
     * para turnos de noche). Inicio igual a fin significa todo el día.
     */
    fun dentroDeHorario(minutoDelDia: Int, inicioMin: Int, finMin: Int): Boolean = when {
        inicioMin == finMin -> true
        inicioMin < finMin -> minutoDelDia in inicioMin until finMin
        else -> minutoDelDia >= inicioMin || minutoDelDia < finMin
    }

    /**
     * Decide si el recordatorio periódico debe mostrarse. La alarma sigue sonando a su ritmo;
     * esto solo evita la notificación fuera de horario o cuando ya no aporta nada.
     */
    fun debeRecordar(
        minutoDelDia: Int,
        inicioMin: Int,
        finMin: Int,
        pausarAlCumplir: Boolean,
        consumidoML: Int,
        objetivoML: Int,
        mlPorVaso: Int
    ): Boolean {
        if (!dentroDeHorario(minutoDelDia, inicioMin, finMin)) return false
        if (pausarAlCumplir && objetivoAlcanzado(consumidoML, objetivoML, mlPorVaso)) return false
        return true
    }

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
    /**
     * Convierte un mapa de claves/valores crudos (ej. provenientes de Preferences) en el historial
     * ordenado ascendentemente por fecha, ignorando claves ajenas o malformadas.
     */
    fun extraerHistoricoOrdenado(entries: Map<String, Any?>): List<Pair<LocalDate, Int>> {
        return entries
            .mapNotNull { (keyName, value) ->
                if (!keyName.startsWith("agua_ml_")) return@mapNotNull null
                val fechaStr = keyName.removePrefix("agua_ml_")
                try {
                    val fecha = LocalDate.parse(fechaStr, DATE_FORMATTER)
                    (value as? Int)?.let { fecha to it }
                } catch (_: DateTimeParseException) {
                    null
                }
            }
            .sortedBy { it.first }
    }

    /**
     * Últimos [n] días de calendario hasta [hoy] inclusive, del más viejo al más nuevo.
     * Los días sin registro valen 0: antes se tomaban los últimos 7 registros y los días
     * sin agua desaparecían del gráfico.
     */
    fun ultimosDias(historico: List<Pair<LocalDate, Int>>, hoy: LocalDate, n: Int = 7): List<Pair<LocalDate, Int>> {
        val porFecha = historico.toMap()
        return (n - 1 downTo 0).map { atras ->
            val fecha = hoy.minusDays(atras.toLong())
            fecha to (porFecha[fecha] ?: 0)
        }
    }

    /**
     * Promedio diario de los 7 días completos anteriores a [hoy]; hoy queda afuera porque
     * está en curso. Los días sin registro cuentan como 0, salvo los anteriores al primer
     * registro: quien empezó a usar la herramienta hace 3 días no arrastra 4 días en 0.
     * Devuelve null si todavía no hay ningún día completo.
     */
    fun promedioUltimaSemana(historico: List<Pair<LocalDate, Int>>, hoy: LocalDate): Int? {
        val primero = historico.minOfOrNull { it.first } ?: return null
        val porFecha = historico.toMap()
        val dias = (1..7).map { hoy.minusDays(it.toLong()) }.filter { !it.isBefore(primero) }
        if (dias.isEmpty()) return null
        return dias.map { porFecha[it] ?: 0 }.average().roundToInt()
    }

    /**
     * Días seguidos con la meta cumplida que terminan hoy o, si hoy todavía no se cumplió,
     * ayer: el día en curso no corta la racha. Usa la meta actual para todos los días, porque
     * el historial no guarda la meta que había en cada fecha.
     */
    fun rachaActual(historico: List<Pair<LocalDate, Int>>, hoy: LocalDate, objetivoML: Int, mlPorVaso: Int): Int {
        val porFecha = historico.toMap()
        // Sin registro no hay racha: además corta el recorrido aunque la meta fuera 0
        fun cumplio(fecha: LocalDate): Boolean =
            porFecha[fecha]?.let { objetivoAlcanzado(it, objetivoML, mlPorVaso) } ?: false

        var dia = if (cumplio(hoy)) hoy else hoy.minusDays(1)
        var racha = 0
        while (cumplio(dia)) {
            racha++
            dia = dia.minusDays(1)
        }
        return racha
    }

    /** Racha más larga de días de calendario seguidos con la meta cumplida. */
    fun mejorRacha(historico: List<Pair<LocalDate, Int>>, objetivoML: Int, mlPorVaso: Int): Int {
        var mejor = 0
        var actual = 0
        var anterior: LocalDate? = null
        for ((fecha, ml) in historico.sortedBy { it.first }) {
            if (objetivoAlcanzado(ml, objetivoML, mlPorVaso)) {
                actual = if (anterior != null && fecha == anterior.plusDays(1)) actual + 1 else 1
                anterior = fecha
                mejor = maxOf(mejor, actual)
            } else {
                actual = 0
                anterior = null
            }
        }
        return mejor
    }
}
