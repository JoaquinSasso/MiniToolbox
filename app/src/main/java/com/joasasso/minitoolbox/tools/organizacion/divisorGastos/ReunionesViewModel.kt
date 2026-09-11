package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReunionesUiState(
    val reuniones: List<Reunion> = emptyList(),
    val isLoading: Boolean = false
)

class ReunionesViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ReunionesRepository = ReunionesRepository
) : AndroidViewModel(application) {

    val uiState: StateFlow<ReunionesUiState> = repository.flujoReuniones(application)
        .map { reuniones ->
            ReunionesUiState(
                reuniones = reuniones.sortedByDescending { it.fecha },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReunionesUiState(isLoading = true)
        )

    fun eliminarReunion(id: String) {
        viewModelScope.launch {
            repository.eliminarReunion(getApplication(), id)
        }
    }
}
