package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Gasto
import com.joasasso.minitoolbox.data.ReunionesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class GastoFormUiState(
    val descripcion: String = "",
    val aportes: Map<String, String> = emptyMap(),
    val consumidores: Map<String, Int> = emptyMap(),
    val integrantes: List<String> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false
) {
    val montoTotalCentavos: Long
        get() = aportes.values.sumOf { DebtEngine.parseTextToCents(it) ?: 0L }

    val montoTotal: Double
        get() = montoTotalCentavos / 100.0
}

sealed interface GastoFormEvent {
    data object SaveSuccess : GastoFormEvent
    data class ShowError(@StringRes val messageResId: Int) : GastoFormEvent
}

class GastoFormViewModel @JvmOverloads constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: ReunionesRepository = ReunionesRepository
) : AndroidViewModel(application) {

    val reunionId: String = savedStateHandle.get<String>("reunionId")?.trim().orEmpty()
    val gastoId: String? = savedStateHandle.get<String>("gastoId")?.trim()?.takeIf { it.isNotBlank() }
    val isEditMode: Boolean = gastoId != null

    private val _uiState = MutableStateFlow(GastoFormUiState(isEditMode = isEditMode))
    val uiState: StateFlow<GastoFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<GastoFormEvent>(Channel.BUFFERED)
    val events: Flow<GastoFormEvent> = _events.receiveAsFlow()

    private var initialized = false

    init {
        viewModelScope.launch {
            repository.flujoReuniones(application).collect { reuniones ->
                val reunion = reuniones.find { it.id == reunionId } ?: return@collect
                if (!initialized) {
                    initialized = true
                    if (isEditMode && gastoId != null) {
                        val gasto = reunion.gastos.find { it.id == gastoId }
                        if (gasto != null) {
                            val initialAportes = gasto.obtenerAportesCentavos().mapValues { (_, cents) ->
                                if (cents % 100L == 0L) (cents / 100L).toString() else String.format(Locale.US, "%.2f", cents / 100.0)
                            }
                            val initialConsumidores = reunion.integrantes.associateWith { nombre ->
                                gasto.consumidoPor[nombre] ?: 0
                            }
                            _uiState.update {
                                it.copy(
                                    descripcion = gasto.descripcion,
                                    aportes = initialAportes,
                                    consumidores = initialConsumidores,
                                    integrantes = reunion.integrantes,
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    } else {
                        val initialConsumidores = reunion.integrantes.associateWith { 1 }
                        _uiState.update {
                            it.copy(
                                consumidores = initialConsumidores,
                                integrantes = reunion.integrantes,
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(integrantes = reunion.integrantes) }
                }
            }
        }
    }

    fun onDescripcionChange(nuevaDescripcion: String) {
        _uiState.update { it.copy(descripcion = nuevaDescripcion) }
    }

    fun onAporteChange(integrante: String, nuevoValor: String) {
        _uiState.update {
            val nuevosAportes = it.aportes.toMutableMap().apply {
                put(integrante, nuevoValor)
            }
            it.copy(aportes = nuevosAportes)
        }
    }

    fun onConsumidorToggle(integrante: String, activo: Boolean) {
        _uiState.update {
            val nuevosConsumidores = it.consumidores.toMutableMap().apply {
                put(integrante, if (activo) 1 else 0)
            }
            it.copy(consumidores = nuevosConsumidores)
        }
    }

    fun guardarGasto() {
        val currentState = _uiState.value
        if (currentState.isSubmitting) return

        val descripcion = currentState.descripcion.trim()
        if (descripcion.isBlank()) {
            _events.trySend(GastoFormEvent.ShowError(R.string.expense_name_required))
            return
        }

        val aportesCentavosValidos = currentState.aportes
            .mapValues { DebtEngine.parseTextToCents(it.value) }
            .filterValues { it != null && it > 0L }
            .mapValues { it.value!! }

        if (aportesCentavosValidos.values.sum() <= 0L) {
            _events.trySend(GastoFormEvent.ShowError(R.string.expense_amount_required))
            return
        }

        val consumidoresFinales = currentState.consumidores.filterValues { it > 0 }
        if (consumidoresFinales.isEmpty()) {
            _events.trySend(GastoFormEvent.ShowError(R.string.expense_consumers_required))
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val id = if (isEditMode && gastoId != null) gastoId else UUID.randomUUID().toString()
            val nuevoGasto = Gasto(
                id = id,
                descripcion = descripcion,
                aportesIndividuales = aportesCentavosValidos.mapValues { it.value / 100.0 },
                aportesCentavos = aportesCentavosValidos,
                consumidoPor = consumidoresFinales
            )

            repository.guardarGasto(getApplication(), reunionId, nuevoGasto)
            _events.send(GastoFormEvent.SaveSuccess)
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }
}
