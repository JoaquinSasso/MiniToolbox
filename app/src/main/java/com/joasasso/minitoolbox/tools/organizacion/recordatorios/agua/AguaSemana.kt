package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Inicial del día de la semana en el idioma del sistema: "L", "M", "X"... o "M", "T", "W"... */
private fun inicialDia(fecha: LocalDate, locale: Locale): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)

/** Descripción para TalkBack: "jueves 12: 1,25 L, Meta cumplida". */
@Composable
private fun descripcionDia(fecha: LocalDate, ml: Int, cumplido: Boolean): String {
    val locale = LocalConfiguration.current.locales[0]
    val base = stringResource(
        R.string.water_stats_bar_desc,
        "${fecha.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${fecha.dayOfMonth}",
        AguaFormat.litros(ml)
    )
    return if (cumplido) "$base, ${stringResource(R.string.water_goal_reached)}" else base
}

/**
 * Barras de los últimos días con la meta como línea punteada. Cumplido o no se distingue
 * por color y además por un check, para no depender solo del color.
 */
@Composable
fun GraficoSemana(
    semana: List<Pair<LocalDate, Int>>,
    hoy: LocalDate,
    objetivoML: Int,
    mlPorVaso: Int,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val maximo = maxOf(objetivoML, semana.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1) * 1.1f
    val colorCumplido = MaterialTheme.colorScheme.primary
    // outlineVariant y no outline: en temas con primary muy claro, outline se confunde con él
    val colorPendiente = MaterialTheme.colorScheme.outlineVariant
    val colorMeta = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                semana.forEach { (fecha, ml) ->
                    key("barra-$fecha") {
                        val cumplido = AguaMath.objetivoAlcanzado(ml, objetivoML, mlPorVaso)
                        val descripcion = descripcionDia(fecha, ml, cumplido)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clearAndSetSemantics { contentDescription = descripcion },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (ml > 0) {
                                Box(
                                    Modifier
                                        .width(24.dp)
                                        .fillMaxHeight((ml / maximo).coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(if (cumplido) colorCumplido else colorPendiente)
                                )
                            }
                        }
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - objetivoML / maximo)
                drawLine(
                    color = colorMeta,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            semana.forEach { (fecha, ml) ->
                key("etiqueta-$fecha") {
                    val esHoy = fecha == hoy
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (AguaMath.objetivoAlcanzado(ml, objetivoML, mlPorVaso)) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = colorCumplido,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Spacer(Modifier.size(16.dp))
                        }
                        Text(
                            if (esHoy) stringResource(R.string.water_stats_today) else inicialDia(fecha, locale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (esHoy) FontWeight.Bold else null,
                            color = if (esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            fecha.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resumen compacto para la pantalla Hoy: un anillo por día y acceso a las estadísticas.
 * Reemplaza al botón "Estadísticas" que estaba al pie de la pantalla.
 */
@Composable
fun ResumenSemana(
    semana: List<Pair<LocalDate, Int>>,
    hoy: LocalDate,
    objetivoML: Int,
    mlPorVaso: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.water_statistics_ultimos7),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.stats),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(Modifier.fillMaxWidth()) {
                semana.forEach { (fecha, ml) ->
                    key("resumen-$fecha") {
                        val cumplido = AguaMath.objetivoAlcanzado(ml, objetivoML, mlPorVaso)
                        val esHoy = fecha == hoy
                        val descripcion = descripcionDia(fecha, ml, cumplido)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clearAndSetSemantics { contentDescription = descripcion },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = {
                                        if (objetivoML > 0) (ml / objetivoML.toFloat()).coerceIn(0f, 1f) else 1f
                                    },
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 4.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    gapSize = 0.dp
                                )
                                if (cumplido) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                inicialDia(fecha, locale),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (esHoy) FontWeight.Bold else null,
                                color = if (esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
