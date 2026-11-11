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

    /**
     * Extra que identifica qué widget abrió la app. Hoy solo lo pone el widget de
     * favoritos: los de agua también abren con [WIDGET], pero no deben contar como él.
     */
    const val EXTRA_START_WIDGET = "startWidget"

    /** Clave de `widgets` del widget de favoritos. Es contrato: no se renombra. */
    const val FAVORITES_WIDGET_KEY = "widget_shortcuts"

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

    /**
     * Clave de `widgets` a contar cuando la app se abre por deep link, o null si no
     * corresponde contar ninguna.
     *
     * Solo cuenta el widget de favoritos. Las notificaciones, los widgets de agua y los
     * deep links sin origen ya quedan registrados en `tool_entry` con su origen, y
     * contarlos acá inflaba `widget_shortcuts` con aperturas que no venían del widget.
     */
    fun deepLinkWidgetKey(source: String?, widget: String?): String? =
        if (source == WIDGET && widget == FAVORITES_WIDGET_KEY) FAVORITES_WIDGET_KEY else null
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