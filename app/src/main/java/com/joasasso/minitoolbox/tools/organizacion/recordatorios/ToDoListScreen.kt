package com.joasasso.minitoolbox.tools.organizacion.recordatorios

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.ToDoDataStore
import com.joasasso.minitoolbox.data.ToDoItem
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun ToDoListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Pendiente, 1 = Hecho
    var showDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }

    var toDoList by remember { mutableStateOf<List<ToDoItem>>(emptyList()) }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    LaunchedEffect(Unit) {
        ToDoDataStore.getToDoList(context).collect { toDoList = it }
    }

    fun updateList(newList: List<ToDoItem>) {
        toDoList = newList
        scope.launch { ToDoDataStore.saveToDoList(context, newList) }
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_todo_list), onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tarea")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
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
                    Text("Pendientes")
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Hechas")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val itemsToShow = toDoList.filter { it.isDone == (selectedTab == 1) }
                if (itemsToShow.isEmpty()) {
                    item {
                        Text(
                            "No hay tareas",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(itemsToShow, key = { it.id }) { item ->
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val scope = rememberCoroutineScope()

                    var transitioning by remember { mutableStateOf(false) }
                    var offsetX by remember { mutableFloatStateOf(0f) }
                    var alpha by remember { mutableFloatStateOf(1f) }
                    var visible by remember { mutableStateOf(item.isDone == (selectedTab == 1)) }
                    var bgColor by remember {
                        mutableStateOf(
                            if (item.isDone) primaryContainerColor
                            else surfaceVariantColor
                        )
                    }

                    if (visible || transitioning) {
                        val animatedOffsetX by animateFloatAsState(
                            targetValue = offsetX,
                            animationSpec = tween(durationMillis = 500),
                            label = "offsetAnim"
                        )

                        val animatedAlpha by animateFloatAsState(
                            targetValue = alpha,
                            animationSpec = tween(durationMillis = 500),
                            label = "alphaAnim"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationX = animatedOffsetX
                                    this.alpha = animatedAlpha
                                }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isDone,
                                    onCheckedChange = {
                                        if (!transitioning) {
                                            transitioning = true

                                            // Cambio inmediato de color
                                            bgColor = if (!item.isDone)
                                                primaryContainerColor
                                            else
                                                surfaceVariantColor

                                            // Salida hacia la derecha si se marca como hecho, izquierda si se desmarca
                                            offsetX = if (selectedTab == 0) screenWidth.value * 2f else -screenWidth.value * 2f
                                            alpha = 0f

                                            scope.launch {
                                                delay(500)
                                                val updated = toDoList.map {
                                                    if (it.id == item.id) it.copy(isDone = !it.isDone) else it
                                                }
                                                updateList(updated)
                                                offsetX = 0f
                                                alpha = 1f
                                                transitioning = false
                                                visible = false
                                            }
                                        }
                                    }
                                )
                                Text(
                                    item.text,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (selectedTab == 1) {
                                    IconButton(onClick = {
                                        val updated = toDoList.filter { it.id != item.id }
                                        alpha = 0f
                                        scope.launch {
                                            delay(500)
                                            updateList(updated)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Nueva tarea") },
                text = {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Descripción") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newItemText.isNotBlank()) {
                            val new = ToDoItem(
                                id = toDoList.maxOfOrNull { it.id + 1 } ?: 0,
                                text = newItemText.trim(),
                                isDone = false
                            )
                            updateList(toDoList + new)
                            newItemText = ""
                            showDialog = false
                        }
                    }) {
                        Text("Agregar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        newItemText = ""
                        showDialog = false
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
