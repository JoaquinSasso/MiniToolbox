package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formato de cantidades de agua para la UI. Solo devuelve el número: la unidad ("L", "ml")
 * la agregan los strings localizados.
 */
object AguaFormat {

    /** Litros con hasta dos decimales y sin ceros sobrantes: 2000 -> "2", 1250 -> "1,25" en español. */
    fun litros(ml: Int, locale: Locale = Locale.getDefault()): String {
        // BigDecimal y HALF_UP: con double, 285 ml (0.285) quedaría en 0.28 por la representación binaria
        val litros = BigDecimal.valueOf(ml.toLong(), 3).setScale(2, RoundingMode.HALF_UP)
        return DecimalFormat("0.##", DecimalFormatSymbols.getInstance(locale)).format(litros)
    }

    /** true si la cantidad conviene mostrarla en litros (desde 1 L) y no en mililitros. */
    fun usaLitros(ml: Int): Boolean = ml >= 1000
}
