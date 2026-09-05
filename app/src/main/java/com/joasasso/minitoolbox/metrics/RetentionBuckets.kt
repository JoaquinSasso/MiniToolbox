package com.joasasso.minitoolbox.metrics

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Retención sin identificadores.
 *
 * El dispositivo es el único que conoce su propio historial, así que el cálculo se hace
 * localmente y lo que viaja al servidor es un `1` en una de las categorías. Dos
 * dispositivos con la misma clave son indistinguibles y no hay nada que reidentificar:
 * el servidor sólo ve una distribución.
 *
 * Es la misma lógica que ya usa `daily_active`, donde el estado local decide y lo que
 * se envía es un contador sin memoria.
 *
 * ## Qué mide y qué no
 *
 * Esto NO es retención por cohortes ("de los que instalaron en la semana X, cuántos
 * siguen a los 7 días"): eso exigiría seguir individuos en el tiempo. Lo que da es la
 * distribución de intensidad de uso cruzada con antigüedad, que responde si la base son
 * visitantes ocasionales o gente que vuelve, y si los que vuelven son nuevos o veteranos.
 *
 * Limitaciones conocidas:
 * - Reinstalar o borrar datos reinicia el historial: un usuario de dos años vuelve a
 *   aparecer como nuevo. No hay forma de corregirlo sin identificadores persistentes.
 * - La ventana tarda 28 días en madurar. Durante el primer mes de una instalación la
 *   intensidad está subestimada por construcción.
 */
object RetentionBuckets {

    /** Días de historial que se conservan y sobre los que se calcula la intensidad. */
    const val WINDOW_DAYS = 28

    /**
     * Intensidad: días activos dentro de la ventana.
     *
     * Los cortes están cargados hacia abajo a propósito: en una app de herramientas
     * sueltas, la mayor parte de la masa está en uso ocasional, y agrupar todo eso en
     * un solo bucket escondería la única diferencia que importa.
     *
     * Una vez publicados NO se cambian: partirían la serie histórica en dos.
     */
    fun intensityBucket(activeDays: Int): String = when {
        activeDays <= 1 -> "d1"
        activeDays <= 3 -> "d2_3"
        activeDays <= 7 -> "d4_7"
        activeDays <= 14 -> "d8_14"
        else -> "d15_28"
    }

    /** Antigüedad: días transcurridos desde el primer día registrado en el dispositivo. */
    fun ageBucket(ageDays: Int): String = when {
        ageDays <= 6 -> "age0_6"
        ageDays <= 29 -> "age7_29"
        ageDays <= 89 -> "age30_89"
        ageDays <= 179 -> "age90_179"
        else -> "age180p"
    }

    /**
     * Clave compuesta "<antigüedad>.<intensidad>", por ejemplo "age30_89.d4_7".
     *
     * Se reporta sólo el cruce: sumando por un eje se obtienen las marginales, así que
     * mandar además los mapas sueltos sería redundante. La cardinalidad son 5x5 = 25
     * claves, acotada y muy por debajo del tope del backend.
     */
    fun key(ageDays: Int, activeDays: Int): String =
        "${ageBucket(ageDays)}.${intensityBucket(activeDays)}"

    // -----------------------------------------------------------------
    // Utilidades de fechas. Funciones puras para poder testearlas sin Android.
    // -----------------------------------------------------------------

    private const val DAY_FORMAT = "yyyy-MM-dd"

    private fun formatter() = SimpleDateFormat(DAY_FORMAT, Locale.US)

    /** Día más antiguo que sigue dentro de la ventana, inclusive. */
    fun windowStart(today: String): String {
        val fmt = formatter()
        val date = try {
            fmt.parse(today) ?: return today
        } catch (_: ParseException) {
            return today
        }
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, -(WINDOW_DAYS - 1))
        return fmt.format(cal.time)
    }

    /**
     * Días entre dos fechas "yyyy-MM-dd". Devuelve 0 si alguna no se puede interpretar,
     * que es el caso conservador: un dispositivo sin historial legible cuenta como nuevo.
     */
    fun daysBetween(from: String, to: String): Int {
        val fmt = formatter()
        return try {
            val a = fmt.parse(from) ?: return 0
            val b = fmt.parse(to) ?: return 0
            val diff = b.time - a.time
            max(0, (diff / MILLIS_PER_DAY).toInt())
        } catch (_: ParseException) {
            0
        }
    }

    /**
     * Recorta la lista de días activos a la ventana y agrega el día de hoy.
     * Devuelve la lista ordenada y sin repetidos.
     */
    fun updateActiveDays(previous: Collection<String>, today: String): List<String> {
        val start = windowStart(today)
        return (previous + today)
            .filter { it >= start && it <= today }
            .distinct()
            .sorted()
    }

    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    /** Fecha de hoy en el huso local, en el mismo formato que el resto de las métricas. */
    fun today(): String = formatter().format(Date())
}