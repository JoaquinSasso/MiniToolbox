# Cooldown de Métricas de Uso de Herramientas

Implementar un mecanismo de deduplicación (debouncing) para el registro de métricas de uso de herramientas, evitando que rebotes de navegación o clics repetidos inflen las estadísticas.

## User Review Required

> [!IMPORTANT]
> El cooldown de métricas se implementará **en memoria de proceso**. Esto significa que si el usuario cierra la app (la mata desde el gestor de tareas) y la vuelve a abrir, el cooldown se reinicia inmediatamente. Esto es aceptable para deduplicar ráfagas de navegación en una misma sesión.

## Proposed Changes

### [Component] Utils

Crear un mecanismo genérico de cooldown que pueda ser testeado fácilmente.

#### [NEW] [ToolDebouncer.kt](file:///C:/Users/nico_/AndroidStudioProjects/MiniToolbox/app/src/main/java/com/joasasso/minitoolbox/utils/ToolDebouncer.kt)
Implementar una clase `ToolDebouncer` que gestione un mapa de `toolId -> lastExecutionTime`.

### [Component] Navigation

Integrar el debouncer en el flujo de navegación principal.

#### [MODIFY] [NavGraph.kt](file:///C:/Users/nico_/AndroidStudioProjects/MiniToolbox/app/src/main/java/com/joasasso/minitoolbox/NavGraph.kt)
- Instanciar `ToolDebouncer(5_000L)` usando `remember`.
- Envolver la llamada a `toolUse(context, route)` con `if (metricsDebouncer.canExecute(route))`.
- Eliminar cualquier referencia residual a `routeToToolId`.

### [Component] Tests

#### [NEW] [ToolDebouncerTest.kt](file:///C:/Users/nico_/AndroidStudioProjects/MiniToolbox/app/src/test/java/com/joasasso/minitoolbox/utils/ToolDebouncerTest.kt)
Unit tests para verificar:
- Registro exitoso en la primera llamada.
- Bloqueo de llamadas dentro del cooldown.
- Registro exitoso tras expirar el cooldown.
- Independencia entre diferentes herramientas.

## Verification Plan

### Automated Tests
- Ejecutar `./gradlew testDebugUnitTest` para validar el nuevo `ToolDebouncerTest`.

### Manual Verification
1. Abrir la pantalla de desarrollo (`Screen.MetricsDev`).
2. Abrir una herramienta cualquiera.
3. Volver atrás rápidamente y abrirla de nuevo (varias veces).
4. Regresar a `MetricsDev` y verificar que el contador local para esa herramienta solo haya subido en **1**.
5. Verificar que los anuncios intersticiales sigan apareciendo según su propia lógica de cooldown (30s en `ToolUsageTracker`).
