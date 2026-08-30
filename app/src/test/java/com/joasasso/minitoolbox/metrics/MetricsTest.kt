package com.joasasso.minitoolbox.metrics

import android.content.Context
import android.content.SharedPreferences
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        metricsDispatcher = testDispatcher
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        metricsDispatcher = Dispatchers.IO
        metricsRepoFactory = null
        metricsTestScheduleHook = null
    }

    private fun setupContextMocks(): Pair<Context, Context> {
        val appContext = mock(Context::class.java)
        val context = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(appContext)
        `when`(appContext.applicationContext).thenReturn(appContext)

        listOf(context, appContext).forEach { ctx ->
            val sp = mock(SharedPreferences::class.java)
            `when`(ctx.getSharedPreferences(anyString(), anyInt())).thenReturn(sp)
            `when`(sp.getBoolean(anyString(), anyBoolean())).thenReturn(true)
            `when`(sp.getString(anyString(), any())).thenReturn("1970-01-01")

            val editor = mock(SharedPreferences.Editor::class.java)
            `when`(sp.edit()).thenReturn(editor)
            `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
            `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        }

        return context to appContext
    }

    @Test
    fun `appOpen increments app_open and NOT daily_active`() = runTest {
        val (context, _) = setupContextMocks()
        val mockRepo = mock(AggregatesRepository::class.java)
        metricsRepoFactory = { mockRepo }
        metricsTestScheduleHook = { /* no-op */ }

        appOpen(context)
        advanceUntilIdle()

        verify(mockRepo).incrementAppOpen()
        verify(mockRepo, never()).incrementDailyActive()
    }

    @Test
    fun `dailyOpenOnce increments daily_active and NOT app_open`() = runTest {
        val (context, _) = setupContextMocks()
        val mockRepo = mock(AggregatesRepository::class.java)
        metricsRepoFactory = { mockRepo }
        metricsTestScheduleHook = { /* no-op */ }

        dailyOpenOnce(context)
        advanceUntilIdle()

        verify(mockRepo).incrementDailyActive()
        verify(mockRepo, never()).incrementAppOpen()
    }
}
