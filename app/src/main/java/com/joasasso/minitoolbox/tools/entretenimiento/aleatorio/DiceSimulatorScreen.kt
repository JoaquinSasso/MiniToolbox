package com.joasasso.minitoolbox.tools.entretenimiento.aleatorio

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.DadosHistorialRepository
import com.joasasso.minitoolbox.data.TiradaDeDados
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LanzadorDadosScreen(
    onBack: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }
    var cantidad by remember { mutableIntStateOf(1) }
    val rerollOnes = remember { mutableStateOf(false) }
    val rerollOnesOrTwos = remember { mutableStateOf(false) }
    val resultados = remember { mutableStateListOf<Int>() }
    var sumaTotal by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context: Context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = dados, 1 = historial
    val historial by DadosHistorialRepository.flujoHistorial(context).collectAsState(initial = emptyList())
    var sliderValue by remember { mutableFloatStateOf(cantidad.toFloat()) }
    var lastCantidad by remember { mutableIntStateOf(cantidad) }

    val tiposDados = listOf(4, 6, 8, 10, 12, 20, 100)

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_dice), onBack) { showInfo = true }},
        bottomBar = {
            if (selectedTab == 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()){

                    Text(stringResource(R.string.dice_cantidad_dados, cantidad), style = MaterialTheme.typography.titleMedium)

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            val nuevoValor = it.toInt()
                            if (nuevoValor != lastCantidad) {
                                cantidad = nuevoValor
                                lastCantidad = nuevoValor
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                        },
                        valueRange = 1f..20f,
                        steps = 19,
                        modifier = Modifier
                            .padding(horizontal = 40.dp)
                            .fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.dice_tab_dados))
                }

                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.dice_tab_historial))
                }
            }


            when (selectedTab) {
                0 -> {
                    val dadosFila1 = tiposDados.take(4)
                    val dadosFila2 = tiposDados.drop(4)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            dadosFila1.forEach { caras ->
                                DadoIcono(
                                    caras = caras,
                                    cantidad = cantidad,
                                    resultados = resultados,
                                    scope = scope,
                                    context = context,
                                    onResultado = { sumaTotal = resultados.sum() },
                                    rerollOnes = rerollOnes.value,
                                    rerollOnesOrTwos = rerollOnesOrTwos.value
                                )
                                    sumaTotal = resultados.sum()
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            dadosFila2.forEach { caras ->
                                DadoIcono(
                                    caras = caras,
                                    cantidad = cantidad,
                                    resultados = resultados,
                                    scope = scope,
                                    context = context,
                                    onResultado = { sumaTotal = resultados.sum() },
                                    rerollOnes = rerollOnes.value,
                                    rerollOnesOrTwos = rerollOnesOrTwos.value
                                )
                                    sumaTotal = resultados.sum()
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Checkbox(
                                    checked = rerollOnes.value,
                                    onCheckedChange = { rerollOnes.value = it; if (it) rerollOnesOrTwos.value = false }
                                )
                                Text(stringResource(R.string.dice_reroll_1))

                                Checkbox(
                                    checked = rerollOnesOrTwos.value,
                                    onCheckedChange = { rerollOnesOrTwos.value = it; if (it) rerollOnes.value = false }
                                )
                                Text(stringResource(R.string.dice_reroll_1_or_2))
                            }
                        }


                        if (resultados.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Total: $sumaTotal", style = MaterialTheme.typography.headlineSmall)
                                Text("Resultados: ${resultados.joinToString()}")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp)) // último espacio para evitar corte
                    }
                }

                1 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(historial.take(30)) { tirada ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(12.dp)
                            ) {
                                Text("D${tirada.tipo} x${tirada.cantidad}", style = MaterialTheme.typography.titleMedium)
                                Text( stringResource(R.string.dice_cantidad_dados, tirada.cantidad), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.dice_resultados, tirada.resultados.joinToString()))
                                Text(
                                    text = "Total: ${tirada.resultados.sum()} • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(tirada.timestamp))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.dice_info_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dice_info_1))
                    Text(stringResource(R.string.dice_info_2))
                    Text(stringResource(R.string.dice_info_3))
                    Text(stringResource(R.string.dice_info_4))
                    Text(stringResource(R.string.dice_info_5))
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

@DrawableRes
fun getDadoSvg(caras: Int): Int {
    return when (caras) {
        4 -> R.drawable.d4
        6 -> R.drawable.d6
        8 -> R.drawable.d8
        10 -> R.drawable.d10
        12 -> R.drawable.d12
        20 -> R.drawable.d20
        100 -> R.drawable.d100
        else -> R.drawable.d6
    }
}
@Composable
fun DadoIcono(
    caras: Int,
    cantidad: Int,
    resultados: MutableList<Int>,
    scope: CoroutineScope,
    context: Context,
    onResultado: () -> Unit,
    rerollOnes: Boolean,
    rerollOnesOrTwos: Boolean
) {
    val haptic = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            resultados.clear()

            repeat(cantidad) {
                val range = when {
                    rerollOnesOrTwos -> (3..caras)
                    rerollOnes       -> (2..caras)
                    else             -> (1..caras)
                }

                val result = range.random()
                resultados.add(result)
            }


            onResultado()

            scope.launch {
                DadosHistorialRepository.guardarTirada(
                    context,
                    TiradaDeDados(caras, cantidad, resultados.toList(), System.currentTimeMillis())
                )
            }
        }) {
            Icon(
                painterResource(id = getDadoSvg(caras)),
                contentDescription = "D$caras",
                modifier = Modifier.size(48.dp)
            )
        }
        Text("D$caras")
    }
}


