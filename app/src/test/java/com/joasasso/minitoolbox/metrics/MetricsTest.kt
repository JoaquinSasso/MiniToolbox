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
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.never
import org.mockito.Mockito.times
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
    }

    @Test
    fun `verify double count fix - appOpen and dailyOpenOnce increment separate counters`() = runTest {
        val context = mock(Context::class.java)
        val appContext = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(appContext)

        val sp = mock(SharedPreferences::class.java)
        `when`(appContext.getSharedPreferences("metrics_prefs", 0)).thenReturn(sp)
        `when`(sp.getBoolean("enabled", true)).thenReturn(true)

        val spDaily = mock(SharedPreferences::class.java)
        `when`(appContext.getSharedPreferences("metrics_daily_once", 0)).thenReturn(spDaily)
        `when`(spDaily.getString("last_day", null)).thenReturn("1970-01-01")

        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(spDaily.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        mockConstruction(AggregatesRepository::class.java).use { mockRepo ->
            appOpen(context)
            dailyOpenOnce(context)

            val repo = mockRepo.constructed()[0]
            verify(repo, times(1)).incrementAppOpen()
            verify(repo, times(1)).incrementDailyActive()
        }
    }
    
    @Test
    fun `verify dailyOpenOnce only increments dailyActive and NOT appOpen`() = runTest {
        val context = mock(Context::class.java)
        val appContext = mock(Context::class.java)
        `when`(context.applicationContext).thenReturn(appContext)

        val sp = mock(SharedPreferences::class.java)
        `when`(appContext.getSharedPreferences("metrics_prefs", 0)).thenReturn(sp)
        `when`(sp.getBoolean("enabled", true)).thenReturn(true)

        val spDaily = mock(SharedPreferences::class.java)
        `when`(appContext.getSharedPreferences("metrics_daily_once", 0)).thenReturn(spDaily)
        `when`(spDaily.getString("last_day", null)).thenReturn("1970-01-01")

        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(spDaily.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        mockConstruction(AggregatesRepository::class.java).use { mockRepo ->
            dailyOpenOnce(context)

            val repo = mockRepo.constructed()[0]
            verify(repo, times(1)).incrementDailyActive()
            verify(repo, never()).incrementAppOpen()
        }
    }
}
