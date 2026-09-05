# MiniToolbox — Reglas de Proyecto para Agentes

## Qué es este proyecto

MiniToolbox es una aplicación Android (Kotlin, Jetpack Compose) publicada en Google Play con ~115 descargas orgánicas. Es un módulo único con 33 herramientas de utilidad, un sistema de métricas propietario sobre Firebase, y funciona como portfolio técnico del autor. El 40% del uso se concentra en el clúster de sensores/hardware (lupa, linterna, regla AR, brújula, nivel, fotómetro).

## Documentos clave

Leer **antes** de proponer cambios:

| Documento | Propósito | Cuándo leerlo |
|---|---|---|
| `DECISIONS.md` | Restricciones arquitectónicas innegociables | Siempre, antes de cualquier refactoring |
| `.agentrules.md` | Reglas de conducta del agente | Ya cargado en contexto |
| `SCRATCHPAD.md` | Lista técnica de deuda y bugs (formato máquina) | Antes de empezar trabajo, después de terminarlo |
| `PRODUCT_BACKLOG.md` | Ideas de producto, features, nice-to-have | Al encontrar ideas de producto durante el trabajo |
| `docs/metrics-glossary.md` | Semántica de cada contador de telemetría | Al tocar métricas |
| `docs/architecture.md` | Diagrama de arquitectura general | Para orientación inicial |

## Reglas de flujo de trabajo

### Antes de proponer cambios

1. Leer `DECISIONS.md`. Las violaciones se rechazan sin discusión.
2. Leer `SCRATCHPAD.md` para entender prioridades y no duplicar trabajo.
3. **Flujo de Git (Creación de ramas):** Antes de realizar cualquier cambio en el código, crear una nueva rama descriptiva a partir de `master`:
   ```bash
   git checkout master
   git pull
   git checkout -b <tipo>/<nombre-descriptivo>
   ```
4. Si la tarea toca el pipeline de métricas, leer el skill en `.agents/skills/metrics-pipeline/SKILL.md`.

### Al terminar trabajo

1. Actualizar `SCRATCHPAD.md`: marcar ítems completados `[x]`, agregar hallazgos nuevos con su sección y prioridad.
2. Si durante el trabajo aparecen ideas de producto, sugerencias de features, decisiones de diseño pendientes, o cualquier "nice to have", **agregarlos a `PRODUCT_BACKLOG.md`**. No mezclar decisiones de producto con deuda técnica en `SCRATCHPAD.md`.

### Verificación de código

Antes de entregar cambios de código, ejecutar:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Si se modificó el backend:

```bash
cd backend/functions && npm test
```

### Privacidad por diseño

Solo se manejan métricas agregadas sin identificadores de dispositivo. Si una métrica nueva requiere seguir individuos, descartarla o buscar su variante agregada. Esta restricción está detallada en `DECISIONS.md` §3.

### Continuidad de datos históricos

Los identificadores de métricas son un contrato estricto. Renombrar una clave rompe la serie en dos. El contrato compartido vive en `metrics-fixtures/keys.json`. Esta restricción está detallada en `DECISIONS.md` §4.

### Formato del SCRATCHPAD

El SCRATCHPAD.md usa un formato máquina optimizado para agentes. Cada ítem tiene:
- Status: `[ ]` pendiente, `[/]` en progreso, `[x]` hecho, `[—]` descartado
- ID corto (grep-friendly, e.g. `goAsync-receivers`)
- Archivos afectados
- Descripción en una línea

Las decisiones de producto, features, y nice-to-haves van en `PRODUCT_BACKLOG.md`, nunca en el SCRATCHPAD.

