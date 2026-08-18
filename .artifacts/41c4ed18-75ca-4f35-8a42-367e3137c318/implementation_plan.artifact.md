# Plan de Actualización de Documentación de Arquitectura

Este plan tiene como objetivo producir una documentación de arquitectura real, honesta y verificada del repositorio MiniToolbox, corrigiendo errores previos y añadiendo subsistemas críticos.

## User Review Required

> [!IMPORTANT]
> Se ha detectado una desincronización taxonómica: la ruta analítica `quotes` está vinculada a la pantalla `BasicPhrasesScreen`. Esto significa que las métricas reportadas bajo "quotes" corresponden en realidad a "Frases Básicas". Se documentará como limitación.

> [!WARNING]
> Existe un bug potencial en `CategoriesScreen.kt:72` donde un auto-import de Protobuf está sombreando `kotlin.collections.emptyList()`. Esto se documentará y se propone corregir en un paso posterior (fuera del alcance de esta tarea de documentación, pero anotado).

## Proposed Changes

### [Componente: Documentación]

#### [MODIFY] [architecture.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/docs/architecture.md)
Reescritura completa:
- Diagrama A: Componentes y Gestión de Estado (ViewModel vs. Local State, inclusión de Widgets Glance).
- Diagrama B: Pipeline de Telemetría End-to-End (corregido: CF como gate de lectura, flujo de payload desde Worker, gate de opt-out).
- Diagrama C: Navegación y Rutas (validación de 42 rutas vs. 33 herramientas).
- Diagrama D: Subsistema de Alarmas Pomodoro (Secuencia/Componentes de background).
- Estructura de paquetes real y modelo de persistencia (19 DataStores).
- Sección de "Limitaciones conocidas".

#### [MODIFY] [README.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/README.md)
- Enlace al diagrama principal de arquitectura.

#### [MODIFY] [CONTRIBUTING.md](file:///C:/Users/nico_/StudioProjects/MiniToolbox/CONTRIBUTING.md)
- Adición de regla de actualización de documentación de arquitectura ante cambios estructurales.

## Verification Plan

### Manual Verification
1. **Tabla de Respaldo Caja-por-Caja**: Se generará una tabla cruzando cada elemento de los diagramas con el archivo y línea de código correspondiente.
2. **Validación de Paquetes**: Ejecución de `find` para asegurar que no existan menciones a paquetes fantasma.
3. **Renderizado Mermaid**: Confirmación de legibilidad de los diagramas en el entorno de visualización de GitHub/Android Studio.

### Automated Tests
- No aplica (cambios en documentación).
