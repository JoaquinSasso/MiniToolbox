package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.AguaRepository
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.flujoFechaActual
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AguaUiState(
    val totalAgua: Int = 0,
    val objetivoML: Int = 2000,
    val mlPorVaso: Int = 250,
    val notificacionesActivas: Boolean = false,
    val frecuenciaMinutos: Int = 180,
    val fecha: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false
)

sealed interface AguaEvent {
    data object ShowZeroWarning : AguaEvent
    data class WaterAdded(val cantidad: Int) : AguaEvent
    data class WaterRemoved(val cantidad: Int) : AguaEvent
    data object WaterReset : AguaEvent
    data object NotifEnabled : AguaEvent
    data object NotifDisabled : AguaEvent
}

class AguaViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AguaRepository = DefaultAguaRepository,
    dateFlowOverride: Flow<LocalDate>? = null
) : AndroidViewModel(application) {

    private val _eventChannel = Channel<AguaEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private val _currentDateFlow = MutableStateFlow(LocalDate.now())
    private val activeDateFlow: Flow<LocalDate> = dateFlowOverride ?: combine(
        _currentDateFlow,
        flujoFechaActual()
    ) { manual, timer ->
        if (manual >= timer) manual else timer
    }

    val uiState: StateFlow<AguaUiState> = combine(
        combine(
            repository.flujoAguaHoy(application, activeDateFlow),
            repository.flujoObjetivo(application),
            repository.flujoPorVaso(application)
        ) { agua, objetivo, porVaso ->
            Triple(agua, objetivo, porVaso)
        },
        combine(
            repository.flujoNotificacionesActivas(application),
            repository.flujoFrecuenciaMinutos(application),
            activeDateFlow
        ) { notif, freq, fecha ->
            Triple(notif, freq, fecha)
        }
    ) { (agua, objetivo, porVaso), (notif, freq, fecha) ->
        AguaUiState(
            totalAgua = agua,
            objetivoML = objetivo,
            mlPorVaso = porVaso,
            notificacionesActivas = notif,
            frecuenciaMinutos = freq,
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

    fun agregarAgua(cantidad: Int) {
        val currentTotal = uiState.value.totalAgua
        if (currentTotal == 0 && cantidad < 0) {
            viewModelScope.launch {
                _eventChannel.send(AguaEvent.ShowZeroWarning)
            }
            return
        }

        val nuevo = (currentTotal + cantidad).coerceAtLeast(0)
        viewModelScope.launch {
            repository.guardarAguaHoy(getApplication(), nuevo)
            actualizarWidgetAguaSuspend(getApplication())

            val event = if (cantidad > 0) {
                AguaEvent.WaterAdded(cantidad)
            } else {
                AguaEvent.WaterRemoved(kotlin.math.abs(cantidad))
            }
            _eventChannel.send(event)

            if (uiState.value.notificacionesActivas) {
                programarRecordatorioAgua(
                    getApplication(),
                    uiState.value.frecuenciaMinutos,
                    nuevo,
                    uiState.value.objetivoML
                )
            }
        }
    }

    fun resetear() {
        viewModelScope.launch {
            repository.guardarAguaHoy(getApplication(), 0)
            actualizarWidgetAguaSuspend(getApplication())
            _eventChannel.send(AguaEvent.WaterReset)

            if (uiState.value.notificacionesActivas) {
                programarRecordatorioAgua(
                    getApplication(),
                    uiState.value.frecuenciaMinutos,
                    0,
                    uiState.value.objetivoML
                )
            }
        }
    }

    fun guardarObjetivo(objetivo: Int) {
        if (objetivo == uiState.value.objetivoML) return
        viewModelScope.launch {
            repository.guardarObjetivo(getApplication(), objetivo)
            actualizarWidgetAguaSuspend(getApplication())
            if (uiState.value.notificacionesActivas) {
                programarRecordatorioAgua(
                    getApplication(),
                    uiState.value.frecuenciaMinutos,
                    uiState.value.totalAgua,
                    objetivo
                )
            }
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
                programarRecordatorioAgua(
                    getApplication(),
                    uiState.value.frecuenciaMinutos,
                    uiState.value.totalAgua,
                    uiState.value.objetivoML
                )
                _eventChannel.send(AguaEvent.NotifEnabled)
            } else {
                cancelarRecordatorioAgua(getApplication())
                _eventChannel.send(AguaEvent.NotifDisabled)
            }
        }
    }

    fun guardarFrecuenciaMinutos(minutos: Int) {
        if (minutos == uiState.value.frecuenciaMinutos) return
        viewModelScope.launch {
            repository.guardarFrecuenciaMinutos(getApplication(), minutos)
            if (uiState.value.notificacionesActivas) {
                programarRecordatorioAgua(
                    getApplication(),
                    minutos,
                    uiState.value.totalAgua,
                    uiState.value.objetivoML
                )
            }
        }
    }
}
