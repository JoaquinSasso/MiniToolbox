package com.joasasso.minitoolbox.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AguaRepository {
    fun flujoAguaHoy(context: Context, dateFlow: Flow<LocalDate> = flujoFechaActual()): Flow<Int>
    fun flujoAguaFecha(context: Context, fecha: LocalDate): Flow<Int>
    fun flujoObjetivo(context: Context): Flow<Int>
    fun flujoPorVaso(context: Context): Flow<Int>
    fun flujoNotificacionesActivas(context: Context): Flow<Boolean>
    fun flujoFrecuenciaMinutos(context: Context): Flow<Int>

    suspend fun guardarAguaHoy(context: Context, valor: Int)
    suspend fun guardarAguaFecha(context: Context, fecha: LocalDate, valor: Int)
    suspend fun guardarObjetivo(context: Context, valor: Int)
    suspend fun guardarPorVaso(context: Context, valor: Int)
    suspend fun guardarNotificacionesActivas(context: Context, activo: Boolean)
    suspend fun guardarFrecuenciaMinutos(context: Context, min: Int)
}

object DefaultAguaRepository : AguaRepository {
    override fun flujoAguaHoy(context: Context, dateFlow: Flow<LocalDate>): Flow<Int> =
        context.flujoAguaHoy(dateFlow)

    override fun flujoAguaFecha(context: Context, fecha: LocalDate): Flow<Int> =
        context.flujoAguaFecha(fecha)

    override fun flujoObjetivo(context: Context): Flow<Int> =
        context.flujoObjetivo()

    override fun flujoPorVaso(context: Context): Flow<Int> =
        context.flujoPorVaso()

    override fun flujoNotificacionesActivas(context: Context): Flow<Boolean> =
        context.flujoNotificacionesActivas()

    override fun flujoFrecuenciaMinutos(context: Context): Flow<Int> =
        context.flujoFrecuenciaMinutos()

    override suspend fun guardarAguaHoy(context: Context, valor: Int) =
        context.guardarAguaHoy(valor)

    override suspend fun guardarAguaFecha(context: Context, fecha: LocalDate, valor: Int) =
        context.guardarAguaFecha(fecha, valor)

    override suspend fun guardarObjetivo(context: Context, valor: Int) =
        context.guardarObjetivo(valor)

    override suspend fun guardarPorVaso(context: Context, valor: Int) =
        context.guardarPorVaso(valor)

    override suspend fun guardarNotificacionesActivas(context: Context, activo: Boolean) =
        context.guardarNotificacionesActivas(activo)

    override suspend fun guardarFrecuenciaMinutos(context: Context, min: Int) =
        context.guardarFrecuenciaMinutos(min)
}
