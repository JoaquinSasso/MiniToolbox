package com.joasasso.minitoolbox.tools.info

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.CountryResponse
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisesInfoScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val formatter = remember { NumberFormat.getInstance(Locale("es", "AR")) }

    var countries by remember { mutableStateOf<List<CountryResponse>>(emptyList()) }
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCountry by remember { mutableStateOf<CountryResponse?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(true) }

    LaunchedEffect(true) {
        countries = loadCountriesFromAssets(context)
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
            OutlinedTextField(
                value = search,
                onValueChange = { new ->
                    search = new
                    showList = true
                    if (new.text.isEmpty()) {
                        selectedCountry = null
                    }
                },
                label = { Text("Buscar país") },
                modifier = Modifier.fillMaxWidth()
            )

            val filtered = countries
                .filter { country ->
                    val common = country.name?.common.orEmpty()
                    val official = country.name?.official.orEmpty()
                    common.contains(search.text, ignoreCase = true) ||
                            official.contains(search.text, ignoreCase = true)
                }
                .sortedBy { it.name?.common }

            if (showList && filtered.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { country ->
                        TextButton(
                            onClick = {
                                selectedCountry = country
                                val name = country.name?.common ?: ""
                                search = TextFieldValue(
                                    text = name,
                                    selection = TextRange(name.length)
                                )
                                showList = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${country.flagEmoji ?: ""} ${country.name?.common ?: "N/A"}")
                        }
                    }
                }
            }

            selectedCountry?.let { country ->
                Spacer(Modifier.height(16.dp))
                val nameES = country.name?.common ?: "N/A"
                val nameOfficial = country.name?.official ?: "N/A"
                val nativeName = country.name?.nativeName
                    ?.values?.firstOrNull()
                    ?.common ?: "N/A"

                val currency = country.currencies?.entries?.firstOrNull()
                val currencyName = currency?.value?.name ?: "N/A"
                val currencyCode = currency?.key ?: ""

                Text("🌍 Nombre (ES): $nameES", style = MaterialTheme.typography.titleMedium)
                Text("🌐 Nombre oficial: $nameOfficial")
                Text("📝 Nombre nativo: $nativeName")
                Text("🏛️ Capital: ${country.capital?.firstOrNull() ?: "N/A"}")
                Text("💰 Moneda: $currencyName${if (currencyCode.isNotEmpty()) " ($currencyCode)" else ""}")
                Text("📞 Código tel.: ${
                    buildString {
                        append(country.idd?.root ?: "")
                        if (!country.idd?.suffixes.isNullOrEmpty()) {
                            append(country.idd.suffixes.firstOrNull() ?: "")
                        }
                    }.ifEmpty { "N/A" }
                }")
                Text("👥 Población: ${country.population?.let { formatter.format(it) } ?: "N/A"}")
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Información de Países") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Consulta rápidamente datos básicos de todos los países: nombre en español, oficial, nativo, capital, moneda, idioma, teléfono y población.")
                    Text("• Fuente de datos: Dataset local (offline), actualizado al 2 de Julio de 2025.")
                    Text("• Guía rápida:")
                    Text("   – Ingresá texto para buscar un país.")
                    Text("   – Seleccioná un país para ver su información detallada.")
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

fun loadCountriesFromAssets(context: Context): List<CountryResponse> {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val type = Types.newParameterizedType(List::class.java, CountryResponse::class.java)
    val adapter = moshi.adapter<List<CountryResponse>>(type)

    val json = context.assets.open("countries_dataset_translated.json")
        .bufferedReader().use { it.readText() }

    return adapter.fromJson(json) ?: emptyList()
}
