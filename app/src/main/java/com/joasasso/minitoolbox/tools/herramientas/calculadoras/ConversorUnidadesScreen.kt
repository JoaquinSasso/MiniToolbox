package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

private val UnitCategory.icon: ImageVector
    get() = when (this) {
        UnitCategory.Length -> Icons.Outlined.Straighten
        UnitCategory.Weight -> Icons.Outlined.FitnessCenter
        UnitCategory.Temperature -> Icons.Outlined.Thermostat
        UnitCategory.Time -> Icons.Outlined.Schedule
    }

@get:StringRes
private val UnitCategory.hintRes: Int
    get() = when (this) {
        UnitCategory.Length -> R.string.conversor_hint_length
        UnitCategory.Weight -> R.string.conversor_hint_weight
        UnitCategory.Temperature -> R.string.conversor_hint_temperature
        UnitCategory.Time -> R.string.conversor_hint_time
    }

/** Teclas que el teclado decimal del sistema no tiene, según la categoría. */
private sealed interface ExtraKey {
    data class Insert(val label: String, val token: String, @StringRes val descriptionRes: Int) : ExtraKey
    data object ToggleSign : ExtraKey
}

private fun extraKeysFor(category: UnitCategory): List<ExtraKey> = when (category) {
    UnitCategory.Length -> listOf(
        ExtraKey.Insert("ft '", "'", R.string.conversor_key_feet),
        ExtraKey.Insert("in \"", "\"", R.string.conversor_key_inches)
    )
    UnitCategory.Weight -> listOf(
        ExtraKey.Insert("lb", " lb ", R.string.conversor_key_pounds),
        ExtraKey.Insert("oz", " oz", R.string.conversor_key_ounces)
    )
    UnitCategory.Temperature -> listOf(ExtraKey.ToggleSign)
    UnitCategory.Time -> emptyList()
}

