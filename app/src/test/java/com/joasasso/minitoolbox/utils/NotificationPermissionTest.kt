package com.joasasso.minitoolbox.utils

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class NotificationPermissionTest {

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [28])
    fun `en API 28 no se pide porque el permiso no existe`() {
        // Antes se pedía igual y el sistema respondía "denegado" (snackbar en Agua).
        assertFalse(NotificationPermission.shouldRequest(context))
    }

    @Test
    @Config(sdk = [32])
    fun `en API 32 tampoco se pide`() {
        assertFalse(NotificationPermission.shouldRequest(context))
    }

    @Test
    @Config(sdk = [33])
    fun `en API 33 se pide si no esta concedido`() {
        assertTrue(NotificationPermission.shouldRequest(context))
    }

    @Test
    @Config(sdk = [33])
    fun `en API 33 no se pide si ya esta concedido`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificationPermission.shouldRequest(context))
    }
}
