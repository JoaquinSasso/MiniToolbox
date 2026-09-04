---
name: metrics-pipeline
description: Reglas y flujo del sistema de métricas/telemetría propietario de MiniToolbox. Leer antes de tocar cualquier código de métricas en cliente, backend, dashboard o fixtures compartidas.
---

# Pipeline de Métricas — MiniToolbox

## Flujo del dato (extremo a extremo)

```
App (Kotlin)                  Backend (TypeScript)           Almacenamiento
─────────────                 ────────────────────           ──────────────
Metrics.kt (fachada)
  → AggregatesRepository      
    → MetricsDataStore         
      (DataStore: agregados    
       diarios acumulados)     
        → UploadMetricsWorker  
          (WorkManager: envío  
           periódico con       
           constraints de red) 
            → MetricsUploader  
              (HTTP + App Check)
                ─────────────→ index.ts (Cloud Function)
                               validate.ts (validación)
                               → Firestore (documento diario)
                                                              → dashboard/
                                                                (HTML estático
                                                                 en Firebase
                                                                 Hosting)
```

## Restricciones innegociables

Estas vienen de `DECISIONS.md` y no se negocian:

1. **Cero identificadores de dispositivo.** Solo viajan agregados. Si una métrica propuesta requiere seguir un individuo, se descarta.
2. **Claves inmutables.** Renombrar una clave de métrica rompe la serie histórica. El contrato vive en `metrics-fixtures/keys.json` y es consumido por tests de Kotlin y TypeScript. Si cambiás el regex o `normalizeKey` en un lado, el test del otro falla.
3. **Backend estrictamente en TypeScript.** Cloud Functions en TypeScript, almacenamiento en Firestore, dashboard estático en Firebase Hosting.
4. **Almacenamiento local en DataStore.** La orquestación de envíos usa WorkManager.

## Archivos clave

### Cliente (Kotlin)

| Archivo | Responsabilidad |
|---|---|
| `app/.../metrics/Metrics.kt` | Fachada pública. Funciones `appOpen()`, `dailyOpenOnce()`, `toolUse()`. Tiene costuras de test: `metricsRepoFactory`, `metricsTestScheduleHook`, `metricsDispatcher`. |
| `app/.../metrics/MetricsContract.kt` | Definición del contrato de claves y esquema de versiones. |
| `app/.../metrics/MetricsConfig.kt` | Configuración (endpoint, intervalos). Se carga de `BuildConfig`. |
| `app/.../metrics/MetricsSource.kt` | Tracking de fuente de entrada (widget, shortcut, catálogo). |
| `app/.../metrics/ToolRoutes.kt` | Normalización de rutas de navegación a claves de métricas. |
| `app/.../metrics/RetentionBuckets.kt` | Buckets de retención privacy-preserving. |
| `app/.../metrics/storage/AggregatesRepository.kt` | Repositorio de agregados diarios sobre DataStore. |
| `app/.../metrics/storage/MetricsDataStore.kt` | Claves de DataStore y helpers. |
| `app/.../metrics/storage/MetricsSanitizer.kt` | Sanitización de datos antes del envío. |
| `app/.../metrics/storage/JsonUtils.kt` | Serialización JSON de agregados. |
| `app/.../metrics/uploader/MetricsUploader.kt` | HTTP client con App Check token. |
| `app/.../metrics/uploader/UploadMetricsWorker.kt` | CoroutineWorker para WorkManager. |
| `app/.../metrics/uploader/UploadScheduler.kt` | Programación de trabajo periódico. |

### Backend (TypeScript)

| Archivo | Responsabilidad |
|---|---|
| `backend/functions/src/index.ts` | Cloud Function `receiveMetrics`. Verifica App Check, valida, agrega, escribe a Firestore. |
| `backend/functions/src/validate.ts` | Validación de payload y claves contra el contrato. |

### Contrato compartido

| Archivo | Responsabilidad |
|---|---|
| `metrics-fixtures/keys.json` | Casos de test compartidos entre Kotlin y TypeScript. Define claves válidas, normalizaciones esperadas, y formatos de fecha. |

### Dashboard

| Archivo | Responsabilidad |
|---|---|
| `dashboard/index.html` | HTML estático con JS inline. Lee Firestore y renderiza gráficos. Hosted en Firebase Hosting. |

### Tests

| Archivo | Qué verifica |
|---|---|
| `app/.../test/metrics/MetricsTest.kt` | Que `appOpen()` y `dailyOpenOnce()` llaman a los métodos correctos del repo. |
| `app/.../test/metrics/MetricsContractTest.kt` | Que las claves del contrato compartido pasan la validación del cliente. |
| `app/.../test/metrics/ToolMetricsKeysTest.kt` | Que todas las herramientas del catálogo tienen claves de métricas válidas. |
| `app/.../test/metrics/RetentionBucketsTest.kt` | Que los buckets de retención son correctos y privacy-preserving. |

## Verificación

Después de tocar métricas, ejecutar **ambos**:

```powershell
# Cliente (desde la raíz del proyecto)
.\gradlew.bat testDebugUnitTest --tests "*.metrics.*"

# Backend (desde backend/functions/)
cd backend/functions && npm test
```

## Trampas conocidas

1. **`metricsDispatcher` en tests.** Siempre inyectar `StandardTestDispatcher` y llamar `advanceUntilIdle()`. El dispatcher de producción es `Dispatchers.IO` fire-and-forget; sin inyección, los tests son flaky.
2. **BOM de Compose alpha.** El proyecto usa `compose-bom-alpha` porque Material3 1.5.x trae la API expresiva usada en el Pomodoro. Mezclar material3 alpha con foundation estable produce `AbstractMethodError` en runtime.
3. **`metrics-fixtures/keys.json` es un contrato bilateral.** Editarlo requiere verificar que los tests de **ambos** lados (Kotlin y TypeScript) sigan pasando.

## Documentación relacionada

- `docs/metrics-glossary.md` — semántica de cada contador
- `docs/metrics-double-count-postmortem.md` — postmortem de conteo doble
- `docs/metrics-pipeline-blockage-postmortem.md` — postmortem de bloqueo del pipeline
