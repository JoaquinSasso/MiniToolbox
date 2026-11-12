package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class FlagGameRepositoryTest {

    private lateinit var context: Context
    private val repository = DefaultFlagGameRepository

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.flagGameDataStore.edit { it.clear() }
    }

    @Test
    fun bestScore_sinDatosEsCero() = runTest {
        assertEquals(0, repository.bestScore(context).first())
    }

    @Test
    fun recordScore_guardaSoloSiSuperaElRecord() = runTest {
        repository.recordScore(context, 7)
        assertEquals(7, repository.bestScore(context).first())

        repository.recordScore(context, 3)
        assertEquals(7, repository.bestScore(context).first())

        repository.recordScore(context, 8)
        assertEquals(8, repository.bestScore(context).first())
    }
}
