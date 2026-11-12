package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.AguaRepository
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.KEY_CANTIDADES_RAPIDAS
import com.joasasso.minitoolbox.data.KEY_FRECUENCIA_MIN
import com.joasasso.minitoolbox.data.KEY_NOTIF_ACTIVAS
import com.joasasso.minitoolbox.data.KEY_OBJETIVO
import com.joasasso.minitoolbox.data.KEY_POR_VASO
import com.joasasso.minitoolbox.data.keyFecha
import com.joasasso.minitoolbox.data.parsearCantidadesRapidas
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

data class AguaUiState(
    val totalAgua: Int = 0,
    val objetivoML: Int = 2000,
    val mlPorVaso: Int = 250,
    val notificacionesActivas: Boolean = false,
    val frecuenciaMinutos: Int = 30,
    val cantidadesRapidas: List<Int> = emptyList(),
    val fecha: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false
)

sealed interface AguaEvent {
    data object ShowZeroWarning : AguaEvent
    data class WaterAdded(val cantidad: Int) : AguaEvent
    data class WaterRemoved(val cantidad: Int) : AguaEvent
    /** [anterior] permite deshacer el reinicio desde el snackbar. */
    data class WaterReset(val anterior: Int) : AguaEvent
}

class AguaViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AguaRepository = DefaultAguaRepository,
    private val dateFlowOverride: Flow<LocalDate>? = null
) : AndroidViewModel(application) {

    private val _eventChannel = Channel<AguaEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private val _currentDateFlow = MutableStateFlow(LocalDate.now())
    private val activeDateFlow: Flow<LocalDate> = dateFlowOverride ?: _currentDateFlow

    fun scheduleMidnightCheck() {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delayMs = Duration.between(now, midnight).toMillis().coerceAtLeast(500L) + 50L
            delay(delayMs)
            verificarFecha()
            scheduleMidnightCheck()
        }
    }

    val uiState: StateFlow<AguaUiState> = combine(
        activeDateFlow,
        repository.flujoAguaPreferences(application)
    ) { fecha, prefs ->
        AguaUiState(
            totalAgua = prefs[keyFecha(fecha)] ?: 0,
            objetivoML = prefs[KEY_OBJETIVO] ?: 2000,
            mlPorVaso = prefs[KEY_POR_VASO] ?: 250,
            notificacionesActivas = prefs[KEY_NOTIF_ACTIVAS] == 1,
            frecuenciaMinutos = prefs[KEY_FRECUENCIA_MIN] ?: 30,
            cantidadesRapidas = parsearCantidadesRapidas(prefs[KEY_CANTIDADES_RAPIDAS]),
            fecha = fecha,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AguaUiState(isLoading = true)
    )

    fun verificarFecha() {
        val today = LocalDate.now()
        if (today != _currentDateFlow.value) {
            _currentDateFlow.value = today
        }
    }

    private suspend fun obtenerFechaActiva(): LocalDate {
        return if (dateFlowOverride != null) {
            activeDateFlow.first()
        } else {
            verificarFecha()
            _currentDateFlow.value
        }
    }

    fun agregarAgua(cantidad: Int) {
        viewModelScope.launch {
            val fecha = obtenerFechaActiva()
            val (anterior, nuevo) = repository.incrementarAguaFecha(getApplication(), fecha, cantidad)
            if (anterior == 0 && cantidad < 0) {
                _eventChannel.send(AguaEvent.ShowZeroWarning)
                return@launch
            }
            sincronizarTrasCambioDeConsumo()

            // El repositorio no baja de 0: con 100 ml y -250 se quitan 100, y eso es lo que deshace el snackbar
            val real = nuevo - anterior
            val event = if (real > 0) {
                AguaEvent.WaterAdded(real)
            } else {
                AguaEvent.WaterRemoved(-real)
            }
            _eventChannel.send(event)
        }
    }

    /**
     * Revierte un [agregarAgua] desde el snackbar. No emite evento: el usuario ya ve el cambio
     * en el indicador y otro snackbar encima del que acaba de tocar sería ruido.
     */
    fun deshacerCambio(cantidad: Int) {
        viewModelScope.launch {
            val fecha = obtenerFechaActiva()
            repository.incrementarAguaFecha(getApplication(), fecha, -cantidad)
            sincronizarTrasCambioDeConsumo()
        }
    }

    fun resetear() {
        viewModelScope.launch {
            val fecha = obtenerFechaActiva()
            val anterior = repository.flujoAguaFecha(getApplication(), fecha).first()
            repository.guardarAguaFecha(getApplication(), fecha, 0)
            sincronizarTrasCambioDeConsumo()
            _eventChannel.send(AguaEvent.WaterReset(anterior))
        }
    }

    /** Restaura el consumo que había antes de [resetear]. */
    fun deshacerReset(anterior: Int) {
        viewModelScope.launch {
            val fecha = obtenerFechaActiva()
            repository.guardarAguaFecha(getApplication(), fecha, anterior.coerceAtLeast(0))
            sincronizarTrasCambioDeConsumo()
        }
    }

    /** La notificación visible queda desactualizada, el widget también, y el próximo recordatorio se corre. */
    private suspend fun sincronizarTrasCambioDeConsumo() {
        cancelarNotificacionAguaActiva(getApplication())
        actualizarWidgetAguaSuspend(getApplication())
        val prefs = repository.flujoAguaPreferences(getApplication()).first()
        if (prefs[KEY_NOTIF_ACTIVAS] == 1) {
            val frecuencia = prefs[KEY_FRECUENCIA_MIN] ?: 30
            programarRecordatorioAgua(getApplication(), frecuencia)
        }
    }

    fun guardarObjetivo(objetivo: Int) {
        if (objetivo == uiState.value.objetivoML) return
        viewModelScope.launch {
            repository.guardarObjetivo(getApplication(), objetivo)
            sincronizarTrasCambioDeConsumo()
        }
    }

    fun guardarPorVaso(ml: Int) {
        if (ml == uiState.value.mlPorVaso) return
        viewModelScope.launch {
            repository.guardarPorVaso(getApplication(), ml)
            actualizarWidgetAguaSuspend(getApplication())
        }
    }

    fun toggleNotificaciones(activas: Boolean) {
        viewModelScope.launch {
            repository.guardarNotificacionesActivas(getApplication(), activas)
            if (activas) {
                val prefs = repository.flujoAguaPreferences(getApplication()).first()
                val frecuencia = prefs[KEY_FRECUENCIA_MIN] ?: 30
                programarRecordatorioAgua(getApplication(), frecuencia)
            } else {
                cancelarRecordatorioAgua(getApplication())
            }
        }
    }

    fun guardarFrecuenciaMinutos(minutos: Int) {
        if (minutos == uiState.value.frecuenciaMinutos) return
        viewModelScope.launch {
            repository.guardarFrecuenciaMinutos(getApplication(), minutos)
            val prefs = repository.flujoAguaPreferences(getApplication()).first()
            if (prefs[KEY_NOTIF_ACTIVAS] == 1) {
                programarRecordatorioAgua(getApplication(), minutos)
            }
        }
    }

    fun guardarCantidadesRapidas(cantidades: List<Int>) {
        viewModelScope.launch {
            repository.guardarCantidadesRapidas(getApplication(), cantidades)
        }
    }
}
