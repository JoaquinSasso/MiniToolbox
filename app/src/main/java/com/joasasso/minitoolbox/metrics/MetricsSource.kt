package com.joasasso.minitoolbox.metrics

/**
 * Origen desde el que se abrió una herramienta.
 *
 * Responde una pregunta que hoy no se puede contestar: si las notificaciones y los widgets
 * sirven para algo, o si toda la actividad entra por navegación dentro de la app. Sin esto,
 * `tools.water = 500` no distingue entre alguien que abre la app y busca la herramienta y
 * alguien que toca un widget en la pantalla de inicio.
 *
 * Los valores son parte del contrato de datos: una vez publicados no se renombran, porque
 * partirían la serie histórica. Deben cumplir [MetricsContract.KEY_RE] y no contener puntos,
 * ya que el punto es el separador de la clave compuesta.
 */
object MetricsSource {

    /** Navegación dentro de la app (categorías, favoritos, búsqueda). */
    const val NAV = "nav"

    /** Notificación del sistema. */
    const val NOTIFICATION = "notification"

    /** Widget de la pantalla de inicio. */
    const val WIDGET = "widget"

    /** Acceso directo de la app (long press sobre el ícono). */
    const val SHORTCUT = "shortcut"

    /** Deep link sin origen declarado. */
    const val UNKNOWN = "unknown"

    /** Extra del Intent que transporta el origen hasta MainActivity. */
    const val EXTRA_START_SOURCE = "startSource"

    private val KNOWN = setOf(NAV, NOTIFICATION, WIDGET, SHORTCUT, UNKNOWN)

    /**
     * Normaliza un origen recibido desde un Intent.
     * Cualquier valor no reconocido cae en [UNKNOWN] para acotar la cardinalidad: la clave
     * compuesta es herramienta x origen, y un origen libre multiplicaría los campos del
     * documento diario sin control.
     */
    fun normalize(raw: String?): String =
        if (raw != null && raw in KNOWN) raw else UNKNOWN

    /**
     * Clave compuesta "<herramienta>.<origen>", por ejemplo "water.widget".
     *
     * El punto está permitido por [MetricsContract.KEY_RE], así que el par entra en un único
     * mapa plano sin necesidad de anidar otro nivel.
     */
    fun entryKey(toolKey: String, source: String): String = "$toolKey.${normalize(source)}"
}

/**
 * Origen pendiente de atribuir a la próxima apertura de herramienta.
 *
 * MainActivity recibe el Intent con el origen, pero quien registra el uso es el NavGraph,
 * que es el único punto por el que pasan todas las aperturas. Este objeto transporta el dato
 * entre los dos sin duplicar el registro.
 *
 * Es de un solo uso: [consume] lo devuelve y lo limpia, así que si el usuario sigue navegando
 * dentro de la app las aperturas siguientes se atribuyen a [MetricsSource.NAV].
 */
object PendingEntrySource {

    @Volatile
    private var pending: String? = null

    fun set(source: String?) {
        pending = source?.let { MetricsSource.normalize(it) }
    }

    fun consume(): String {
        val value = pending
        pending = null
        return value ?: MetricsSource.NAV
    }
}