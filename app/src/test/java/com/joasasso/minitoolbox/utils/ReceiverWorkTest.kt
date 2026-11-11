package com.joasasso.minitoolbox.utils

import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric solo para que `android.util.Log` no falle en la JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ReceiverWorkTest {

    @Test
    fun `una excepcion se captura y el receiver se libera igual`() = runTest {
        var done = false
        val job = launchReceiverWork(
            tag = "test",
            onDone = { done = true },
            dispatcher = StandardTestDispatcher(testScheduler)
        ) { error("fallo de DataStore") }

        advanceUntilIdle()

        assertTrue("finish() tiene que correr aunque falle el trabajo", done)
        // Si la excepción escapara, el Job terminaría cancelado (y en producción el
        // proceso moriría en el handler del hilo).
        assertTrue(job.isCompleted)
        assertFalse("la excepción no puede escapar del bloque", job.isCancelled)
    }

    @Test
    fun `la cancelacion se propaga y el receiver se libera igual`() = runTest {
        var done = false
        val job = launchReceiverWork(
            tag = "test",
            onDone = { done = true },
            dispatcher = StandardTestDispatcher(testScheduler)
        ) { throw CancellationException("cancelado") }

        advanceUntilIdle()

        assertTrue(done)
        assertTrue("CancellationException se re-lanza, no se traga", job.isCancelled)
    }

    @Test
    fun `sin errores ejecuta el bloque y despues onDone`() = runTest {
        val order = mutableListOf<String>()
        launchReceiverWork(
            tag = "test",
            onDone = { order += "done" },
            dispatcher = StandardTestDispatcher(testScheduler)
        ) { order += "block" }

        advanceUntilIdle()

        assertTrue(order == listOf("block", "done"))
    }
}
