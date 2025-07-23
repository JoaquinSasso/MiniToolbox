package com.joasasso.minitoolbox.tools.recordatorios

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.coroutines.resume

data class RachaActividad(
    val emoji: String,
    val nombre: String,
    val inicio: String // fecha de inicio
)

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
                inicio = o.getString("inicio")
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
                    put("inicio", it.inicio)
                }
            )
        }
        prefs(context).edit { putString("rachas", arr.toString()) }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RachaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    var rachas by remember { mutableStateOf(RachaPrefs.loadAll(context)) }

    val hoy = remember { LocalDate.now() }

    LaunchedEffect(rachas) {
        RachaPrefs.saveAll(context, rachas)
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_habit_tracker), onBack, { showInfo = true }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ) { Icon(Icons.Default.Add, contentDescription = "Agregar actividad") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (rachas.isEmpty()) {
                item(){
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(
                            Modifier.padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Todavía no has agregado ningún hábito", fontSize = 20.sp)
                            Text("Pulsa el botón + para agregarlos.", fontSize = 20.sp)
                        }
                    }
                }
            }
            items(rachas) { racha ->
                val inicio = LocalDate.parse(racha.inicio)
                val dias = hoy.toEpochDay().toInt() - inicio.toEpochDay().toInt()
                val bg = getColorForDays(dias)
                val fg = getTextColorForCard(bg)

                Card(colors = CardDefaults.cardColors(containerColor = bg)) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(racha.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(racha.nombre, fontSize = 20.sp, color = fg)
                            Text(rangoMotivador(dias), fontSize = 14.sp, color = fg.copy(alpha = 0.7f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$dias días", fontSize = 24.sp, color = fg)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier.clickable {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val confirm = confirmResetDialog(context, racha.nombre, haptic)
                                        if (confirm) {
                                            rachas = rachas.map {
                                                if (it == racha) it.copy(inicio = LocalDate.now().toString()) else it
                                            }
                                        }
                                    }
                                }.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resetear", fontSize = 12.sp, color = fg)
                            }
                            Row(
                                Modifier.clickable {
                                    rachas = rachas - racha
                                }.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Eliminar", fontSize = 12.sp, color = fg)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddActividadDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { emoji, nombre, diasIniciales ->
                val inicioCalculado = LocalDate.now().minusDays(diasIniciales.toLong()).toString()
                rachas = rachas + RachaActividad(emoji, nombre, inicioCalculado)
                showAddDialog = false
            }
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text("¿Cómo funciona?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Puedes usar este contador para seguir buenos hábitos o dejar malos.")
                    Text("• Las rachas se calculan a partir de la fecha de inicio.")
                    Text("• Si ya llevabas una racha, puedes cargar la cantidad de días y se ajustará la fecha.")
                    Text("• Los colores y frases cambian según tu progreso.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun AddActividadDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var emoji by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf(TextFieldValue("")) }
    var dias by remember { mutableStateOf("0") }
    val ok = nombre.text.isNotBlank() && dias.toIntOrNull() != null

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        title = { Text("Agregar actividad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("Emoji") })
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") })
                OutlinedTextField(dias, { dias = it.filter(Char::isDigit) }, label = { Text("Días") })
            }
        },
        confirmButton = {
            TextButton(enabled = ok, onClick = {
                onAdd(emoji, nombre.text, dias.toInt())
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }) { Text("Cancelar") }
        }
    )
}

suspend fun confirmResetDialog(
    context: Context,
    nombre: String,
    haptic: HapticFeedback
): Boolean = suspendCancellableCoroutine { cont ->
    val dialog = AlertDialog.Builder(context)
        .setTitle("Resetear contador")
        .setMessage("¿Seguro que quieres reiniciar el contador de \"$nombre\" a cero?")
        .setPositiveButton("Sí") { _, _ ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            cont.resume(true)
        }
        .setNegativeButton("No") { _, _ ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            cont.resume(false)
        }
        .create()

    dialog.show()
}

// Utils

// Frases motivacionales/niveles según días
fun rangoMotivador(dias: Int): String = when (dias) {
    0 -> "¡Hoy arranca la racha! 🏁"
    1 -> "¡Sobreviviste el primer día! 🥳"
    2 -> "¡Dos días! ¡Ya es tendencia! 📈"
    3 -> "¡Tres días! ¡Esto va en serio! 😎"
    in 4..6 -> "¡Casi una semana! Ya puedes dar consejos. 👏"
    in 7..9 -> "¡Una semana entera! Tu familia estaría orgullosa. 👨‍👩‍👧‍👦"
    in 10..13 -> "¡Doble dígito! Ahora ya puedes presumir. 💬"
    in 14..20 -> "¡Dos semanas! Dicen que ya es hábito... ¿Será? 🤔"
    in 21..29 -> "¡Tres semanas! ¡Mira esa constancia! 🚴"
    in 30..44 -> "¡Un mes! ¡Eres una máquina! 🤖"
    in 45..59 -> "¡Ya perdí la cuenta! ¿Quién eres? 👀"
    in 60..89 -> "¡Dos meses! ¡Esto dura más que mi serie favorita! 📺"
    in 90..179 -> "¡Tres meses! Leyenda en progreso. 🏅"
    in 180..364 -> "¡Medio año! Ya puedes darte el lujo de olvidar cómo era antes. 🧠"
    in 365..729 -> "¡Un año! Si hubiera un club, ya serías presidente. 🏅"
    in 730..1094 -> "¡Dos años! Seguro ya eres una leyenda urbana. 🕵️‍♂️"
    else -> "¡Racha épica! ¿Ya te hiciste famoso? 🦸"
}


/** Cambia color del card según días (progresivo) */
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

fun getTextColorForCard(bg: Color): Color =
    if ((0.299 * bg.red + 0.587 * bg.green + 0.114 * bg.blue) < 0.5) Color.White else Color.Black
