package com.joasasso.minitoolbox.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolDebouncerTest {

    private var currentTime = 1000L
    private val debouncer = ToolDebouncer(cooldownMs = 5000L, clock = { currentTime })

    @Test
    fun `primera llamada debe registrar exitosamente`() {
        assertTrue("La primera llamada debería ser permitida", debouncer.canExecute("tool_1"))
    }

    @Test
    fun `segunda llamada dentro del cooldown debe fallar`() {
        debouncer.canExecute("tool_1")
        
        currentTime += 2000 // Menos de 5000
        assertFalse("La llamada dentro del cooldown debería ser bloqueada", debouncer.canExecute("tool_1"))
    }

    @Test
    fun `llamada despues del cooldown debe registrar exitosamente`() {
        debouncer.canExecute("tool_1")
        
        currentTime += 5000 // Justo en el límite
        assertTrue("La llamada después del cooldown debería ser permitida", debouncer.canExecute("tool_1"))
    }

    @Test
    fun `herramientas distintas deben tener cooldowns independientes`() {
        debouncer.canExecute("tool_1")
        
        currentTime += 1000
        assertTrue("Una herramienta distinta debería permitirse aunque otra esté en cooldown", 
            debouncer.canExecute("tool_2"))
        
        assertFalse("La herramienta original debería seguir en cooldown", debouncer.canExecute("tool_1"))
    }

    @Test
    fun `el cooldown se cuenta desde la ultima ejecucion exitosa`() {
        debouncer.canExecute("tool_1") // T=1000
        
        currentTime += 3000 // T=4000
        debouncer.canExecute("tool_1") // Fallida
        
        currentTime += 3000 // T=7000 (han pasado 6000 desde la exitosa)
        assertTrue("El cooldown debería expirar basado en la última exitosa", debouncer.canExecute("tool_1"))
    }
}
