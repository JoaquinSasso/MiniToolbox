package com.joasasso.minitoolbox.tools.generadores

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

enum class NombreTipo(@StringRes val stringResId: Int) {
    Masculino(R.string.name_generator_masculino),
    Femenino(R.string.name_generator_femenino),
    Mixto(R.string.name_generator_mixto),
    Gracioso(R.string.name_generator_gracioso)
}


enum class IdiomaNombre(@StringRes val stringResId: Int) {
    Espanol(R.string.name_generator_idioma_es),
    Ingles(R.string.name_generator_idioma_en)
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorNombresScreen(onBack: () -> Unit) {
    val nombresMasculinosEs = listOf(
        "Lucas", "Mateo", "Juan", "Santiago", "Benjamín", "Lautaro", "Martín", "Joaquín", "Tomás", "Facundo",
        "Emiliano", "Franco", "Bruno", "Andrés", "Ramiro", "Nicolás", "Agustín", "Iván", "Matías", "Esteban",
        "Felipe", "Valentín", "Santino", "Ulises"
    )
    val nombresFemeninosEs = listOf(
        "Sofía", "Valentina", "Martina", "Camila", "Lucía", "Isabella", "Emilia", "Julieta", "Mía", "Agustina",
        "Gabriela", "Aitana", "Milagros", "Paula", "Florencia", "Renata", "Jazmín", "Bianca", "Carolina", "Nicole",
        "Ana", "Laura", "Juliana", "María", "Guadalupe"
    )
    val apellidosEs = listOf(
        "Pérez", "Gómez", "Rodríguez", "Fernández", "López", "Díaz", "Martínez", "Romero", "Sosa", "Torres",
        "Álvarez", "Acosta", "Silva", "Suárez", "Castro", "Molina", "Ortiz", "Medina", "Herrera", "Gutiérrez",
        "Cruz", "Moreno", "Reyes", "Ruiz", "Navarro", "Aguirre", "Rojas", "Vega", "Ibáñez", "Muñoz", "Garcia"
    )
    val nombresGraciososEs = listOf(
        "Elsa Pato",
        "Aitor Tilla",
        "Mario Neta",
        "Zoila Vaca",
        "Paco Merlo",
        "Igor Dito",
        "Elena Nito",
        "Alan Brito",
        "Olga Casco",
        "Juan Estan Camino",
        "Carlos Perez Gil",
        "Luz Cuesta Mogollón",
        "Elton Tito",
        "Elvis Nieto",
        "Esteban Dido",
        "Lola Mento",
        "Lucho Mucho",
        "Clara Mente",
        "Pancho Villa",
        "Coco Liso",
        "Cacho Castaña",
        "Chiqui Tapia",
        "Lola Lazo",
        "Zoila Cerda",
        "Kito Fokito",
        "Toka Fondo",
        "Aitor Menta",
        "Paco Jones",
        "Keko Jones",
        "Elsa Nitario",
        "Aitor Nillos"
    )

    val nombresMasculinosEn = listOf(
        "James", "Liam", "Oliver", "Benjamin", "Elijah",
        "Lucas", "Henry", "Alexander", "Jack", "William",
        "Ethan", "Noah", "Mason", "Logan", "Michael",
        "Daniel", "Jackson", "Sebastian", "Caleb", "Matthew"
    )

    val nombresFemeninosEn = listOf(
        "Emma", "Olivia", "Sophia", "Isabella", "Ava",
        "Charlotte", "Amelia", "Mia", "Luna", "Harper",
        "Ella", "Grace", "Scarlett", "Chloe", "Abigail",
        "Aria", "Emily", "Hazel", "Nora", "Layla"
    )

    val apellidosEn = listOf(
        "Smith", "Johnson", "Brown", "Jones", "Davis",
        "Miller", "Wilson", "Anderson", "Taylor", "Thomas",
        "Moore", "Martin", "White", "Clark", "Hall",
        "Lewis", "Young", "Walker", "King", "Wright"
    )

    val nombresGraciososEn = listOf(
        "Ben Dover", "Al Beback", "Justin Time", "Sue Flay", "Anita Bath",
        "Rick O'Shea", "Paige Turner", "Chris P. Bacon", "Sal Ami", "Barry Cuda",
        "Terry Aki", "Bea O’Problem", "Warren Peace", "Gail Forcewind", "Sonny Day",
        "Dinah Mite", "Bill Board", "Manny Jah", "Hal Jalikee", "Jonkey Donkey",
        "Ella Vator", "Ima Pigg", "Neil Down", "Lois Price", "Lance Boyle"
    )




    var tipo by remember { mutableStateOf(NombreTipo.Mixto) }
    var idioma by remember { mutableStateOf(IdiomaNombre.Ingles) }
    var incluirApellido by remember { mutableStateOf(true) }
    var nombre by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    fun generar() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        val nombresMasculinos = if (idioma == IdiomaNombre.Espanol) nombresMasculinosEs else nombresMasculinosEn
        val nombresFemeninos = if (idioma == IdiomaNombre.Espanol) nombresFemeninosEs else nombresFemeninosEn
        val apellidos = if (idioma == IdiomaNombre.Espanol) apellidosEs else apellidosEn
        val nombresGraciosos = if (idioma == IdiomaNombre.Espanol) nombresGraciososEs else nombresGraciososEn

        nombre = when (tipo) {
            NombreTipo.Masculino -> {
                val nom = nombresMasculinos.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Femenino -> {
                val nom = nombresFemeninos.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Mixto -> {
                val todos = nombresMasculinos + nombresFemeninos
                val nom = todos.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Gracioso -> nombresGraciosos.random()
        }
    }


    // Generar uno al inicio o cuando cambia tipo o el toggle
    LaunchedEffect(tipo, incluirApellido) { generar() }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_name_generator), onBack, {showInfo = true})}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.name_generator_subtitulo),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Botones en dos filas
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(NombreTipo.Masculino, NombreTipo.Femenino).forEach {
                        Button(
                            onClick = {
                                tipo = it
                                generar()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tipo == it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tipo == it) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(it.stringResId), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(NombreTipo.Mixto, NombreTipo.Gracioso).forEach {
                        Button(
                            onClick = {
                                tipo = it
                                generar()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tipo == it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tipo == it) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(it.stringResId), fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.name_generator_generado),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombre,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { generar() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.name_generator_boton_nuevo))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.name_generator_boton_nuevo))
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(nombre))
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }
            // Toggle al final
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.name_generator_toggle_apellido), fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = incluirApellido,
                    onCheckedChange = {
                        incluirApellido = it
                        generar()
                    }
                )
            }
            // Toggle para cambiar idiomas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.name_generator_idioma) + ": ", fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                // Idiomas disponibles, con sus nombres traducidos
                val idiomaOpcionesMap = IdiomaNombre.entries.associateBy { stringResource(it.stringResId) }
                val idiomaOpciones = idiomaOpcionesMap.keys.toList()
                val idiomaSeleccionado = stringResource(idioma.stringResId)

                SegmentedButton(
                    options = idiomaOpciones,
                    selected = idiomaSeleccionado,
                    onSelect = { selectedLabel ->
                        idiomaOpcionesMap[selectedLabel]?.let {
                            idioma = it
                            generar()
                        }
                    }
                )

            }

        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text(stringResource(R.string.name_generator_help_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.name_generator_help_linea1))
                    Text(stringResource(R.string.name_generator_help_linea2))
                    Text(stringResource(R.string.name_generator_help_linea3))
                    Text(stringResource(R.string.name_generator_help_linea4))
                    Text(stringResource(R.string.name_generator_help_linea5))
                    Text(stringResource(R.string.name_generator_help_linea6))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
