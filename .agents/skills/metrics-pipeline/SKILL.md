---
name: metrics-pipeline
description: >-
  Mapa, contratos y trampas del sistema de telemetría propio de MiniToolbox (cliente Kotlin,
  Cloud Functions en TypeScript, Firestore y dashboard). Usar antes de modificar cualquier
  archivo en app/.../metrics/, backend/functions/, dashboard/, metrics-fixtures/ o
  docs/metrics-glossary.md, y antes de interpretar datos exportados del dashboard.
---

# Pipeline de métricas

## Flujo del dato

1. **Eventos.** `metrics/Metrics.kt` es la fachada: `appOpen`, `dailyOpenOnce`, `toolUse` y `widgetUse`. (`adImpression` se retiró con los anuncios; ver `ads` en el glosario.) Cada evento corre fire-and-forget en `Dispatchers.IO` y respeta el opt-out (`isMetricsEnabled`).
2. **Persistencia.** `metrics/storage/AggregatesRepository.kt` acumula contadores por día en `metricsDataStore` (`storage/MetricsDataStore.kt`), como mapas JSON dentro de Preferences.
3. **Agendado.** `metrics/uploader/UploadScheduler.kt` encola `OneTimeWorkRequest` únicos (`maybeSchedule`, `enqueueNowExpedited` en `ON_STOP`, `maybeFlushOnThreshold`). No es un trabajo periódico.
4. **Envío.** `metrics/uploader/UploadMetricsWorker.kt` arma el payload (`PAYLOAD_SCHEMA_VERSION = 5`), lo congela como lote pendiente con un `batch_id` y lo envía con `MetricsUploader.kt`, que agrega el header `X-Firebase-AppCheck`. La respuesta se clasifica en `UploadOutcome`: Success, PermanentReject, AuthError o Transient.
5. **Ingesta.** `backend/functions/src/index.ts`, función `ingest`. Valida y sanea con `validate.ts`, deduplica por `batch_id` (`metrics_ingest_batches`) e incrementa `metrics_daily/{mes}/days/{día}`. Solo acepta App Check: sin token válido responde 401 y loguea `ingest_unauthorized`. Registra cada lote en `metrics_ingest_logs` y los lotes aceptados y la salud del cliente en el rollup `metrics_auth`.
6. **Lectura.** Las funciones `metricsDaily`, `metricsSummary` y `metricsAuth` requieren el header `X-API-Key` con `READ_METRICS_API_KEY` (la clave de escritura ya no existe). El dashboard (`dashboard/public/index.html`) las consume y guarda la URL y la clave en `localStorage`. Sin clave, muestra datos de demostración.

## Contratos (romperlos corrompe datos de forma irreversible)

1. **Formato de clave.** `MetricsContract.KEY_RE` (Kotlin) y `KEY_RE` en `validate.ts` (TS) deben coincidir. Los casos compartidos están en `metrics-fixtures/keys.json`, pero **hoy solo los consume el lado Kotlin** (`MetricsContractTest`): el backend no tiene tests.
2. **Clave de herramienta.** `Tool.metricsKey` (`tools/Tool.kt`), que por defecto es `screen.route`. Nunca se cambia una clave existente: si cambia la ruta, se fija `metricsKey` con el valor anterior. `ToolMetricsKeysTest.expectedKeys` congela el conjunto. Solo se edita al **agregar** una herramienta.
3. **Lista de herramientas del backend.** `KNOWN_TOOLS` en `index.ts` se arma con los valores de `TOOL_ROUTE_MAP` más `EXTRA_KNOWN_TOOLS`. Una clave que no figure ahí se guarda como `other` (y se loguea `ingest_unknown_tools`). **Toda herramienta nueva se agrega en los dos lados en el mismo PR.**
4. **Subpantallas.** Las subpantallas reportan bajo la clave de su herramienta. El mapeo está en `ToolRoutes.findToolByScreen`: `PomodoroDetail` va a `pomodoro`, `WaterStats` a `water` y las pantallas de gastos a `meetings`.
5. **Orígenes.** Los valores de `MetricsSource` (`nav`, `notification`, `widget`, `shortcut`, `unknown`) deben coincidir con `KNOWN_SOURCES` en `index.ts`.
6. **Retención.** Las categorías de `RetentionBuckets` deben coincidir con `KNOWN_AGE` y `KNOWN_INTENSITY` en `index.ts`.
7. **Versión de esquema.** Cambiar la semántica de un campo exige subir `PAYLOAD_SCHEMA_VERSION` y documentarlo en `docs/metrics-glossary.md` (sección "Metadatos del lote").
8. **Privacidad.** Ningún identificador persistente en el payload. Ver `DECISIONS.md` §3.

## Sesgos conocidos (no los empeores; si los corregís, actualizá el glosario)

- Las builds debug envían al endpoint de producción a propósito (decisión del autor: su uso diario es en debug) y el payload no lleva `build_type`, así que no se pueden separar. `dev/DevInspector.kt` genera claves de prueba (`metricTest`, `metricsTest`), que el backend guarda como `other`. Detalle en el glosario, "Nota sobre las builds debug".
- Detalle y estado de cada uno: `docs/backlog.md`.

## Interpretar datos exportados

Leé `docs/metrics-glossary.md` antes de sacar conclusiones:
- `versions` es DAU por versión (1 por dispositivo y por día), no aperturas.
- `versions_first_seen` suma una unidad por cada versión que pasa por un dispositivo, así que **no** mide instalaciones.
- `app_open` está inflado hasta el 17/08/2026.
- Las claves históricas que ya no existen en `ToolRegistry` (`quotes`, `pomodoro_list`, `pomodoro_detail`, `pro`, `other`, claves de prueba) se normalizan o excluyen explícitamente, y se dice cómo.
- Con pocos usuarios, avisá siempre del bajo poder estadístico.

## Verificación

```powershell
# Cliente
.\gradlew.bat testDebugUnitTest --tests "*.metrics.*"
# Backend (no hay tests: solo lint y build)
cd backend/functions; npm ci; npm run lint; npm run build
```

Si tocaste el backend, aclarale al autor que el deploy (`npm run deploy`) es manual y que no lo ejecutaste.

## Documentación relacionada

- `docs/metrics-glossary.md`: semántica de cada contador y sus sesgos.
- `docs/metrics-double-count-postmortem.md` y `docs/metrics-pipeline-blockage-postmortem.md`.
