# Postmortem: Bloqueo permanente del envío de métricas

**Fecha de detección:** 29 de agosto de 2026

**Origen probable:** 11 de octubre de 2025 (`9906e22`, deep linking de notificaciones de Pomodoro)

**Agravante:** rotación de `METRICS_API_KEY` sin ventana de solapamiento, agosto de 2026

**Entorno:** Telemetría propia (Android → Firebase Functions → Firestore)

**Síntoma:** Dispositivos que dejan de reportar métricas de forma definitiva, sin error visible para el usuario ni alerta en el servidor.

## Resumen

Dos fallas independientes dejaron dispositivos incapaces de enviar métricas **para siempre**, sin posibilidad de recuperarse solos. Ambas comparten la misma causa estructural: **un fallo determinista tratado como transitorio, sobre un payload congelado que nunca se reconstruía**.

La primera fue una clave de herramienta generada a partir de una ruta de navegación con argumentos, que el backend rechazaba con 400. La segunda fue una rotación de credencial que dejó a toda la base instalada respondiendo 401. En los dos casos el cliente reintentaba indefinidamente exactamente los mismos bytes.

---

## Detección

El hallazgo no vino de una alerta sino de una observación doméstica: una usuaria conocida utilizaba a diario la herramienta de registro de agua y su actividad no figuraba en las métricas, con la telemetría habilitada en configuración.

Al inspeccionar el estado interno en una build de debug apareció un lote congelado con una clave de herramienta que contenía barras y un UUID, del estilo `pomodoro/detail/<uuid>`.

**El sistema no tenía forma de avisar.** No existían contadores de fallo del lado del cliente, y del lado del servidor un dispositivo que no envía es indistinguible de un dispositivo que no se usa. El incidente pudo durar meses sin que nada lo señalara.

---

## Análisis de causa raíz

### Falla 1: la ruta de navegación usada como clave de métrica

El backend valida las claves contra `^[a-zA-Z0-9._-]{1,64}$` y rechaza el lote completo si alguna no cumple. En el cliente había dos puntos que registraban uso de herramientas:

- `NavGraph.kt`, protegido por un filtro contra el catálogo de herramientas.
- `MainActivity.kt`, al procesar un deep link, **sin ningún filtro**.

En este último, un comentario documentaba la decisión que causó el problema:

```kotlin
// Quitamos el chequeo problemático de if (Screen.isValidRoute(route))
toolUse(applicationContext, route)
```

El chequeo se había eliminado porque daba falsos negativos con rutas parametrizadas. Al sacarlo, la ruta llegaba cruda a la métrica. La cadena completa:

```
PomodoroAlarmActivity → Screen.PomodoroDetail.createRoute(id)  // "pomodoro/detail/<uuid>"
  → putExtra("startRoute", route)
  → el usuario toca la notificación
  → MainActivity → toolUse(context, route)   // clave inválida escrita en disco
```

Cualquier usuario que tocara una notificación de Pomodoro se envenenaba la cola. No era un artefacto de desarrollo.

### Falla 2: rotación de credencial sin solapamiento

`MetricsConfig` lee la API key desde `BuildConfig`, o sea que **queda horneada en cada APK publicado**. Al rotar el secret y deshabilitar el valor anterior, todos los dispositivos instalados empezaron a recibir 401.

A diferencia de un dato corrupto, esto no se puede sanear: es una credencial que dejó de existir. La única salida era que el usuario actualizara la app, o que el servidor volviera a aceptar la clave anterior.

### La causa estructural común

Las dos fallas fueron letales por la misma razón, en `UploadMetricsWorker`:

```kotlin
val ok = postJson(endpoint, payloadJson)
if (ok) { /* ... */ } else { Result.retry() }
```

`postJson` devolvía un `Boolean`, así que un 400 determinista era indistinguible de un timeout. Como el payload se congela para garantizar idempotencia y nunca se reconstruía, cada reintento enviaba los mismos bytes y recibía el mismo rechazo. WorkManager no impone tope de intentos: el ciclo no terminaba nunca.

Había además una segunda forma de bloqueo, más silenciosa. Las validaciones previas del worker hacían esto:

```kotlin
if (!isValidDay(d.day)) return@withContext Result.success()
```

Un día mal formado o un contador negativo apagaban el envío **para siempre**, sin enviar nada, sin log, y reportando éxito.

---

## Corrección aplicada

### 1. Backend tolerante (`de27574`)

`validateBody` pasó de rechazar el lote entero a sanear: las claves inválidas se normalizan (`pomodoro/detail/<uuid>` → `pomodoro_detail`, colapsando los segmentos que son identificadores para no explotar la cardinalidad), los items con día inválido se descartan individualmente, y el 400 queda reservado para fallas estructurales irreparables.

Fue el arreglo de mayor palanca: **desbloqueó a los dispositivos afectados sin necesidad de publicar una versión**.

### 2. Reversión de la rotación de credencial

