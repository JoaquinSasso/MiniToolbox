# ADR 002: La ingesta de métricas acepta solo App Check

*   **Estado:** Aceptada
*   **Fecha:** 2026-11-10
*   **Decisión del autor:** pedido explícito de retirar la autenticación por API key en la ingesta, sin esperar al criterio que fijaba `DECISIONS.md` §2.

## Contexto

Desde la 1.3.2 la app autentica cada lote con un token de Firebase App Check (`X-Firebase-AppCheck`). La API key dejó de existir en el cliente en `a95191b`. El backend mantenía una segunda rama que aceptaba `METRICS_API_KEY` para no dejar sin envío a los dispositivos con versiones anteriores a la 1.3.2.

`DECISIONS.md` §2 fijaba retirar esa rama cuando el rollup `metrics_auth` mostrara 0 tráfico por API key. Este ADR reemplaza ese criterio por una decisión directa.

Hay un segundo motivo, de seguridad: `METRICS_API_KEY` viajó embebida en APKs publicados, así que hay que considerarla expuesta. Mientras el backend la aceptara, cualquiera que la extrajera podía escribir lotes en `ingest` y, como los endpoints de lectura también la aceptaban, leer todas las métricas.

## Decisión

1.  `ingest` acepta un lote solo si trae un token de App Check válido. Sin token válido responde 401 y registra `ingest_unauthorized` con la `app_version` declarada.
2.  El secret `METRICS_API_KEY` deja de usarse en todas las funciones. Los endpoints de lectura (`metricsDaily`, `metricsSummary`, `metricsAuth`) aceptan solo `READ_METRICS_API_KEY`.
3.  `auth_method` deja de escribirse en `metrics_ingest_batches` y `metrics_ingest_logs`, porque ahora tendría siempre el mismo valor.
4.  El rollup mensual `metrics_auth` se conserva con su nombre y su forma, porque además lleva la salud del pipeline (`batches`, `with_dropped`, `with_sanitized`, `with_failures`). Sigue incrementando `appcheck` y `by_version.<versión>.appcheck`; desde este cambio, esos contadores equivalen a lotes aceptados. El campo `api_key` queda solo en los meses anteriores y la API ya no lo expone.

## Consecuencias

*   **Pérdida de datos aceptada.** Los dispositivos que sigan con una versión anterior a la 1.3.2 reciben 401 en cada envío y no vuelven a reportar hasta que actualicen. Sus agregados se pierden. Antes de este cambio no se consultó `metrics_auth` para saber cuántos son.
*   **Cómo medir el impacto.** Contar los eventos `ingest_unauthorized` en Cloud Logging, agrupados por `app_version`.
*   **Builds actuales, sin cambios.** Las versiones 1.3.2 en adelante ya autenticaban solo con App Check, incluidas las builds debug, que necesitan su token de debug registrado en la consola de Firebase.
*   **Dashboard.** Si el dashboard estaba configurado con `METRICS_API_KEY` en lugar de `READ_METRICS_API_KEY`, va a recibir 401 hasta que se cambie la clave en su configuración.
*   **Operación manual pendiente.** Deploy de las funciones (`npm run deploy`) y, después, borrar el secret `METRICS_API_KEY` de Secret Manager.
