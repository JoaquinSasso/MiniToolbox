package com.joasasso.minitoolbox.tools.info

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.CountryOuterClass
import com.joasasso.minitoolbox.data.CountryResponse
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val locale = Locale.getDefault()
    val formatter = remember { NumberFormat.getInstance(locale) }
    val isEnglish = locale.language == "en"

    var countries by remember { mutableStateOf<List<CountryResponse>>(emptyList()) }
    var search by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCountry by remember { mutableStateOf<CountryResponse?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(true) }

    //Cargar dataset con información de países
    LaunchedEffect(Unit) {
        val bytes = context.assets.open("countries_dataset.pb").readBytes()
        val protoList = CountryOuterClass.CountryList.parseFrom(bytes)
        countries = protoList.countriesList.map {
            CountryResponse(
                name = it.name,
                englishName = it.englishName, // nuevo campo
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

    //Volver a la lista de países al realizar gesto de volver atras
    BackHandler(enabled = selectedCountry != null) {
        selectedCountry = null
        showList = true
        search = TextFieldValue("")
    }


    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.tool_country_info),
                onBack,
                { showInfo = true })
        }
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
                    label = { Text(stringResource(R.string.country_search_label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                val filtered = countries
                    .filter {
                        val searchText = search.text.lowercase()
                        it.name.lowercase().contains(searchText) ||
                                it.official.lowercase().contains(searchText) ||
                                it.englishName.lowercase().contains(searchText)
                    }
                    .sortedBy { if (isEnglish) it.englishName else it.name }

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
                                    val displayName = if (isEnglish) country.englishName else country.name
                                    search = TextFieldValue(text = displayName, selection = TextRange(displayName.length))
                                    showList = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${country.flag} ${if (isEnglish) country.englishName else country.name}")
                            }
                        }
                    }
                }

                selectedCountry?.let { country ->
                    Spacer(Modifier.height(16.dp))
                    selectedCountry?.let { country ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "🌍 ${stringResource(R.string.country_common_name)}: ${if (isEnglish) country.englishName else country.name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("🌐 ${stringResource(R.string.country_official_name)}: ${country.official}")
                        Text("📝 ${stringResource(R.string.country_native_name)}: ${country.native}")
                        Text("🏛️ ${stringResource(R.string.country_capital)}: ${if (country.capital.isNotEmpty()) country.capital.joinToString() else "N/A"}")
                        Text("💰 ${stringResource(R.string.country_currency)}: ${country.currency}")
                        Text("📞 ${stringResource(R.string.country_phone_code)}: ${country.phoneCode}")
                        Text(
                            "👥 ${stringResource(R.string.country_population)}: ${
                                formatter.format(
                                    country.population
                                )
                            }"
                        )
                    }
                }
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(stringResource(R.string.country_info_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.country_info_line1))
                        Text(stringResource(R.string.country_info_line2))
                        Text(stringResource(R.string.country_info_line3))
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
}
