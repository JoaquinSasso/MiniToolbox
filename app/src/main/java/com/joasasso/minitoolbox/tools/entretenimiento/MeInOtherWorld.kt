package com.joasasso.minitoolbox.tools.entretenimiento

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun MiYoDelMultiversoScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var resultado by remember { mutableStateOf("") }
    val fadeAnim by animateFloatAsState(targetValue = if (resultado.isNotBlank()) 1f else 0f)
    var backgroundColor by remember { mutableStateOf(ComposeColor(0xFFFFF176)) }
    var textBackgroundColor: Int = 0


    val emojis = listOf(
        "🧙", "🤖", "👑", "🐸", "👻", "🐉", "🧞", "🛸", "🐍", "☠️",
        "🦄", "🍕", "🍄", "🐐", "🦕", "🧛", "👽", "🐙", "🎩", "👾",
        "🎃", "🧠", "🧃", "🪩", "🐢"
    )
    val nombres = listOf(
        "hechicero jubilado", "cajero automático con alma de poeta", "emperador de memes", "lagarto influencer",
        "rey del karaoke intergaláctico", "zombie filósofo", "DJ en un culto de caracoles", "vampiro vegano",
        "bot de Twitter", "dios del WiFi", "pingüino emprendedor", "papa frita semiconsciente", "pastelero intergaláctico",
        "dinosaurio con MBA", "luz de emergencia ansiosa", "cebra con doble identidad", "koala programador de C++",
        "caracol filósofo estoico", "drone existencialista", "sandía con complejo de superioridad",
        "código QR sensible", "cactus extrovertido", "pez payaso antisocial", "cuadro abstracto que llora",
        "horno microondas con sueños"
    )
    val roles = listOf(
        "que cría alpacas fluorescentes", "que da clases de filosofía a grillos", "que canta boleros con hologramas",
        "que dirige una secta de plantas carnívoras", "que colecciona recuerdos ajenos", "que hackea tostadoras",
        "que flota en spas gravitacionales", "que organiza fiestas para fantasmas", "que trota en cámara lenta",
        "que baila reguetón metafísico", "que cocina sin ingredientes", "que convence a piedras de cambiar de opinión",
        "que da discursos motivacionales a hojas secas", "que entrena patitos para el combate", "que murmura secretos a enchufes",
        "que juega ajedrez en 5 dimensiones", "que pelea con pelusas cósmicas", "que programa en BASIC por gusto",
        "que vende NFT de pensamientos", "que actúa en sueños ajenos", "que resuelve acertijos de unicornios borrachos",
        "que documenta la vida de los calcetines perdidos", "que colecciona likes de galaxias lejanas",
        "que escribe haikus en código Morse", "que guarda silencio en idiomas extintos"
    )
    val ubicaciones = listOf(
        "en Marte", "en la dimensión 404", "en una línea temporal olvidada", "en el año 3022",
        "en un mundo hecho de gelatina", "en la mente de un ornitorrinco", "en una app abandonada",
        "en la deep web emocional", "en una cafetería interdimensional", "en un universo sin vocales",
        "en un sueño compartido por ardillas", "en la nube de datos de un tostador", "en un reality show con fantasmas",
        "en la república independiente de mi heladera", "en una novela de ciencia ficción escrita por una lombriz",
        "en un loop de TikToks infinitos", "en el metaverso de los memes", "en el grupo de WhatsApp de los multiversos",
        "en la playlist de una IA deprimida", "en la corte suprema de los calcetines perdidos",
        "en un salón de belleza de dragones", "en un servidor de Minecraft medieval", "en un algoritmo de horóscopos falsos",
        "en una dimensión pixelada", "en una fábrica de abrazos sintéticos"
    )

    fun generarResultadoAleatorio(): String {
        val emoji = emojis.random()
        val nombre = nombres.random()
        val rol = roles.random()
        val ubicacion = ubicaciones.random()
        return "$emoji Sos un $nombre $rol $ubicacion."
    }

    var imagenResultado by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = { TopBarReusable("Mi yo del multiverso", onBack) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (imagenResultado != null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val cacheDir = context.cacheDir
                                    val file = File(cacheDir, "yo_multiverso.png")

                                    // Marca de agua
                                    val bitmap = imagenResultado!!
                                    val resultBitmap = createBitmap(bitmap.width, bitmap.height + 80)
                                    val canvas = Canvas(resultBitmap)
                                    canvas.drawColor(textBackgroundColor)
                                    canvas.drawBitmap(bitmap, 0f, 0f, null)

                                    val paint = Paint().apply {
                                        color = Color.DKGRAY
                                        textSize = 48f
                                        textAlign = Paint.Align.CENTER
                                        isAntiAlias = true
                                    }

                                    canvas.drawText(
                                        "#MiniToolbox",
                                        resultBitmap.width / 2f,
                                        bitmap.height + 60f,
                                        paint
                                    )

                                    FileOutputStream(file).use { out ->
                                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }

                                    val uri: Uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir imagen con..."))
                                } catch (e: Exception) {
                                    Log.e("MiYoDelMultiverso", "Error exportando imagen", e)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        text = { Text("Compartir imagen") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        resultado = generarResultadoAleatorio()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    text = { Text("Nuevo universo") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (resultado.isNotEmpty()) {
                val resultBitmap = remember(resultado) {
                    val paint = Paint().apply {
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                        color = Color.BLACK
                        textSize = 72f
                    }

                    val fondoColores = listOf(
                        "#FFF176", "#AED581", "#81D4FA", "#FFAB91", "#CE93D8", "#FFD54F", "#B39DDB", "#FFCDD2"
                    )

                    val fondoColor = fondoColores.random().toColorInt()
                    textBackgroundColor = fondoColor
                    backgroundColor = ComposeColor(fondoColor)

                    val width = 1080
                    val height = 600
                    val bitmap = createBitmap(width, height)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(fondoColor)

                    val paddingPx = 64
                    val maxWidth = width - 100
                    val words = resultado.split(" ")
                    val lines = mutableListOf<String>()
                    var currentLine = ""

                    for (word in words) {
                        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                        val textWidth = paint.measureText(testLine)
                        if (textWidth <= maxWidth) {
                            currentLine = testLine
                        } else {
                            lines.add(currentLine)
                            currentLine = word
                        }
                    }
                    if (currentLine.isNotEmpty()) lines.add(currentLine)

                    lines.forEachIndexed { index, line ->
                        canvas.drawText(line, width / 2f, (index + 1) * 100f + paddingPx, paint)
                    }

                    bitmap
                }
                imagenResultado = resultBitmap

                Image(
                    painter = BitmapPainter(resultBitmap.asImageBitmap()),
                    contentDescription = "Resultado multiverso",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, backgroundColor, RoundedCornerShape(12.dp))
                        .background(backgroundColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .alpha(fadeAnim)
                )


            } else {
                Button(
                    onClick = {
                        resultado = generarResultadoAleatorio()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("🌌 Descubrir quién soy en otro universo", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
