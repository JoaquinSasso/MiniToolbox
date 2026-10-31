package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.AguaRepository
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.KEY_OBJETIVO
import com.joasasso.minitoolbox.data.KEY_POR_VASO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AguaStatisticsUiState(
    val historico: List<Pair<LocalDate, Int>> = emptyList(),
    val promedioSemana: Int = 0,
    val mejorDia: Pair<LocalDate, Int>? = null,
    val objetivoML: Int = 2000,
    val mlPorVaso: Int = 250,
    val isLoading: Boolean = false
)

class AguaStatisticsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AguaRepository = DefaultAguaRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : AndroidViewModel(application) {

    val uiState: StateFlow<AguaStatisticsUiState> = repository
        .flujoAguaPreferences(application)
        .map { prefs ->
            val rawEntries = prefs.asMap().mapKeys { (key, _) -> key.name }
            val historicoOrdenado = AguaMath.extraerHistoricoOrdenado(rawEntries)
            AguaStatisticsUiState(
                historico = historicoOrdenado,
                promedioSemana = AguaMath.calcularPromedioUltimos7Dias(historicoOrdenado),
                mejorDia = AguaMath.encontrarMejorDia(historicoOrdenado),
                objetivoML = prefs[KEY_OBJETIVO] ?: 2000,
                mlPorVaso = prefs[KEY_POR_VASO] ?: 250,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AguaStatisticsUiState(isLoading = true)
        )

    fun editarConsumoFecha(fecha: LocalDate, nuevoValor: Int) {
        val sanitized = nuevoValor.coerceAtLeast(0)
        viewModelScope.launch {
            repository.guardarAguaFecha(getApplication(), fecha, sanitized)
            if (fecha == todayProvider()) {
                cancelarNotificacionAguaActiva(getApplication())
                actualizarWidgetAguaSuspend(getApplication())
            }
        }
    }
}
