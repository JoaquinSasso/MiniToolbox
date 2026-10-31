package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

/**
 * Ícono representativo por categoría de conversión.
 */
val UnitCategory.icon: ImageVector
    get() = when (this) {
        UnitCategory.Length -> Icons.Outlined.Straighten
        UnitCategory.Weight -> Icons.Outlined.FitnessCenter
        UnitCategory.Temperature -> Icons.Outlined.Thermostat
        UnitCategory.Time -> Icons.Outlined.Schedule
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversorUnidadesScreen(
    onBack: () -> Unit,
    viewModel: ConversorUnidadesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.unidad_resultado_copiado)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversorUnidadesEvent.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(event.text))
                }
                is ConversorUnidadesEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = copiedMessage,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_unit_converter),
                onBack = onBack,
                onShowInfo = { viewModel.onShowInfoDialog(true) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Selector horizontal de categoría (M3 FilterChips)
            CategorySelectorRow(
                selectedCategory = state.category,
                onSelectCategory = { cat ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onCategorySelected(cat)
                }
            )

            Spacer(Modifier.height(12.dp))

            // Contenedor principal de conversión (Cards Origen, Intercambio y Destino)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card de Origen (Input)
                OriginInputCard(
                    input = state.input,
                    onInputChange = viewModel::onInputChanged,
                    fromUnit = state.fromUnit,
                    availableUnits = UnitConverterEngine.getUnitsForCategory(state.category),
                    onSelectUnit = { unit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onFromUnitSelected(unit)
                    },
                    onClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onClearInput()
                    },
                    onQuickPreset = { preset ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onQuickPresetSelected(preset)
                    },
                    onDone = { focusManager.clearFocus() }
                )

                // Botón interactivo de intercambio con resorte animado
                SwapUnitsDivider(
                    onSwap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onSwapUnits()
                    }
                )

                // Card de Destino (Resultado en tonal primaryContainer)
                DestinationResultCard(
                    toUnit = state.toUnit,
                    availableUnits = UnitConverterEngine.getUnitsForCategory(state.category),
                    formattedResult = state.formattedResult,
                    onSelectUnit = { unit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onToUnitSelected(unit)
                    },
                    onCopy = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onCopyResult()
                    }
                )

                // Sección "Todas las conversiones"
                AllConversionsSection(
                    items = state.allConversions,
                    selectedTargetUnit = state.toUnit,
                    isReferenceMode = state.input.isEmpty(),
                    fromUnitSymbol = state.fromUnit.symbol,
                    onSelectAsTarget = { unit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onUnitSelectedFromList(unit)
                    }
                )
            }
        }
    }

    if (state.showInfoDialog) {
        ConverterInfoDialog(onDismiss = { viewModel.onShowInfoDialog(false) })
    }
}

/**
 * Fila desplazable con chips Material 3 para alternar categorías.
 */
@Composable
private fun CategorySelectorRow(
    selectedCategory: UnitCategory,
    onSelectCategory: (UnitCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UnitCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                label = {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                },
                shape = CircleShape
            )
        }
    }
}

/**
 * Card para el valor y la unidad de origen.
 */
@Composable
private fun OriginInputCard(
    input: String,
    onInputChange: (String) -> Unit,
    fromUnit: ConversionUnit,
    availableUnits: List<ConversionUnit>,
    onSelectUnit: (ConversionUnit) -> Unit,
    onClear: () -> Unit,
    onQuickPreset: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: Etiqueta "De" y selector de unidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = stringResource(R.string.conversor_de_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                UnitDropdownSelector(
                    currentUnit = fromUnit,
                    availableUnits = availableUnits,
                    onUnitSelected = onSelectUnit,
                    contentDescription = stringResource(R.string.conversor_unidad_origen_cd)
                )
            }

            // Campo de entrada de valor con tipografía display y botón de limpieza
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                },
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                trailingIcon = {
                    AnimatedVisibility(
                        visible = input.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.conversor_limpiar),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Chips rápidos de prueba/ejemplo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("1", "5", "10", "50", "100").forEach { preset ->
                    SuggestionChip(
                        onClick = { onQuickPreset(preset) },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }
    }
}