Se restauró el valor anterior de `METRICS_API_KEY` como versión vigente del secret. Se verificó previamente que la clave nueva nunca había salido del entorno de desarrollo, así que aceptar la anterior no ampliaba la superficie expuesta.

### 3. Clasificación de respuestas en el cliente

`MetricsUploader` devuelve un tipo con cuatro casos en lugar de un booleano:

| Respuesta | Tratamiento |
|---|---|
| 2xx | commit del delta |
| 400, 404, 413, 422 | rechazo permanente: sanear origen, descartar lote, seguir |
| 401, 403 | credenciales: no reintentar, conservar el lote |
| 408, 429, 5xx, red | transitorio: reintentar con backoff hasta 5 intentos |

En el caso permanente el orden importa: **primero se sanea el origen y después se descarta el pendiente**. Al revés, `buildDeltasSinceLastSent` regenera el mismo payload envenenado.

### 4. Eliminación del origen

La clave de métrica se desacopló de la ruta: `ToolRegistry` expone un `metricsKey` estable y `ToolRoutes` resuelve rutas contra el catálogo. Una ruta desconocida o con argumentos **no genera métrica**, en lugar de generar una inválida.

### 5. Saneo automático del almacenamiento

`MetricsSanitizer` corre al arrancar la app y al inicio de cada ejecución del worker, versionado por esquema. Repara los dispositivos que ya tenían datos corruptos en disco.

### 6. Migración a Firebase App Check

Se reemplazó el secreto compartido por atestación de Play Integrity, que no se puede extraer del APK y no necesita rotarse. La rama de API key se mantiene en el backend durante la transición.

---

## Impacto

- **Duración estimada:** hasta 10 meses para los dispositivos afectados por la falla 1, según cuándo tocaron una notificación de Pomodoro por primera vez.
- **Datos perdidos:** no recuperables. Los agregados quedaron en el dispositivo y, superada la ventana de retención local, se podan.
- **Sesgo introducido:** los usuarios de Pomodoro con notificaciones activas están subrepresentados en toda la serie previa a la corrección. Es un sesgo **selectivo**, no uniforme: afecta más a los usuarios más comprometidos con esa herramienta.

Se corrigió además un bug adyacente encontrado durante la investigación: las notificaciones y widgets de agua usaban `startRoute = "agua"`, ruta que no existe en el NavGraph. La navegación fallaba dentro de un `try/catch` silencioso, así que **tocar esos accesos no llevaba a ninguna parte**.

---

## Control preventivo

**Invariante de diseño.** Ningún camino del worker puede dejar el pipeline detenido de forma indefinida. Todo error corrige y sigue, o descarta y sigue. En particular, ninguna validación devuelve `Result.success()` sin haber enviado o descartado explícitamente.

**Contrato de claves compartido.** `MetricsContract.kt` en el cliente y `validate.ts` en el backend implementan la misma normalización, y los tests de ambos lados consumen `metrics-fixtures/keys.json`. Si alguien cambia un lado y no el otro, un test falla.

**Observabilidad del propio pipeline.** El cliente acumula `LAST_UPLOAD_CODE`, `LAST_UPLOAD_ERROR`, `LAST_SUCCESS_AT`, `CONSECUTIVE_FAILURES`, `DROPPED_BATCHES` y `SANITIZED_KEYS_TOTAL`, los envía en `client_health` dentro del propio payload, y los expone en una pantalla de diagnóstico desbloqueable en release. El servidor registra `ingest_sanitized` e `ingest_unknown_tools`.

**Tests de regresión.** `UploadWorkerTest` cubre la matriz completa de respuestas HTTP y reproduce el estado envenenado exacto. `ToolMetricsKeysTest` congela el conjunto de claves publicadas y verifica que todo `startRoute` resuelva a una ruta real.

**Procedimiento de rotación de credenciales.** Toda credencial embebida en el APK requiere ventana de solapamiento: aceptar la anterior y la nueva, medir el tráfico de cada una, y retirar la vieja recién cuando llegue a cero de forma sostenida. El contador `auth_method` en `metrics_ingest_batches` existe para eso.

---

## Lecciones

**Un fallo determinista tratado como transitorio no es un reintento, es un bucle.** La distinción entre "esto puede funcionar más tarde" y "esto nunca va a funcionar" tiene que ser explícita en el código, no implícita en un booleano.

**Un sistema de telemetría necesita telemetría propia.** Un dispositivo que dejó de reportar es indistinguible de uno que no se usa. Sin contadores de salud, el silencio se lee como ausencia de actividad.

**Un contrato que vive en un solo lado del cable no es un contrato.** El regex de claves existía únicamente en el backend; el cliente podía generar datos que el servidor jamás aceptaría, y sólo se enteraba en producción.

**Los identificadores de datos y los identificadores de navegación evolucionan por razones distintas.** Acoplarlos hace que un cambio de UX rompa la serie histórica, o peor, el pipeline entero.