package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReunionDetailUiState(
    val reunion: Reunion? = null,
    val deudas: List<String> = emptyList(),
    val textoCompartir: String = "",
    val isLoading: Boolean = false
)

class ReunionDetailViewModel @JvmOverloads constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val repository: ReunionesRepository = ReunionesRepository
) : AndroidViewModel(application) {

    val reunionId: String = savedStateHandle.get<String>("reunionId")?.trim().orEmpty()

    val uiState: StateFlow<ReunionDetailUiState> = repository.flujoReuniones(application)
        .map { reuniones ->
            val r = reuniones.find { it.id == reunionId }
            if (r != null) {
                ReunionDetailUiState(
                    reunion = r,
                    deudas = DebtEngine.calcularDeudas(r, application),
                    textoCompartir = generarTextoCompartible(r, application),
                    isLoading = false
                )
            } else {
                ReunionDetailUiState(reunion = null, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReunionDetailUiState(isLoading = true)
        )

    fun actualizarIntegrante(original: String, nuevo: String) {
        if (reunionId.isBlank()) return
        viewModelScope.launch {
            repository.actualizarIntegrante(getApplication(), reunionId, original, nuevo)
        }
    }

    fun eliminarGasto(gastoId: String) {
        if (reunionId.isBlank()) return
        viewModelScope.launch {
            repository.eliminarGasto(getApplication(), reunionId, gastoId)
        }
    }

    fun eliminarIntegrante(nombre: String) {
        if (reunionId.isBlank()) return
        viewModelScope.launch {
            repository.eliminarIntegrante(getApplication(), reunionId, nombre)
        }
    }

    fun agregarIntegrante(nuevo: String) {
        if (reunionId.isBlank() || nuevo.isBlank()) return
        viewModelScope.launch {
            repository.agregarIntegrante(getApplication(), reunionId, nuevo)
        }
    }
}
