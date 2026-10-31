package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ConversorUnidadesEvent {
    data class ShowSnackbar(@StringRes val messageRes: Int) : ConversorUnidadesEvent
    data class CopyToClipboard(val text: String) : ConversorUnidadesEvent
}

data class ConversorUnidadesUiState(
    val category: UnitCategory = UnitCategory.Length,
    val input: String = "",
    val fromUnit: ConversionUnit = UnitConverterEngine.lengthUnits[0],
    val toUnit: ConversionUnit = UnitConverterEngine.lengthUnits[1],
    val formattedResult: String? = null,
    val rawResult: Double? = null,
    val allConversions: List<UnitConversionItem> = emptyList(),
    val showInfoDialog: Boolean = false
)

class ConversorUnidadesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ConversorUnidadesUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConversorUnidadesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        recalculate()
    }

    private fun createInitialState(): ConversorUnidadesUiState {
        val initialCategory = UnitCategory.Length
        val units = UnitConverterEngine.getUnitsForCategory(initialCategory)
        return ConversorUnidadesUiState(
            category = initialCategory,
            input = "",
            fromUnit = units[0],
            toUnit = units[1]
        )
    }

    fun onCategorySelected(newCategory: UnitCategory) {
        if (_uiState.value.category == newCategory) return
        val units = UnitConverterEngine.getUnitsForCategory(newCategory)
        _uiState.update { current ->
            current.copy(
                category = newCategory,
                fromUnit = units[0],
                toUnit = units[1]
            )
        }
        recalculate()
    }

    fun onInputChanged(raw: String) {
        // Filtra caracteres válidos: dígitos y a lo sumo un separador decimal (. o ,)
        val sanitized = sanitizeDecimalInput(raw)
        _uiState.update { it.copy(input = sanitized) }
        recalculate()
    }

    fun onFromUnitSelected(unit: ConversionUnit) {
        _uiState.update { it.copy(fromUnit = unit) }
        recalculate()
    }

    fun onToUnitSelected(unit: ConversionUnit) {
        _uiState.update { it.copy(toUnit = unit) }
        recalculate()
    }

    fun onSwapUnits() {
        _uiState.update { current ->
            current.copy(
                fromUnit = current.toUnit,
                toUnit = current.fromUnit
            )
        }
        recalculate()
    }

    fun onClearInput() {
        _uiState.update { it.copy(input = "") }
        recalculate()
    }

    fun onQuickPresetSelected(preset: String) {
        _uiState.update { it.copy(input = preset) }
        recalculate()
    }

    fun onUnitSelectedFromList(unit: ConversionUnit) {
        _uiState.update { it.copy(toUnit = unit) }
        recalculate()
    }

    fun onShowInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showInfoDialog = show) }
    }

    fun onCopyResult() {
        val state = _uiState.value
        val textToCopy = state.rawResult?.let { UnitConverterEngine.formatCleanRaw(it) }
            ?: state.formattedResult
            ?: return

        viewModelScope.launch {
            _events.send(ConversorUnidadesEvent.CopyToClipboard(textToCopy))
            _events.send(ConversorUnidadesEvent.ShowSnackbar(R.string.unidad_resultado_copiado))
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val parsedValue = UnitConverterEngine.parseInput(state.input)

        if (parsedValue != null) {
            val converted = UnitConverterEngine.convert(parsedValue, state.fromUnit, state.toUnit)
            val all = UnitConverterEngine.convertAll(parsedValue, state.fromUnit)
            _uiState.update {
                it.copy(
                    rawResult = converted,
                    formattedResult = UnitConverterEngine.formatNumber(converted),
                    allConversions = all
                )
            }
        } else {
            // Con entrada vacía o inválida, mostramos como referencia la conversión de 1 unidad base
            val referenceConversions = UnitConverterEngine.convertAll(1.0, state.fromUnit)
            _uiState.update {
                it.copy(
                    rawResult = null,
                    formattedResult = null,
                    allConversions = referenceConversions
                )
            }
        }
    }

    private fun sanitizeDecimalInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
        val separatorCount = filtered.count { it == '.' || it == ',' }
        if (separatorCount <= 1) return filtered

        // Preservar solo el primer separador
        var seenSeparator = false
        val sb = java.lang.StringBuilder()
        for (ch in filtered) {
            if (ch == '.' || ch == ',') {
                if (!seenSeparator) {
                    sb.append(ch)
                    seenSeparator = true
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
