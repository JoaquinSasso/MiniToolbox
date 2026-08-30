package com.joasasso.minitoolbox.metrics

/**
 * Fuente única de verdad del formato de claves y días de métricas en el cliente.
 *
 * DEBE mantenerse en sincronía con `backend/functions/src/validate.ts`
 * (KEY_RE, DAY_RE, collapseRouteKey, normalizeKey). Los tests de ambos lados
 * comparten las fixtures de `metrics-fixtures/keys.json` para garantizarlo.
 *
 * Motivo de existir: el backend rechazaba lotes enteros cuando llegaba una clave
 * con caracteres inválidos, y como el cliente congela el payload para garantizar
 * idempotencia, ese rechazo dejaba las métricas del dispositivo detenidas de forma
 * permanente. Validar del lado del cliente, con el mismo contrato, evita que ese
 * dato llegue siquiera a generarse.
 */
object MetricsContract {

    /** Largo máximo de una clave, impuesto por el backend. */
    const val MAX_KEY_LEN = 64

    /** Claves aceptadas por el backend: toolId, adType, versión, idioma, widgetKind. */
    val KEY_RE = Regex("^[a-zA-Z0-9._-]{1,$MAX_KEY_LEN}$")

    /** Días en formato "YYYY-MM-DD". */
    val DAY_RE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    /**
     * Segmentos que son identificadores y no aportan información agregable:
     * UUIDs, hashes hexadecimales y números.
     */
    private val ID_SEGMENT_RE = Regex(
        "^(?:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|[0-9a-f]{8,}|\\d+)$",
        RegexOption.IGNORE_CASE
    )

    private val ROUTE_SEPARATOR_RE = Regex("[/?#]")
    private val INVALID_CHARS_RE = Regex("[^a-zA-Z0-9._-]")
    private val REPEATED_UNDERSCORE_RE = Regex("_+")

    fun isValidKey(key: String): Boolean = KEY_RE.matches(key)

    fun isValidDay(day: String): Boolean = DAY_RE.matches(day)

    /**
     * Convierte una clave con forma de ruta en una clave estable, descartando los
     * segmentos que son identificadores.
     *
     *   "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b" -> "pomodoro_detail"
     *   "pomodoro/detail/{timerId}"                            -> "pomodoro_detail_timerId"
     *   "dev/metrics"                                          -> "dev_metrics"
     *
     * Sin este colapso cada timer generaría su propia clave y explotaría la
     * cardinalidad de los mapas agregados del backend.
     */
    fun collapseRouteKey(raw: String): String =
        raw.split(ROUTE_SEPARATOR_RE)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { ID_SEGMENT_RE.matches(it) }
            .map { it.removePrefix("{").removeSuffix("}") }
            .joinToString("_")

    /**
     * Convierte una clave arbitraria en una que cumpla [KEY_RE].
     * Devuelve null si no queda nada utilizable.
     */
    fun normalizeKey(raw: String?): String? {
        if (raw == null) return null
        val collapsed = if (ROUTE_SEPARATOR_RE.containsMatchIn(raw)) collapseRouteKey(raw) else raw
        val clean = collapsed
            .trim()
            .replace(INVALID_CHARS_RE, "_")
            .replace(REPEATED_UNDERSCORE_RE, "_")
            .trim('_')
            .take(MAX_KEY_LEN)
        return clean.ifEmpty { null }
    }

    /**
     * Resultado de sanear un mapa de contadores.
     *
     * @property clean mapa con todas las claves cumpliendo [KEY_RE]
     * @property fixedKeys claves que hubo que normalizar
     * @property droppedKeys claves descartadas por completo
     */
    data class MapReport(
        val clean: MutableMap<String, Int>,
        val fixedKeys: Int,
        val droppedKeys: Int
    ) {
        val changed: Boolean get() = fixedKeys > 0 || droppedKeys > 0
    }

    /**
     * Sanea un mapa clave -> contador.
     *
     * - Las claves inválidas se normalizan. Si dos claves distintas colapsan a la
     *   misma, los contadores se SUMAN en lugar de pisarse.
     * - Los contadores negativos se descartan.
     *
     * Importante: aplicar siempre la misma transformación a los mapas acumulados y
     * a sus correspondientes mapas de "enviados", o el delta queda descuadrado.
     */
    fun sanitizeCounters(src: Map<String, Int>): MapReport {
        val out = LinkedHashMap<String, Int>()
        var fixed = 0
        var dropped = 0

        for ((rawKey, value) in src) {
            if (value < 0) {
                dropped++
                continue
            }
            val key = if (isValidKey(rawKey)) {
                rawKey
            } else {
                val normalized = normalizeKey(rawKey)
                if (normalized == null) {
                    dropped++
                    continue
                }
                fixed++
                normalized
            }
            out[key] = (out[key] ?: 0) + value
        }

        return MapReport(out, fixed, dropped)
    }

    /**
     * Sanea un mapa día -> (clave -> contador), descartando los días mal formados.
     */
    fun sanitizeDayNested(
        src: Map<String, Map<String, Int>>
    ): Pair<MutableMap<String, MutableMap<String, Int>>, MapReport> {
        val out = LinkedHashMap<String, MutableMap<String, Int>>()
        var fixed = 0
        var dropped = 0

        for ((day, counters) in src) {
            if (!isValidDay(day)) {
                dropped += counters.size
                continue
            }
            val report = sanitizeCounters(counters)
            fixed += report.fixedKeys
            dropped += report.droppedKeys
            if (report.clean.isNotEmpty()) out[day] = report.clean
        }

        return out to MapReport(mutableMapOf(), fixed, dropped)
    }

    /**
     * Sanea un mapa día -> contador, descartando días mal formados y valores negativos.
     */
    fun sanitizeDayFlat(src: Map<String, Int>): Pair<MutableMap<String, Int>, MapReport> {
        val out = LinkedHashMap<String, Int>()
        var dropped = 0

        for ((day, value) in src) {
            if (!isValidDay(day) || value < 0) {
                dropped++
                continue
            }
            out[day] = value
        }

        return out to MapReport(mutableMapOf(), 0, dropped)
    }
}