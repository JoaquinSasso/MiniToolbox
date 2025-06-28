// app/src/main/java/com/example/minitoolbox/CategoriesScreen.kt
package com.example.minitoolbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minitoolbox.tools.Tool
import com.example.minitoolbox.tools.ToolCategory
import com.example.minitoolbox.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    vm: CategoryViewModel = viewModel()
) {
    // Estado de la categoría seleccionada
    val selectedCategory by vm.selectedCategory
    // Haptic feedback
    val haptic = LocalHapticFeedback.current

    // Lista hardcodeada de categorías para evitar nulos
    val categories = listOf(
        ToolCategory.Generadores,
        ToolCategory.Calculadoras,
        ToolCategory.Juegos,
        ToolCategory.Informacion,
        ToolCategory.Recordatorios,
        ToolCategory.Herramientas
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedCategory.titleRes)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    NavigationBarItem(
                        selected        = isSelected,
                        onClick         = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.selectedCategory.value = category
                        },
                        icon            = {
                            Icon(
                                imageVector       = category.icon,
                                contentDescription = stringResource(category.titleRes)
                            )
                        },
                        alwaysShowLabel = false,
                        colors          = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        // Filtramos las herramientas por categoría
        val filteredTools = tools.filter { it.category == selectedCategory }

        LazyColumn(
            contentPadding = PaddingValues(
                top    = innerPadding.calculateTopPadding(),  // Esto garantiza que el contenido no se sobreponga
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 15.dp)
        )

        {
            items(filteredTools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToolClick(tool)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor   = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(tool.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
