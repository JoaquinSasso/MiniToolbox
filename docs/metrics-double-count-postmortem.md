# Postmortem: Sesgo de doble conteo en métricas de apertura (app_open)

**Fecha:** 17 de agosto de 2026
**Rango afectado:** 24 de septiembre de 2025 – 17 de agosto de 2026
**Commit de corrección:** `927ac8e`
**Entorno:** Telemetría propia (Android -> Firebase Functions -> Firestore)
**Síntoma:** Inflación sistemática del contador `app_open`.

## Resumen
Se detectó y corrigió un bug de lógica en el sistema de métricas que causaba un doble conteo de la primera apertura diaria de cada dispositivo. El error inflaba las estadísticas de uso total (`app_open`) y prohibía estadísticamente el valor unitario (1) en cualquier ventana de tiempo de 24 horas, sesgando la serie histórica hacia valores pares.

---

## Detección y Evidencia Estadística
El hallazgo se produjo tras auditar la forma de la distribución de los datos crudos en Firestore, observando anomalías que no correspondían con un uso orgánico:

1. **Análisis de Paridad**: Sobre un dataset de 280 días, 202 días (72%) presentaban un valor de aperturas par. En una distribución normal con tráfico aleatorio, la probabilidad de paridad debería tender al 50%. En el segmento de los últimos 120 días (con tráfico controlado de 1-3 usuarios), la anomalía subió al 83% (100 de 120 días).
2. **Prueba del Mínimo Absoluto**: En 327 días de registros, **no existe un solo día con valor 1**. El mínimo absoluto registrado es 2. Esto es estadísticamente imposible para una app con instalaciones nuevas, donde al menos un usuario debería haber abierto la app exactamente una vez en su primer día.
3. **Reproducción**: Se confirmó mediante análisis estático del código fuente (ver sección Causa Raíz) que el flujo de ejecución del cliente Android garantizaba la colisión de llamadas. La ausencia de valores unitarios (1) en los registros con tráfico mínimo es evidencia empírica consistente con la naturaleza determinística del bug.

---

## Análisis de Causa Raíz
El problema residía en una colisión de responsabilidades en `MainActivity.kt` (aprox. líneas 69-70) y `Metrics.kt`.

### Código Defectuoso
En el cliente Android:
```kotlin
// MainActivity.kt
LaunchedEffect(Unit) {
    appOpen(applicationContext)      // Llamada 1
    dailyOpenOnce(applicationContext) // Llamada 2
}
```

En `Metrics.kt`:
```kotlin
fun appOpen(context: Context) = io {
    ctx.repo().incrementAppOpen() // Incrementa app_open
}

fun dailyOpenOnce(context: Context) = io {
    // ... guarda de SharedPreferences para "una vez por día" ...
    if (last != today) {
        ctx.repo().incrementAppOpen() // INCREMENTA EL MISMO CONTADOR
    }
}
```

**La intención original** de `dailyOpenOnce` era registrar "Días Activos" (DAU), pero reutilizó por error el método `incrementAppOpen()`. Como ambas funciones se ejecutan al inicio, la primera apertura del día disparaba ambas; las aperturas subsiguientes sólo disparaban `appOpen()`.

---

## Corrección Aplicada

### 1. Separación de Contadores (Cliente Android)
Se introdujo una nueva métrica con semántica de dispositivo activo, independiente de las aperturas totales:
- `app_open`: Incrementa en cada inicio de la Activity (sin cambios).
- `daily_active`: Incrementa **exactamente una vez por día** por dispositivo.

Se actualizaron los componentes de persistencia local:
- `MetricsKeys`: Nuevas claves `DAILY_ACTIVE_BY_DAY` y `SENT_DAILY_ACTIVE_BY_DAY`.
- `AggregatesRepository`: Implementación de `incrementDailyActive()` y actualización de `DayDelta`.
- `UploadMetricsWorker`: Inclusión del campo `daily_active` en el payload JSON.

### 2. Compatibilidad hacia atrás (Backend)
El backend recibe simultáneamente payloads de versiones desde la 1.0 hasta la 1.3.1. Para evitar la pérdida de datos de usuarios que no actualicen de inmediato:
- En `validate.ts`, el campo `daily_active` se definió como **opcional**.
- En `index.ts` (`normDoc`), la ausencia del campo se trata como 0, nunca como error.
- La función de ingesta ahora realiza incrementos atómicos sobre `totals.daily_active` sólo si el valor en el payload es `> 0`.

### 3. Visualización y Transparencia (Dashboard)
Se modificó `dashboard/public/index.html` para:
- Mostrar el nuevo KPI "Dispositivos Activos" (DDAU).
- Añadir un banner de advertencia: *"Toda la serie desde el inicio hasta la corrección del 2026-08-17 contiene un doble conteo sistemático"* (Commit `927ac8e`).
- **Decisión de Integridad**: No se reescribieron los datos históricos en Firestore. La serie se anota para su interpretación, pero no se edita para "ajustarla a la narrativa", preservando la trazabilidad de la auditoría.

---

## Impacto y Verificación
1. **Build**: `./gradlew clean assembleDebug` exitoso.
2. **Prueba Unitaria**: Verificado que tras la primera apertura del día `app_open == 1` y `daily_active == 1`; tras la segunda apertura, `app_open == 2` y `daily_active == 1`.
3. **Backend Lint**: `npm run lint` sin errores en las funciones de Firebase.
4. **Validación de Payload**:
   - Payload con `daily_active`: Aceptado y persistido.
   - Payload sin `daily_active` (App vieja): Aceptado, `daily_active` tratado como 0.
   - Payload malformado: Rechazado por `validateBody`.

---

## Control Preventivo (Futuro)
Para evitar que errores de lógica de agregación pasen desapercibidos, se recomienda implementar una "Auditoría de Sanidad de Datos" automática que alerte sobre:
- **Invariantes violadas**: Alertar si `daily_active > app_open` en cualquier documento diario.
- **Sesgo de paridad**: Alertar si la proporción de días pares en una ventana móvil de 30 días se desvía más de 3 sigmas del 50%.
- **Mínimo estancado**: Alertar si el valor mínimo de la serie `app_open` es `> 1` durante más de 7 días consecutivos.
