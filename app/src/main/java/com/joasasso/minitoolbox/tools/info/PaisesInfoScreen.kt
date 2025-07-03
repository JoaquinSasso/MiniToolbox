package com.joasasso.minitoolbox.tools.info

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.CountryOuterClass
import com.joasasso.minitoolbox.tools.data.CountryResponse
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisesInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val formatter = remember { NumberFormat.getInstance(Locale("es", "AR")) }

    var countries by remember { mutableStateOf<List<CountryResponse>>(emptyList()) }
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCountry by remember { mutableStateOf<CountryResponse?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val t0 = System.currentTimeMillis()
        countries = withContext(Dispatchers.IO) {
            val tRead0 = System.currentTimeMillis()
            val bytes = context.assets.open("countries_dataset.pb").readBytes()
            val tRead1 = System.currentTimeMillis()
            Log.d("CountryLoad", "Lectura binaria: ${tRead1 - tRead0} ms")

            val tParse0 = System.currentTimeMillis()
            val protoList = CountryOuterClass.CountryList.parseFrom(bytes)
            val tParse1 = System.currentTimeMillis()
            Log.d("CountryLoad", "Parseo binario: ${tParse1 - tParse0} ms")

            protoList.countriesList.map {
                CountryResponse(
                    name = it.name,
                    official = it.official,
                    native = it.native,
                    currency = it.currency,
                    capital = it.capitalList,
                    phoneCode = it.phoneCode,
                    flag = it.flag,
                    population = it.population
                )
            }
        }
        val t1 = System.currentTimeMillis()
        Log.d("CountryLoad", "Carga + parseo total: ${t1 - t0} ms")
    }

    Scaffold(
        topBar = { TopBarReusable("Información de Países", onBack, { showInfo = true }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (countries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                        showList = true
                        if (it.text.isEmpty()) selectedCountry = null
                    },
                    label = { Text("Buscar país") },
                    modifier = Modifier.fillMaxWidth()
                )

                val filtered = countries
                    .filter {
                        it.name.contains(search.text, ignoreCase = true) ||
                                it.official.contains(search.text, ignoreCase = true)
                    }
                    .sortedBy { it.name }

                if (showList && filtered.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered, key = { it.name }) { country ->
                            TextButton(
                                onClick = {
                                    selectedCountry = country
                                    search = TextFieldValue(
                                        text = country.name,
                                        selection = TextRange(country.name.length)
                                    )
                                    showList = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${country.flag} ${country.name}")
                            }
                        }
                    }
                }

                selectedCountry?.let { country ->
                    Spacer(Modifier.height(16.dp))
                    Text("🌍 Nombre (ES): ${country.name}", style = MaterialTheme.typography.titleMedium)
                    Text("🌐 Nombre oficial: ${country.official}")
                    Text("📝 Nombre nativo: ${country.native}")
                    Text("🏛️ Capital: ${if (country.capital.isNotEmpty()) country.capital.joinToString() else "N/A"}")
                    Text("💰 Moneda: ${country.currency}")
                    Text("📞 Código tel.: ${country.phoneCode}")
                    Text("👥 Población: ${formatter.format(country.population)}")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Información de Países") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Consulta rápida de países (offline, binario Protobuf).")
                    Text("• Carga optimizada del dataset binario.")
                    Text("• Seleccioná un país para ver su información detallada.")
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
