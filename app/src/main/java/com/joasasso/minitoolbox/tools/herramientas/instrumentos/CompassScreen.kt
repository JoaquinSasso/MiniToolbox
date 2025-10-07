package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// --- Helpers de heading -------------------------------------------------------

@Composable
private fun rememberSensorHeadingDeg(): Float? {
    val ctx = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rot = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        if (rot != null) {
            // Camino 1: rotation-vector
            val R = FloatArray(9)
            val orient = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(R, e.values)
                    SensorManager.getOrientation(R, orient)
                    var deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                    deg = ((deg % 360f) + 360f) % 360f
                    heading = deg
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, rot, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        } else {
            // Camino 2: ACC + MAG
            val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (acc == null || mag == null) {
                heading = null
                onDispose { }
            } else {
                val g = FloatArray(3)
                val m = FloatArray(3)
                var gOk = false
                var mOk = false

                fun lowPass(input: FloatArray, output: FloatArray?, alpha: Float = 0.15f): FloatArray {
                    if (output == null) return input.copyOf()
                    for (i in 0..2) output[i] = output[i] + alpha * (input[i] - output[i])
                    return output
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                val orient = FloatArray(3)

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) {
                        when (e.sensor.type) {
                            Sensor.TYPE_ACCELEROMETER -> { lowPass(e.values.clone(), g); gOk = true }
                            Sensor.TYPE_MAGNETIC_FIELD -> { lowPass(e.values.clone(), m); mOk = true }
                        }
                        if (gOk && mOk && SensorManager.getRotationMatrix(R, I, g, m)) {
                            SensorManager.getOrientation(R, orient)
                            var deg = Math.toDegrees(orient[0].toDouble()).toFloat()
                            deg = ((deg % 360f) + 360f) % 360f
                            heading = deg
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
                sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
                onDispose { sm.unregisterListener(listener) }
            }
        }
    }
    return heading
}


// --- UI principal -------------------------------------------------------------

@Composable
fun BrujulaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val red = Color(0xFFF5274E)
    val onSurface = MaterialTheme.colorScheme.onSurface
    var showInfo by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // ¿Hay magnetómetro en el dispositivo?
    val hasMagnetometer = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    // Heading de sensores (rot-vector o acc+mag)
    val sensorHeading = rememberSensorHeadingDeg()

    // Animación suave (si no hay dato, quedate en el último)
    var last by remember { mutableFloatStateOf(0f) }
    val target = sensorHeading ?: last
    val animated by animateFloatAsState(
        targetValue = remember(target) {
            val diff = ((target - last + 540f) % 360f) - 180f
            val next = last + diff
            last = next
            next
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "compassRotation"
    )

    TickHaptics(
        headingDeg = animated,
        majorStepDeg = 30f,
        epsilonDeg = 3f,
        cooldownMs = 350L
    )

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_compass), onBack, onShowInfo = {showInfo = true}) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = sensorHeading?.let { headingLabel(animated, context) } ?: "—°",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
                if (!hasMagnetometer) {
                    Text(
                        text = "Este dispositivo no tiene magnetómetro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (hasMagnetometer) {
                BoxWithConstraints(Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    val side = min(maxWidth, maxHeight) * 0.9f
                    CompassDial(
                        diameterDp = side,
                        ringRotationDeg = -animated,   // el anillo gira opuesto al heading
                        northColor = red,          // color para el norte y el triangulo
                        tickColor = onSurface,
                        ringColor = onSurface.copy(alpha = 0.25f)
                    )
                }
            } else {
                // Fallback simple cuando no hay magnetómetro
                Text(
                    text = "Brújula no soportada en este dispositivo",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.compass_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.compass_help_line1))
                    Text(stringResource(R.string.compass_help_line2))
                    Text(stringResource(R.string.compass_help_line3))
                    Text(stringResource(R.string.compass_help_line4))
                    Text(stringResource(R.string.compass_help_line5))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}





