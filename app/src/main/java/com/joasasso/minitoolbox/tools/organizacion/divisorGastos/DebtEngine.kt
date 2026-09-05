package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.content.Context
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Reunion
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

data class DebtTransaction(
    val deudor: String,
    val acreedor: String,
    val montoCentavos: Long
) {
    val monto: Double get() = montoCentavos / 100.0
}

object DebtEngine {

    /**
     * Convierte una cadena de texto ingresada por el usuario en centavos enteros (Long).
     * Acepta coma o punto como separador decimal.
     * Ejemplos:
     * - "10" -> 1000L
     * - "10.5" -> 1050L
     * - "10,50" -> 1050L
     * - "10.05" -> 1005L
     */
    fun parseTextToCents(text: String): Long? {
        if (text.isBlank()) return null
        val trimmed = text.trim()
        if (!trimmed.matches(Regex("^[0-9]+([.,][0-9]*)?$|^[0-9]*[.,][0-9]+$"))) {
            return null
        }
        val cleaned = trimmed.replace(',', '.')
        val parts = cleaned.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val cents = if (parts.size > 1) {
            val dec = parts[1]
            when {
                dec.isEmpty() -> 0L
                dec.length == 1 -> (dec.toLongOrNull() ?: return null) * 10L
                dec.length == 2 -> dec.toLongOrNull() ?: return null
                else -> {
                    // Si tiene más de 2 decimales, redondea al centavo más cercano
                    val twoDigits = dec.substring(0, 2).toDoubleOrNull() ?: return null
                    val thirdDigit = dec[2].digitToIntOrNull() ?: 0
                    if (thirdDigit >= 5) twoDigits.toLong() + 1L else twoDigits.toLong()
                }
            }
        } else 0L

        return whole * 100L + cents
    }

    /**
     * Calcula la liquidación óptima de deudas entre los integrantes de la reunión
     * operando estrictamente en centavos enteros (Long).
     *
     * Invariante matemática garantizada:
     * sum(deudas) == sum(pagos) => sum(balances) == 0L
     * Cero centavos perdidos por punto flotante.
     */
    fun calcularLiquidacion(reunion: Reunion): List<DebtTransaction> {
        val nombresIntegrantes = reunion.integrantes.toSet()
        if (nombresIntegrantes.isEmpty()) return emptyList()

        val deudaPorIntegrante = reunion.integrantes.associateWith { 0L }.toMutableMap()

        for (gasto in reunion.gastos) {
            val consumidoPor = gasto.consumidoPor.filter { it.key in nombresIntegrantes && it.value > 0 }
            val aportes = gasto.obtenerAportesCentavos().filterKeys { it in nombresIntegrantes }
            val totalConsumidores = consumidoPor.size
            if (totalConsumidores == 0) continue

            val montoTotalGastoCentavos = aportes.values.sum()
            if (montoTotalGastoCentavos <= 0L) continue

            val basePorPersona = montoTotalGastoCentavos / totalConsumidores
            val residuoCentavos = (montoTotalGastoCentavos % totalConsumidores).toInt()

            // Repartir base y distribuir el residuo de centavos determinísticamente
            consumidoPor.keys.forEachIndexed { index, nombre ->
                val cuota = basePorPersona + if (index < residuoCentavos) 1L else 0L
                deudaPorIntegrante[nombre] = (deudaPorIntegrante[nombre] ?: 0L) + cuota
            }
        }

        val pagadoPorIntegrante = reunion.integrantes.associateWith { nombre ->
            reunion.gastos.sumOf { it.aporteEnCentavos(nombre) }
        }

        val balances = reunion.integrantes.associateWith { nombre ->
            val pagado = pagadoPorIntegrante[nombre] ?: 0L
            val debe = deudaPorIntegrante[nombre] ?: 0L
            pagado - debe
        }

        // Separar deudores (balance negativo) y acreedores (balance positivo)
        class BalanceItem(val nombre: String, var montoCentavos: Long)

        val deudores = balances
            .filter { it.value < 0L }
            .map { BalanceItem(it.key, -it.value) }
            .sortedByDescending { it.montoCentavos }
            .toMutableList()

        val acreedores = balances
            .filter { it.value > 0L }
            .map { BalanceItem(it.key, it.value) }
            .sortedByDescending { it.montoCentavos }
            .toMutableList()

        val transacciones = mutableListOf<DebtTransaction>()

        var deudorIdx = 0
        var acreedorIdx = 0

        while (deudorIdx < deudores.size && acreedorIdx < acreedores.size) {
            val deudor = deudores[deudorIdx]
            val acreedor = acreedores[acreedorIdx]

            val monto = minOf(deudor.montoCentavos, acreedor.montoCentavos)
            if (monto > 0L) {
                transacciones.add(DebtTransaction(deudor.nombre, acreedor.nombre, monto))
                deudor.montoCentavos -= monto
                acreedor.montoCentavos -= monto
            }

            if (deudor.montoCentavos == 0L) deudorIdx++
            if (acreedor.montoCentavos == 0L) acreedorIdx++
        }

        return transacciones
    }

    /**
     * Formatea una transacción de deuda en la representación textual localizada.
     */
    fun formatearTransaccion(
        transaccion: DebtTransaction,
        context: Context,
        formatoMoneda: NumberFormat? = null
    ): String {
        val formatter = formatoMoneda ?: NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return context.getString(
            R.string.debt_line,
            transaccion.deudor,
            formatter.format(transaccion.monto),
            transaccion.acreedor
        )
    }

    /**
     * Función puente para la UI: calcula la liquidación y devuelve la lista de líneas formateadas.
     */
    fun calcularDeudas(reunion: Reunion, context: Context): List<String> {
        val transacciones = calcularLiquidacion(reunion)
        val formatoMoneda = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return transacciones.map { formatearTransaccion(it, context, formatoMoneda) }
    }
}