@Composable
private fun InputError.message(): String = when (kind) {
    InputErrorKind.InvalidFormat -> stringResource(R.string.conversor_error_invalid_format)
    InputErrorKind.InvalidNumber -> stringResource(R.string.conversor_error_invalid_number)
    InputErrorKind.UnknownUnit -> stringResource(R.string.conversor_error_unknown_unit, detail.orEmpty())
    InputErrorKind.AmbiguousQuotes -> stringResource(R.string.conversor_error_ambiguous_quotes)
    InputErrorKind.InchesOutOfRange -> stringResource(R.string.conversor_error_inches_range)
    InputErrorKind.OuncesOutOfRange -> stringResource(R.string.conversor_error_ounces_range)
    InputErrorKind.FractionalFeet -> stringResource(R.string.conversor_error_fractional_feet)
    InputErrorKind.FractionalPounds -> stringResource(R.string.conversor_error_fractional_pounds)
    InputErrorKind.UnsupportedCombination -> stringResource(R.string.conversor_error_unsupported)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversorUnidadesScreen(
    onBack: () -> Unit,
    viewModel: ConversorUnidadesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.unidad_resultado_copiado)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversorUnidadesEvent.CopyToClipboard ->
                    clipboardManager.setText(AnnotatedString(event.text))
                ConversorUnidadesEvent.ShowCopiedMessage ->
                    snackbarHostState.showSnackbar(copiedMessage, duration = SnackbarDuration.Short)
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
        ) {
            CategoryTabs(
                selected = state.category,
                onSelect = { category ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onCategorySelected(category)
                }
            )

            ValueField(
                input = state.input,
                inputRevision = state.inputRevision,
                category = state.category,
                error = state.error,
                onInputChange = viewModel::onInputChanged,
                onClear = viewModel::onClearInput,
                onSuggestionAccepted = viewModel::onSuggestionAccepted,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )

            SourceUnitRow(
                units = UnitConverterEngine.getUnitsForCategory(state.category),
                selected = state.sourceUnit,
                onSelect = { unit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onSourceUnitSelected(unit)
                }
            )

            ResultsList(
                category = state.category,
                sourceEcho = state.sourceEcho,
                results = state.results,
                stale = state.resultsAreStale,
                onSelect = { unit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onResultSelected(unit)
                },
                onCopy = { unit ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onCopyResult(unit)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (state.showInfoDialog) {
        ConverterInfoDialog(onDismiss = { viewModel.onShowInfoDialog(false) })
    }
}

/**
 * Pestañas de categoría. El contenido es propio en lugar del slot `text` de [Tab] porque ese
 * slot reserva 16 dp por lado y "Temperatura" no entra en un cuarto de pantalla.
 */
@Composable
private fun CategoryTabs(selected: UnitCategory, onSelect: (UnitCategory) -> Unit) {
    val categories = UnitCategory.entries
    PrimaryTabRow(selectedTabIndex = categories.indexOf(selected)) {
        categories.forEach { category ->
            Tab(
                selected = category == selected,
                onClick = { onSelect(category) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(category.icon, contentDescription = null)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ValueField(
    input: String,
    inputRevision: Int,
    category: UnitCategory,
    error: InputError?,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onSuggestionAccepted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // El campo guarda el cursor; el ViewModel, el texto. Solo se pisa el campo cuando el
    // ViewModel cambió el texto por su cuenta (sube inputRevision): comparar contra `input`
    // en cada cambio pisaría teclas escritas antes de que llegue el estado nuevo.
    var fieldValue by remember { mutableStateOf(TextFieldValue(input, TextRange(input.length))) }
    LaunchedEffect(inputRevision) {
        if (fieldValue.text != input) fieldValue = TextFieldValue(input, TextRange(input.length))
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    fun applyEdit(edit: ConverterInputEditing.Edit) {
        fieldValue = TextFieldValue(edit.text, TextRange(edit.cursor))
        onInputChange(edit.text)
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            val textChanged = newValue.text != fieldValue.text
            fieldValue = newValue
            if (textChanged) onInputChange(newValue.text)
        },
        label = { Text(stringResource(R.string.unidad_valor)) },
        placeholder = { Text(stringResource(category.hintRes)) },
        textStyle = MaterialTheme.typography.headlineSmall,
        singleLine = true,
        isError = error != null,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                extraKeysFor(category).forEach { key ->
                    ExtraKeyButton(
                        key = key,
                        onClick = {
                            val edit = when (key) {
                                is ExtraKey.Insert -> ConverterInputEditing.insert(
                                    fieldValue.text, fieldValue.selection.start, fieldValue.selection.end, key.token
                                )
                                ExtraKey.ToggleSign -> ConverterInputEditing.toggleSign(fieldValue.text)
                            }
                            applyEdit(edit)
                        }
                    )
                }
                // Siempre presente (deshabilitado si no hay texto): si apareciera recién al escribir,
                // correría las teclas de la izquierda justo cuando el usuario va a tocarlas.
                IconButton(onClick = onClear, enabled = input.isNotEmpty()) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.conversor_limpiar))
                }
            }
        },
        supportingText = error?.let {
            {
                Column {
                    Text(it.message())
                    it.suggestionText?.let { suggestion ->
                        AssistChip(
                            onClick = onSuggestionAccepted,
                            label = { Text(stringResource(R.string.conversor_suggestion, suggestion)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}

@Composable
private fun ExtraKeyButton(key: ExtraKey, onClick: () -> Unit) {
    val (label, description) = when (key) {
        is ExtraKey.Insert -> key.label to stringResource(key.descriptionRes)
        ExtraKey.ToggleSign -> "±" to stringResource(R.string.conversor_key_sign)
    }
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp),
        modifier = Modifier
            .padding(end = 4.dp)
            .height(40.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description }
        )
    }
}

@Composable
private fun SourceUnitRow(
    units: List<ConversionUnit>,
    selected: ConversionUnit,
    onSelect: (ConversionUnit) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedIndex = units.indexOf(selected)
    // Solo se desplaza si el chip elegido (por ejemplo, detectado desde el texto) quedó fuera
    // de la vista; así al abrir la pantalla se ven las primeras unidades.
    LaunchedEffect(selected) {
        if (selectedIndex < 0) return@LaunchedEffect
        val layout = listState.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val fullyVisible = item != null &&
            item.offset >= layout.viewportStartOffset &&
            item.offset + item.size <= layout.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.conversor_de_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(units, key = { "source_${it.id}" }) { unit ->
                FilterChip(
                    selected = unit == selected,
                    onClick = { onSelect(unit) },
                    label = { Text(unit.symbol) }
                )
            }
        }
    }
}

@Composable
private fun ResultsList(
    category: UnitCategory,
    sourceEcho: String?,
    results: List<ConversionRow>,
    stale: Boolean,
    onSelect: (ConversionUnit) -> Unit,
    onCopy: (ConversionUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) {
        EmptyResults(category = category, modifier = modifier)
        return
    }

    val primary = results.first()
    val others = results.drop(1)
    val contentAlpha by animateFloatAsState(if (stale) 0.38f else 1f, label = "staleAlpha")

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
    ) {
        item(key = "primary") {
            PrimaryResultCard(
                row = primary,
                sourceEcho = sourceEcho,
                enabled = !stale,
                onSelect = { onSelect(primary.unit) },
                onCopy = { onCopy(primary.unit) }
            )
            Spacer(Modifier.height(12.dp))
        }
        itemsIndexed(others, key = { _, row -> "other_${row.unit.id}" }) { index, row ->
            ResultRow(
                row = row,
                shape = groupedShape(index, others.size),
                enabled = !stale,
                onSelect = { onSelect(row.unit) },
                onCopy = { onCopy(row.unit) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

/**
 * Forma de una fila dentro del grupo de resultados secundarios: solo la primera y la última
 * redondean sus esquinas exteriores, para que el grupo se lea como una sola tarjeta.
 */
private fun groupedShape(index: Int, count: Int): Shape {
    val outer = 16.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner
    )
}

@Composable
private fun EmptyResults(category: UnitCategory, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(category.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.conversor_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Texto plano de una fila: las unidades compuestas ya traen sus símbolos (`5'10.9"`). */
private fun ConversionRow.valueWithSymbol(): String =
    if (unit.compound != null) display else "$display ${unit.symbol}"

/** El número con peso completo y el símbolo más liviano, para que la cifra se lea primero. */
private fun ConversionRow.styledValue(symbolStyle: SpanStyle): AnnotatedString = buildAnnotatedString {
    append(display)
    if (unit.compound == null) {
        append(" ")
        withStyle(symbolStyle) { append(unit.symbol) }
    }
}

@Composable
private fun PrimaryResultCard(
    row: ConversionRow,
    sourceEcho: String?,
    enabled: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit
) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClickLabel = stringResource(R.string.conversor_use_as_input),
                onClick = onSelect
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .animateContentSize()
                .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                sourceEcho?.let {
                    Text(
                        text = stringResource(R.string.conversor_echo, it),
                        style = MaterialTheme.typography.titleMedium,
                        color = onContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = row.styledValue(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                            fontWeight = FontWeight.Normal
                        )
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(row.unit.nameRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = onContainer.copy(alpha = 0.8f)
                )
            }
            CopyButton(row = row, enabled = enabled, onCopy = onCopy)
        }
    }
}

@Composable
private fun ResultRow(
    row: ConversionRow,
    shape: Shape,
    enabled: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClickLabel = stringResource(R.string.conversor_use_as_input),
                    onClick = onSelect
                )
                .padding(start = 20.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
        ) {
            Text(
                text = row.styledValue(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(row.unit.nameRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            CopyButton(row = row, enabled = enabled, onCopy = onCopy)
        }
    }
}

@Composable
private fun CopyButton(row: ConversionRow, enabled: Boolean, onCopy: () -> Unit) {
    IconButton(onClick = onCopy, enabled = enabled) {
        Icon(
            Icons.Filled.ContentCopy,
            contentDescription = stringResource(R.string.conversor_copy_cd, row.valueWithSymbol()),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ConverterInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unidad_info_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.conversor_info_1))
                Text(stringResource(R.string.conversor_info_2))
                Text(stringResource(R.string.conversor_info_3))
                Text(stringResource(R.string.conversor_info_4))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
