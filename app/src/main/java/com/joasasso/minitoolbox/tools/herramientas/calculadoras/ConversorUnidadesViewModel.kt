package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface ConversorUnidadesEvent {
    data class CopyToClipboard(val text: String) : ConversorUnidadesEvent
    data object ShowCopiedMessage : ConversorUnidadesEvent
}

/**
 * Una fila de resultados. [display] lleva separador de miles; [copyText] no, para que se
 * pueda pegar en otra app o volver a escribir en el conversor.
 */
data class ConversionRow(
    val unit: ConversionUnit,
    val display: String,
    val copyText: String
)

/** Error de la entrada, con la interpretación sugerida ya formateada si existe. */
data class InputError(
    val kind: InputErrorKind,
    val detail: String? = null,
    val suggestionText: String? = null
)

data class ConversorUnidadesUiState(
    val category: UnitCategory = UnitCategory.Length,
    val input: String = "",
    /**
     * Sube cada vez que el ViewModel reemplaza el texto por su cuenta (borrar, tocar una fila,
     * aceptar una sugerencia). La UI lo usa para saber cuándo pisar el campo y mover el cursor.
     */
    val inputRevision: Int = 0,
    val sourceUnit: ConversionUnit = UnitConverterEngine.defaultUnitFor(UnitCategory.Length),
    /** El valor tal como se interpretó (`180 cm`, `5'11"`), para confirmarlo junto al resultado. */
    val sourceEcho: String? = null,
    val results: List<ConversionRow> = emptyList(),
    /** true cuando la entrada actual es inválida y [results] corresponde al último valor válido. */
    val resultsAreStale: Boolean = false,
    val error: InputError? = null,
    val showInfoDialog: Boolean = false
)

class ConversorUnidadesViewModel(
    private val localeProvider: () -> Locale = { Locale.getDefault() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversorUnidadesUiState())
    val uiState: StateFlow<ConversorUnidadesUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConversorUnidadesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Último valor válido; es el que se usa al tocar una fila. */
    private var lastValid: Quantity? = null
    private var inputHasExplicitUnit = false
    private var pendingSuggestion: Quantity? = null

    fun onInputChanged(raw: String) {
        val text = raw.take(MAX_INPUT_LENGTH)
        _uiState.update {
            it.copy(input = text, inputRevision = if (text != raw) it.inputRevision + 1 else it.inputRevision)
        }
        recalculate()
    }

    /**
     * Cambia de categoría. Un número solo se conserva y se reinterpreta en la unidad por
     * defecto de la nueva categoría; un texto con unidad propia (`180 cm`) o inválido se borra,
     * porque si no la unidad del texto volvería a imponer la categoría anterior.
     */
    fun onCategorySelected(category: UnitCategory) {
        val current = _uiState.value
        if (current.category == category) return
        val keepInput = !inputHasExplicitUnit && current.error == null
        _uiState.update {
            it.copy(
                category = category,
                sourceUnit = UnitConverterEngine.defaultUnitFor(category),
                input = if (keepInput) it.input else "",
                inputRevision = if (keepInput) it.inputRevision else it.inputRevision + 1
            )
        }
        recalculate()
    }

    /** Elige la unidad de origen. Si el texto traía otra unidad escrita, se borra. */
    fun onSourceUnitSelected(unit: ConversionUnit) {
        val current = _uiState.value
        if (current.sourceUnit == unit) return
        _uiState.update {
            it.copy(
                category = unit.category,
                sourceUnit = unit,
                input = if (inputHasExplicitUnit) "" else it.input,
                inputRevision = if (inputHasExplicitUnit) it.inputRevision + 1 else it.inputRevision
            )
        }
        recalculate()
    }

    /** Toca una fila: esa unidad pasa a ser el origen, con el valor ya convertido. */
    fun onResultSelected(unit: ConversionUnit) {
        val quantity = lastValid ?: return
        if (_uiState.value.resultsAreStale) return
        val value = UnitConverterEngine.convert(quantity.value, quantity.unit, unit)
        applyQuantity(Quantity(value, unit))
    }

    fun onSuggestionAccepted() {
        val suggestion = pendingSuggestion ?: return
        applyQuantity(suggestion)
    }

    fun onClearInput() {
        _uiState.update { it.copy(input = "", inputRevision = it.inputRevision + 1) }
        recalculate()
    }

    fun onCopyResult(unit: ConversionUnit) {
        val row = _uiState.value.results.firstOrNull { it.unit == unit } ?: return
        viewModelScope.launch {
            _events.send(ConversorUnidadesEvent.CopyToClipboard(row.copyText))
            _events.send(ConversorUnidadesEvent.ShowCopiedMessage)
        }
    }

    fun onShowInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showInfoDialog = show) }
    }

    private fun applyQuantity(quantity: Quantity) {
        val text = UnitConverterEngine.formatValue(quantity.value, quantity.unit, localeProvider(), grouping = false)
        _uiState.update {
            it.copy(
                category = quantity.unit.category,
                sourceUnit = quantity.unit,
                input = text,
                inputRevision = it.inputRevision + 1
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        pendingSuggestion = null
        when (val parsed = MeasurementParser.parse(state.input, state.sourceUnit)) {
            ParsedInput.Empty -> {
                lastValid = null
                inputHasExplicitUnit = false
                _uiState.update {
                    it.copy(results = emptyList(), sourceEcho = null, resultsAreStale = false, error = null)
                }
            }

            is ParsedInput.Valid -> {
                val quantity = parsed.quantity
                lastValid = quantity
                inputHasExplicitUnit = parsed.explicitUnit
                _uiState.update {
                    it.copy(
                        category = quantity.unit.category,
                        sourceUnit = quantity.unit,
                        sourceEcho = withSymbol(
                            UnitConverterEngine.formatValue(quantity.value, quantity.unit, localeProvider()),
                            quantity.unit
                        ),
                        results = buildRows(quantity),
                        resultsAreStale = false,
                        error = null
                    )
                }
            }

            is ParsedInput.Invalid -> {
                pendingSuggestion = parsed.suggestion
                val suggestionText = parsed.suggestion?.let {
                    UnitConverterEngine.formatValue(it.value, it.unit, localeProvider(), grouping = false)
                }
                _uiState.update {
                    it.copy(
                        resultsAreStale = it.results.isNotEmpty(),
                        error = InputError(parsed.kind, parsed.detail, suggestionText)
                    )
                }
            }
        }
    }

    private fun buildRows(quantity: Quantity): List<ConversionRow> {
        val locale = localeProvider()
        return UnitConverterEngine.targetsFor(quantity.unit).map { target ->
            val value = UnitConverterEngine.convert(quantity.value, quantity.unit, target)
            ConversionRow(
                unit = target,
                display = UnitConverterEngine.formatValue(value, target, locale),
                copyText = UnitConverterEngine.formatValue(value, target, locale, grouping = false)
            )
        }
    }

    private fun withSymbol(text: String, unit: ConversionUnit): String =
        if (unit.compound != null) text else "$text ${unit.symbol}"

    private companion object {
        const val MAX_INPUT_LENGTH = 40
    }
}
