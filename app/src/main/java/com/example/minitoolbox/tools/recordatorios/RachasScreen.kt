package com.example.minitoolbox.tools.recordatorios

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import androidx.core.content.edit
import kotlinx.coroutines.launch

// ---- Data model ----
data class RachaActividad(
    val emoji: String,
    val nombre: String,
    val dias: Int,
    val ultimoDia: String // yyyy-MM-dd
)

// ---- Simple persistence with SharedPreferences ----
object RachaPrefs {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("racha_prefs", Context.MODE_PRIVATE)

    fun loadAll(context: Context): List<RachaActividad> {
        val raw = prefs(context).getString("rachas", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            RachaActividad(
                emoji = o.getString("emoji"),
                nombre = o.getString("nombre"),
                dias = o.getInt("dias"),
                ultimoDia = o.getString("ultimoDia")
            )
        }
    }

    fun saveAll(context: Context, list: List<RachaActividad>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().apply {
                    put("emoji", it.emoji)
                    put("nombre", it.nombre)
                    put("dias", it.dias)
                    put("ultimoDia", it.ultimoDia)
                }
            )
        }
        prefs(context).edit { putString("rachas", arr.toString()) }
    }
}

// ---- Main screen ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContadorRachaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showInfo by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var rachas by remember { mutableStateOf(RachaPrefs.loadAll(context)) }

    // Cada vez que cambia la lista, la guardamos
    LaunchedEffect(rachas) {
        RachaPrefs.saveAll(context, rachas)
    }

    // Actualizar todos los contadores si cambia el día
    LaunchedEffect(Unit) {
        val hoy = LocalDate.now()
        val updated = rachas.map {
            val last = LocalDate.parse(it.ultimoDia)
            val diff = hoy.toEpochDay() - last.toEpochDay()
            if (diff > 0) it.copy(dias = it.dias + diff.toInt(), ultimoDia = hoy.toString()) else it
        }
        if (updated != rachas) rachas = updated
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contador de dias en Racha") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ) { Icon(Icons.Default.Add, contentDescription = "Agregar actividad") }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (rachas.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aún no agregaste ninguna actividad.", fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rachas) { racha ->
                        val backgroundColor = getColorForDays(racha.dias)
                        val textColor = getTextColorForCard(backgroundColor)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(racha.emoji, fontSize = 32.sp)
                                Spacer(Modifier.width(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = racha.nombre,
                                        fontSize = 20.sp,
                                        color = textColor
                                    )
                                    Text(
                                        text = rangoMotivador(racha.dias),
                                        fontSize = 15.sp,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Text(
                                        text = "${racha.dias} días",
                                        fontSize = 26.sp,  // Más grande
                                        color = textColor
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier.height(IntrinsicSize.Min) // Solo el espacio necesario
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable {
                                                        scope.launch {
                                                            val confirmed = confirmResetDialog(context, racha.nombre, haptic)
                                                            if (confirmed) {
                                                                rachas = rachas.map {
                                                                    if (it == racha) it.copy(dias = 0, ultimoDia = LocalDate.now().toString()) else it
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .padding(2.dp) // mínimo padding para tocar bien
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Resetear",
                                                    tint = textColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text("Resetear", fontSize = 12.sp, color = textColor)
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable {
                                                        scope.launch {
                                                            val confirmed = confirmResetDialog(context, "Eliminar \"${racha.nombre}\"", haptic)
                                                            if (confirmed) {
                                                                rachas = rachas - racha
                                                            }
                                                        }
                                                    }
                                                    .padding(2.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = textColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text("Eliminar", fontSize = 12.sp, color = textColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de agregar actividad
    if (showAddDialog) {
        AddActividadDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { emoji, nombre, dias ->
                rachas = rachas + RachaActividad(
                    emoji = emoji,
                    nombre = nombre,
                    dias = dias,
                    ultimoDia = LocalDate.now().toString()
                )
                showAddDialog = false
            }
        )
    }

    // Info
    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text("¿Cómo funciona?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Puedes usar este contador para motivarte tanto a dejar de hacer hábitos negativos (por ejemplo: fumar, consumir azúcar, etc.) como para mantener hábitos positivos (por ejemplo: días haciendo ejercicio, meditando, aprendiendo algo nuevo, etc.).")
                    Text("• Puedes agregarle un nombre motivador y un emoji opcional para personalizar cada actividad.")
                    Text("• Si ya llevabas una racha, puedes cargar la cantidad de días al crear la actividad.")
                    Text("• Cada día, a las 0hs, el contador suma uno automáticamente.")
                    Text("• Si incumples tu objetivo, usa el botón Resetear para reiniciar el contador a cero.")
                    Text("• Los colores y frases motivadoras cambian según tu progreso, representando distintos niveles de racha. ¡Supera tus récords y mantené tu motivación en alto!")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// --- Utilidades y diálogos ---

@Composable
fun AddActividadDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var emoji by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var dias by remember { mutableStateOf("0") }
    val focusOk = nombre.text.isNotBlank() && dias.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        title = { Text("Agregar actividad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = {
                        // Solo dejar máximo 2 caracteres (evita textos largos)
                        emoji = it.take(2)
                    },
                    label = { Text("Emoji (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la actividad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dias,
                    onValueChange = { dias = it.filter { c -> c.isDigit() } },
                    label = { Text("Días iniciales") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = focusOk,
                onClick = {
                    onAdd(emoji, nombre.text, dias.toIntOrNull() ?: 0)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ) { Text("Cancelar") }
        }
    )
}



@OptIn(ExperimentalCoroutinesApi::class)
suspend fun confirmResetDialog(
    context: Context,
    nombre: String,
    haptic: HapticFeedback
): Boolean = suspendCancellableCoroutine { cont ->
    val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
        .setTitle("Resetear contador")
        .setMessage("¿Seguro que quieres reiniciar el contador de \"$nombre\" a cero?")
        .setPositiveButton("Sí") { d, _ ->
            cont.resume(true) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        .setNegativeButton("No") { d, _ ->
            cont.resume(false) {}
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        .create()
    dialog.show()
}

// Frases motivacionales/niveles según días
fun rangoMotivador(dias: Int): String = when {
    dias == 0  -> "¡Hoy arranca la racha! 🏁"
    dias == 1  -> "¡Sobreviviste el primer día! 🥳"
    dias == 2  -> "¡Dos días! ¡Ya es tendencia! 📈"
    dias == 3  -> "¡Tres días! ¡Esto va en serio! 😎"
    dias in 4..6  -> "¡Casi una semana! Ya puedes dar consejos. 👏"
    dias in 7..9  -> "¡Una semana entera! Tu familia estaría orgullosa. 👨‍👩‍👧‍👦"
    dias in 10..13 -> "¡Doble dígito! Ahora ya puedes presumir. 💬"
    dias in 14..20 -> "¡Dos semanas! Dicen que ya es hábito... ¿Será? 🤔"
    dias in 21..29 -> "¡Tres semanas! ¡Mira esa constancia! 🚴"
    dias in 30..44 -> "¡Un mes! ¡Eres una máquina! 🤖"
    dias in 45..59 -> "¡Ya perdí la cuenta! ¿Quién eres? 👀"
    dias in 60..89 -> "¡Dos meses! ¡Esto dura más que mi serie favorita! 📺"
    dias in 90..179 -> "¡Tres meses! Leyenda en progreso. 🏅"
    dias in 180..364 -> "¡Medio año! Ya puedes darte el lujo de olvidar cómo era antes. 🧠"
    dias in 365..729 -> "¡Un año! Si hubiera un club, ya serías presidente. 🏅"
    dias in 730..1094 -> "¡Dos años! Seguro ya eres una leyenda urbana. 🕵️‍♂️"
    else -> "¡Racha épica! ¿Ya te hiciste famoso? 🦸"
}


// Cambia color del card según días (progresivo)
fun getColorForDays(dias: Int): Color = when {
    dias == 0 -> Color(0xFFEEEFF1)
    dias < 3  -> Color(0xFFB3E5FC)
    dias < 7  -> Color(0xFFB2DFDB)
    dias < 14 -> Color(0xFFDCEDC8)
    dias < 30 -> Color(0xFFFFF9C4)
    dias < 90 -> Color(0xFFFFE0B2)
    dias < 365 -> Color(0xFFFFCCBC)
    else     -> Color(0xFFD1C4E9)
}

fun getTextColorForCard(background: Color): Color {
    val luminancia = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue)
    return if (luminancia < 0.5) Color.White else Color.Black
}
