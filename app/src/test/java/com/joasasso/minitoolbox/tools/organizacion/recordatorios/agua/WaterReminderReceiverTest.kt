package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.flujoAguaHoy
import com.joasasso.minitoolbox.data.guardarAguaFecha
import com.joasasso.minitoolbox.data.guardarHorarioRecordatorio
import com.joasasso.minitoolbox.data.guardarNotificacionesActivas
import com.joasasso.minitoolbox.data.guardarPausarAlCumplir
import com.joasasso.minitoolbox.data.guardarPorVaso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class WaterReminderReceiverTest {

    private lateinit var context: Context
    private lateinit var manager: NotificationManager
    private val hoy = LocalDate.of(2026, 11, 12)
    private val mediodia = LocalTime.of(12, 0)

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.aguaDataStore.edit { it.clear() }
        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
        context.guardarNotificacionesActivas(true)
    }

    private fun notificacion(): Notification? =
        shadowOf(manager).getNotification(WATER_REMINDER_NOTIFICATION_ID)

    @Test
    fun fueraDelHorario_noNotifica() = runTest {
        WaterReminderReceiver().handleReminder(context, hoy, LocalTime.of(23, 30))
        assertNull(notificacion())
    }

    @Test
    fun horarioQueCruzaMedianoche_notificaDeMadrugada() = runTest {
        context.guardarHorarioRecordatorio(22 * 60, 6 * 60)
        WaterReminderReceiver().handleReminder(context, hoy, LocalTime.of(2, 0))
        assertNotNull(notificacion())
    }

    @Test
    fun metaCumplida_conPausaPorDefecto_noNotifica() = runTest {
        context.guardarAguaFecha(hoy, 2000)
        WaterReminderReceiver().handleReminder(context, hoy, mediodia)
        assertNull(notificacion())
    }

    @Test
    fun metaCumplida_sinPausa_notificaConTextoDeMetaCumplida() = runTest {
        context.guardarAguaFecha(hoy, 2000)
        context.guardarPausarAlCumplir(false)
        WaterReminderReceiver().handleReminder(context, hoy, mediodia)
        val texto = notificacion()?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        assertEquals(
            context.getString(R.string.water_reminder_content_done, AguaFormat.litros(2000), AguaFormat.litros(2000)),
            texto
        )
    }

    @Test
    fun notificacion_traeAccionParaSumarElVaso() = runTest {
        context.guardarPorVaso(300)
        WaterReminderReceiver().handleReminder(context, hoy, mediodia)
        val acciones = notificacion()?.actions
        assertNotNull(acciones)
        assertEquals(1, acciones!!.size)
        assertEquals(context.getString(R.string.water_add, 300), acciones[0].title.toString())
    }

    @Test
    fun textoRecordatorio_muestraLoQueFaltaEnMlOEnLitros() {
        assertEquals(
            context.getString(
                R.string.water_reminder_content,
                AguaFormat.litros(1250),
                AguaFormat.litros(2000),
                context.getString(R.string.water_settings_glass_value, 750)
            ),
            textoRecordatorio(context, 1250, 2000, 250)
        )
        assertEquals(
            context.getString(
                R.string.water_reminder_content,
                AguaFormat.litros(500),
                AguaFormat.litros(2000),
                context.getString(R.string.water_liters_value, AguaFormat.litros(1500))
            ),
            textoRecordatorio(context, 500, 2000, 250)
        )
    }

    @Test
    fun cambiarAguaFueraDeLaApp_sumaYCierraLaNotificacion() = runTest {
        WaterReminderReceiver().handleReminder(context, LocalDate.now(), mediodia)
        assertNotNull(notificacion())

        cambiarAguaFueraDeLaApp(context, 250)

        assertEquals(250, context.flujoAguaHoy().first())
        assertNull(notificacion())
    }

    @Test
    fun receiverDeLaAccion_ignoraOtrasAcciones() = runTest {
        AguaNotificacionAccionReceiver().onReceive(context, Intent("otra.accion"))
        assertEquals(0, context.flujoAguaHoy().first())
    }
}
