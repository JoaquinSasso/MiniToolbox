# ADR 003: Protección contra replay de App Check en la ingesta

*   **Estado:** Aceptada. Por ahora solo en modo registro: los tokens reusados se aceptan.
*   **Fecha:** 2026-11-23
*   **Decisión del autor:** aprobó el plan del ítem `backend-read-credential` del backlog, que pedía rate limiting en `ingest`.

## Contexto

Desde el ADR 002 (`appcheck-only-ingest.md`), `ingest` acepta un lote solo si trae un token de App Check válido. Pero ese token se puede reusar mientras sigue vigente (alrededor de una hora). Quien lo extraiga de un dispositivo atestado puede mandar lotes en bucle durante esa ventana, y cada lote con un `batch_id` nuevo suma a los agregados.

`ingest` no limitaba los envíos por origen, y la forma habitual de hacerlo, un contador por IP, exige guardar IPs. Eso choca con `DECISIONS.md` §3 (sin identificadores persistentes).

## Decisión

1.  **Cliente.** `HttpMetricsUploader` y `DevInspector` piden un token de uso limitado (`FirebaseAppCheck.limitedUseAppCheckToken`) en cada envío, en lugar del token cacheado de `getAppCheckToken(false)`.
2.  **Backend.** `verifyAppCheck` valida el token sin consumirlo, que es una verificación local del JWT, y después lo consume con `verifyToken(token, { consume: true })`. Un token que ya se había consumido se registra como `appcheck_replay` en Cloud Logging, con la `app_version` declarada y si se rechazó.
3.  **Transición.** `REJECT_REPLAYED_APPCHECK` en `backend/functions/src/index.ts` arranca en `false`: los tokens reusados se registran, pero se aceptan. Las versiones publicadas antes de este cambio reusan el token cacheado, así que rechazar desde el primer día les cortaría envíos legítimos. Pasar a `true` es un cambio de código hecho a propósito.
4.  **Falla del consumo.** Si la llamada de consumo falla (permisos de la cuenta de servicio, cuota o red), el lote se acepta y se registra `appcheck_consume_failed`. El token ya pasó la validación: un problema de infraestructura no debe cortar toda la ingesta.
5.  **Techo de costo.** `ingest` se despliega con `maxInstances: 10`. Esto no limita por origen, solo acota el gasto. El valor no sale de una medición del tráfico.

## Consecuencias

*   **Límite por origen sin identificadores.** Con el rechazo activado, cada lote exige un token nuevo, y cada token exige pasar por Play Integrity en un dispositivo atestado. El servidor no guarda nada del origen.
*   **Lo que no cubre.** Un dispositivo atestado puede seguir mandando un lote con valores inflados: `validate.ts` no acota los contadores ni la cantidad de items por lote (backlog: `ingest-value-bounds`). Tampoco cubre la clave de lectura, que se rota aparte.
*   **Costo por envío.** Cada lote suma una llamada al backend de App Check y una atestación de Play Integrity en el dispositivo, que consume la cuota diaria del proyecto. Ninguno de los dos costos se midió.
*   **Clientes viejos.** Mientras el rechazo siga desactivado, no cambia nada para ellos. Una vez activado, el segundo envío dentro de la vigencia del token recibe 401, que el cliente trata como `AuthError`: conserva el lote y lo reintenta en el próximo disparo con un token nuevo, así que no se pierden datos.
*   **Criterio para activar el rechazo.** Contar `appcheck_replay` en Cloud Logging, agrupado por `app_version`. Activarlo cuando los replays vengan solo de versiones anteriores a este cambio y en una proporción aceptable. Si aparecen replays de versiones nuevas, el cliente tiene un bug o hay abuso, y conviene investigarlo antes.
*   **Sin verificar.** No se probó contra el App Check real: ni el consumo, ni los permisos que necesita la cuenta de servicio de Cloud Functions. Después del deploy hay que buscar `appcheck_consume_failed` en Cloud Logging. Si aparece, la protección no está funcionando, aunque la ingesta siga aceptando lotes.
