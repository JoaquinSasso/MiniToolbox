package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.GastosDataStore
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReunionesScreen(onBack: () -> Unit, onCrearReunion: () -> Unit, onReunionClick: (Reunion) -> Unit) {
    val context = LocalContext.current

    var reuniones by remember { mutableStateOf<List<Reunion>>(emptyList()) }

    // Cargar reuniones desde el DataStore
    LaunchedEffect(Unit) {
        GastosDataStore.getReuniones(context).collect {
            reuniones = it
        }
    }
    Scaffold(
        topBar = { TopBarReusable("Divisor de Gastos", onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearReunion) {
                Icon(Icons.Default.Add, contentDescription = "Nueva reunión")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (reuniones.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay reuniones guardadas.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reuniones) { reunion ->
                        ReunionItem(reunion = reunion, onClick = { onReunionClick(reunion) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReunionItem(reunion: Reunion, onClick: () -> Unit) {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaTexto = formato.format(Date(reunion.fecha))
    val integrantes = reunion.integrantes.map { it.nombre }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(reunion.nombre, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Fecha: $fechaTexto", style = MaterialTheme.typography.bodyMedium)
            Text("Integrantes: ${integrantes.joinToString()}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
