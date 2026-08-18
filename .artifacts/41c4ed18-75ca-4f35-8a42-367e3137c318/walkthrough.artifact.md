# Walkthrough de Actualización de Arquitectura

Se ha reescrito la documentación de arquitectura de MiniToolbox para reflejar fielmente el estado actual del sistema, eliminando referencias a paquetes inexistentes y añadiendo diagramas técnicos verificados.

## Cambios Realizados

### [architecture.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/docs/architecture.md)
- **Diagramas Mermaid**: Se incluyeron 4 diagramas detallados:
    - **Capa de Componentes**: Muestra la asimetría del estado (ViewModel vs. `remember`) y la inclusión de Widgets.
    - **Pipeline de Telemetría**: Pipeline corregido con gate de opt-out, flujo de payload desde el Worker y restricción de seguridad en Firestore.
    - **Sistema de Navegación**: Validación de 42 rutas y su relación con el catálogo de herramientas.
    - **Alarmas Pomodoro**: Detalle del flujo crítico de background (AlarmManager + FGS + Media3).
- **Estructura de Paquetes**: Actualizada a la realidad del filesystem (incluyendo sub-paquetes de `tools`).
- **Persistencia**: Se documentaron las 19 instancias de `DataStore`, identificando las 2 instancias embebidas en Screens.
- **Limitaciones**: Sección honesta sobre deuda técnica, incluyendo el acoplamiento de rutas analíticas y el bug de shadowing de Protobuf.

### [README.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/README.md)
- Se añadió un enlace directo a la documentación de arquitectura en la sección correspondiente.

### [CONTRIBUTING.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/CONTRIBUTING.md)
- Se añadió una regla obligatoria para mantener la documentación de arquitectura actualizada ante cambios estructurales.

## Verificación Realizada

- **Paquetes**: Verificados mediante `Get-ChildItem`. No existen `juegos`, `medicion` ni `tools/data`.
- **Conteos**:
    - 2 ViewModels (`CategoryViewModel`, `MinesViewModel`).
    - 42 Rutas en `NavGraph.kt`.
    - 33 Herramientas en `ToolRegistry.kt`.
    - 19 `preferencesDataStore`.
- **SHA del Commit**: `d43a3d53f2d6703d1cc04dd09aab114902cae5a7`.
- **Anomalías**: Se identificó y documentó el shadowing en `CategoriesScreen.kt:72` y la desincronización de la ruta `quotes`.

---
*Este documento sirve como base para el mantenimiento futuro de la integridad arquitectónica del proyecto.*