/**
 * Divisor con botón de intercambio de unidades con física de resorte.
 */
@Composable
private fun SwapUnitsDivider(
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        var swapAngle by remember { mutableFloatStateOf(0f) }
        val animatedRotation by animateFloatAsState(
            targetValue = swapAngle,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "swapRotation"
        )

        FilledTonalIconButton(
            onClick = {
                swapAngle += 180f
                onSwap()
            },
            shape = CircleShape,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = stringResource(R.string.conversor_intercambiar),
                modifier = Modifier.rotate(animatedRotation)
            )
        }
    }
}

/**
 * Card de resultado en color primario container.
 */
@Composable
private fun DestinationResultCard(
    toUnit: ConversionUnit,
    availableUnits: List<ConversionUnit>,
    formattedResult: String?,
    onSelectUnit: (ConversionUnit) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cabecera: Etiqueta "A" y selector de unidad destino
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = stringResource(R.string.conversor_a_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                UnitDropdownSelector(
                    currentUnit = toUnit,
                    availableUnits = availableUnits,
                    onUnitSelected = onSelectUnit,
                    contentDescription = stringResource(R.string.conversor_unidad_destino_cd),
                    isPrimaryContainer = true
                )
            }

            // Resultado numérico destacado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedResult ?: "0",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = toUnit.symbol,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
                )
            }

            // Botón de acción: Copiar resultado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onCopy,
                    enabled = formattedResult != null,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.conversor_copiar))
                }
            }
        }
    }
}

/**
 * Selector desplegable de unidades con diseño M3.
 */
@Composable
private fun UnitDropdownSelector(
    currentUnit: ConversionUnit,
    availableUnits: List<ConversionUnit>,
    onUnitSelected: (ConversionUnit) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    isPrimaryContainer: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = if (isPrimaryContainer) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (isPrimaryContainer) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.semantics { this.contentDescription = contentDescription }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${stringResource(currentUnit.nameRes)} (${currentUnit.symbol})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 210.dp)
        ) {
            availableUnits.forEach { unit ->
                val isSelected = unit == currentUnit
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(unit.nameRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.width(16.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = unit.symbol,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    onClick = {
                        expanded = false
                        onUnitSelected(unit)
                    }
                )
            }
        }
    }
}

/**
 * Sección que lista todas las conversiones de la categoría en tiempo real.
 */
@Composable
private fun AllConversionsSection(
    items: List<UnitConversionItem>,
    selectedTargetUnit: ConversionUnit,
    isReferenceMode: Boolean,
    fromUnitSymbol: String,
    onSelectAsTarget: (ConversionUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.conversor_todas_conversiones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isReferenceMode) {
                Text(
                    text = stringResource(R.string.conversor_equivalencia_referencia, fromUnitSymbol),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items.forEach { item ->
            val isTarget = item.unit == selectedTargetUnit
            Card(
                onClick = { onSelectAsTarget(item.unit) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isTarget) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.defaultMinSize(minWidth = 44.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = item.unit.symbol,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "${item.formattedValue} ${item.unit.symbol}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(item.unit.nameRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isTarget) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.conversor_a_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.conversor_toca_para_seleccionar),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo informativo con instrucciones y ejemplos.
 */
@Composable
private fun ConverterInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.unidad_info_titulo),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.unidad_info_linea1))
                Text(stringResource(R.string.unidad_info_linea2))
                Text(stringResource(R.string.unidad_info_linea3))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.unidad_info_linea4),
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.unidad_info_linea5))
                Text(stringResource(R.string.unidad_info_linea6))
                Text(stringResource(R.string.unidad_info_linea7))
                Text(stringResource(R.string.unidad_info_linea8))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
