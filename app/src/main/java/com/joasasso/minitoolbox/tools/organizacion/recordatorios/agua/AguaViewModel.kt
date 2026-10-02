package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.AguaRepository
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.KEY_FRECUENCIA_MIN
import com.joasasso.minitoolbox.data.KEY_NOTIF_ACTIVAS
import com.joasasso.minitoolbox.data.KEY_OBJETIVO
import com.joasasso.minitoolbox.data.KEY_POR_VASO
import com.joasasso.minitoolbox.data.keyFecha
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        val fecha = uiState.value.fecha
        viewModelScope.launch {
            repository.guardarAguaFecha(getApplication(), fecha, nuevo)
            actualizarWidgetAgua(getApplication())

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
        val fecha = uiState.value.fecha
        viewModelScope.launch {
            repository.guardarAguaFecha(getApplication(), fecha, 0)
            actualizarWidgetAgua(getApplication())
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
            actualizarWidgetAgua(getApplication())
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
            actualizarWidgetAgua(getApplication())
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
