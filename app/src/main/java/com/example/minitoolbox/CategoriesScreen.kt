package com.example.minitoolbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class Category(
    val name: String,
    val icon: ImageVector,
    val tools: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onToolSelected: (String) -> Unit) {
    val categories = listOf(
        Category(
            name = "Herramientas",
            icon = Icons.Default.Build,
            tools = listOf("Generador de colores", "Selector de grupos", "Ruleta selectora")
        ),
        Category(
            name = "Calculadoras",
            icon = Icons.Default.Functions,
            tools = listOf("Calculadora de edad", "Calculadora de propina")
        ),
        Category(
            name = "Minijuegos",
            icon = Icons.Default.SportsEsports,
            tools = listOf("Medidor de reacción", "Contador de clics")
        )
    )

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val selectedCategory = categories[selectedCategoryIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCategory.name) }
            )
        },
        bottomBar = {
            NavigationBar {
                categories.forEachIndexed { index, category ->
                    NavigationBarItem(
                        icon = { Icon(category.icon, contentDescription = category.name) },
                        selected = index == selectedCategoryIndex,
                        onClick = { selectedCategoryIndex = index },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(selectedCategory.tools) { tool ->
                Card(
                    onClick = { onToolSelected(tool) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tool,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