// --- Componentes --------------------------------------------------------------
@Composable
private fun CompassDial(
    diameterDp: Dp,
    ringRotationDeg: Float,         // el anillo gira en este ángulo
    northColor: Color,
    tickColor: Color,               // onSurface para líneas
    ringColor: Color                // onSurface low alpha
) {
    val directions = listOf(stringResource(R.string.compass_north_abbr),
        stringResource(R.string.compass_northeast_abbr),
        stringResource(R.string.compass_east_abbr),
        stringResource(R.string.compass_southeast_abbr),
        stringResource(R.string.compass_south_abbr),
        stringResource(R.string.compass_southwest_abbr),
        stringResource(R.string.compass_west_abbr),
        stringResource(R.string.compass_northwest_abbr))
    val density = LocalDensity.current

    Box(
        modifier = Modifier.size(diameterDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Tamaño real del lienzo (px)
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseR = kotlin.math.min(w, h) / 2f
            val radiusOuter = baseR * 0.92f
            val radiusTicks = baseR * 0.86f

            // --- Marcas: cada 5° (mayores cada 30°). Giran segun la orientacion ---
            for (deg in 0 until 360 step 5) {
                val isMajor = deg % 30 == 0
                val len = if (isMajor) baseR * 0.22f else baseR * 0.12f
                val stroke = if (isMajor) baseR * 0.024f else baseR * 0.012f

                // 0° arriba → -90°
                val angleDeg = deg + ringRotationDeg
                val rad = Math.toRadians((angleDeg - 90f).toDouble())

                val sx = cx + radiusTicks * cos(rad).toFloat()
                val sy = cy + radiusTicks * sin(rad).toFloat()
                val ex = cx + (radiusTicks - len) * cos(rad).toFloat()
                val ey = cy + (radiusTicks - len) * sin(rad).toFloat()

                val clr = if (deg == 0) northColor
                else tickColor.copy(alpha = if (isMajor) 0.85f else 0.45f)

                drawLine(
                    color = clr,
                    start = androidx.compose.ui.geometry.Offset(sx, sy),
                    end = androidx.compose.ui.geometry.Offset(ex, ey),
                    strokeWidth = stroke
                )
            }

            // --- Aro exterior centrado ---
            drawCircle(
                color = ringColor,
                radius = radiusOuter,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = baseR * 0.04f)
            )

            // --- Triángulo fijo arriba (equilátero, punta hacia abajo) ---
            val offsetIn = baseR * 0.02f                  // pequeño margen hacia adentro del aro
            val baseY = cy - radiusOuter + offsetIn       // base horizontal del triángulo
            val hTri = baseR * 0.11f                      // altura del triángulo
            val side = (2f * hTri / sqrt(3f))             // lado de equilátero: h = (√3/2)*lado

            val leftX  = cx - side / 2f
            val rightX = cx + side / 2f
            val apexY  = baseY + hTri

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(leftX, baseY)
                lineTo(rightX, baseY)
                lineTo(cx, apexY)
                close()
            }
            drawPath(path = path, color = northColor)
        }

        // --- Letras: TRANSLADADAS con el anillo y SIEMPRE VERTICALES (sin rotación) ---
        val step = 360f / directions.size
        val labelRadiusPx = with(density) { diameterDp.toPx() / 2f } * 0.50f

        directions.forEachIndexed { i, dir ->
            val angleDeg = i * step + ringRotationDeg
            val rad = Math.toRadians((angleDeg - 90f).toDouble())
            val tx = (labelRadiusPx * cos(rad)).toFloat()
            val ty = (labelRadiusPx * sin(rad)).toFloat()

            Text(
                text = dir,
                fontSize = if (dir == "N") 32.sp else 20.sp, // respeta tus tamaños
                fontWeight = if (dir == "N") FontWeight.Bold else FontWeight.Medium,
                color = if (dir == "N") northColor else tickColor,
                modifier = Modifier.graphicsLayer {
                    // Partimos del centro del Box y trasladamos en px al perímetro
                    translationX = tx
                    translationY = ty
                }
            )
        }
    }
}



private fun headingLabel(angle: Float, context : Context): String {
    val a = ((angle % 360f) + 360f) % 360f
    val dir = when (a) {
        in 337.5f..360f, in 0f..22.5f -> context.getString(R.string.compass_north_abbr)
        in 22.5f..67.5f -> context.getString(R.string.compass_northeast_abbr)
        in 67.5f..112.5f -> context.getString(R.string.compass_east_abbr)
        in 112.5f..157.5f -> context.getString(R.string.compass_southeast_abbr)
        in 157.5f..202.5f -> context.getString(R.string.compass_south_abbr)
        in 202.5f..247.5f -> context.getString(R.string.compass_southwest_abbr)
        in 247.5f..292.5f -> context.getString(R.string.compass_west_abbr)
        else -> context.getString(R.string.compass_northwest_abbr)
    }
    return "${a.roundToInt()}° $dir"
}

@Composable
private fun TickHaptics(
    headingDeg: Float,
    majorStepDeg: Float = 30f, // coincide con tus marcas largas
    epsilonDeg: Float = 3f,    // margen de tolerancia ±3°
    cooldownMs: Long = 350L    // antirebote temporal
) {
    val haptic = LocalHapticFeedback.current

    fun norm(x: Float) = ((x % 360f) + 360f) % 360f
    fun angDiff(a: Float, b: Float): Float {
        var d = norm(a) - norm(b)
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return abs(d)
    }

    val totalTicks = (360f / majorStepDeg).roundToInt()
    var lastIdx by remember { mutableStateOf<Int?>(null) }
    var lastTime by remember { mutableLongStateOf(0L) }
    var wasInside by remember { mutableStateOf(false) }

    LaunchedEffect(headingDeg) {
        val a = norm(headingDeg)
        val idx = ((a / majorStepDeg).roundToInt() % totalTicks + totalTicks) % totalTicks
        val tickAngle = idx * majorStepDeg
        val inside = angDiff(a, tickAngle) <= epsilonDeg
        val now = SystemClock.elapsedRealtime()

        if (inside && (!wasInside || idx != lastIdx) && (now - lastTime) >= cooldownMs) {
            val type = if (idx == 0) HapticFeedbackType.LongPress   // Norte → fuerte
            else HapticFeedbackType.TextHandleMove       // Ticks → suave
            haptic.performHapticFeedback(type)
            lastTime = now
            lastIdx = idx
        }
        wasInside = inside
    }
}
