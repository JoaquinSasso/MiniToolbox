package com.joasasso.minitoolbox.metrics

import android.content.Context
import android.content.SharedPreferences
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class MetricsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        metricsTestRepo = null
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
    fun `verify double count fix - appOpen and dailyOpenOnce increment separate counters`() = runTest {
        val (context, _) = setupContextMocks()
        val mockRepo = mock(AggregatesRepository::class.java)
        metricsTestRepo = mockRepo
        metricsTestScheduleHook = { /* no-op */ }

        appOpen(context)
        dailyOpenOnce(context)

        verify(mockRepo, timeout(5000)).incrementAppOpen()
        verify(mockRepo, timeout(5000)).incrementDailyActive()
    }

    @Test
    fun `verify dailyOpenOnce only increments dailyActive and NOT appOpen`() = runTest {
        val (context, _) = setupContextMocks()
        val mockRepo = mock(AggregatesRepository::class.java)
        metricsTestRepo = mockRepo
        metricsTestScheduleHook = { /* no-op */ }

        dailyOpenOnce(context)

        verify(mockRepo, timeout(5000)).incrementDailyActive()
        verify(mockRepo, never()).incrementAppOpen()
    }
}
