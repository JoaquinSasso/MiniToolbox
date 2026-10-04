# Decisiones de arquitectura

Restricciones vigentes de MiniToolbox. Cambiar cualquiera requiere una decisión explícita del autor, registrada como ADR en `docs/decisions/`.

## 1. Estructura y UI

- **Módulo único (`:app`).** Es una elección deliberada para el tamaño actual del proyecto. No se modulariza sin un ADR que lo justifique.
- **UI nativa.** Kotlin y Jetpack Compose con Material 3, y Glance para los widgets. Las APIs de Views son excepciones puntuales (`res/layout/view_ar_label.xml` para AR, un `AlertDialog` de AppCompat en `TrucoBoardScreen.kt`) y no se agregan nuevas.
- **MVVM progresivo.** El código nuevo y el refactorizado ponen el estado y la lógica en un `ViewModel` con `StateFlow`, consumido con `collectAsStateWithLifecycle()`. Muchas herramientas antiguas todavía guardan el estado en el Composable: se migran cuando se las toca, no en bloque.
- **Tests de la lógica.** Todo ViewModel, repositorio o motor de cálculo nuevo o modificado lleva tests unitarios. Se usa Robolectric donde hace falta un `Context` o DataStore, y `kotlinx-coroutines-test` para corrutinas.

## 2. Pipeline de métricas

- **Local:** agregados diarios en DataStore (`metrics/storage/`).
- **Envío:** WorkManager (`metrics/uploader/`), autenticado con Firebase App Check. El backend todavía acepta API key para clientes anteriores a la 1.3.2, y esa rama se retira cuando el rollup `metrics_auth` muestre 0 tráfico por API key.
- **Backend:** Firebase Cloud Functions, exclusivamente en TypeScript (`backend/functions/src/`), con escritura en Firestore. Las reglas de Firestore niegan todo acceso directo.
- **Visualización:** dashboard estático en Firebase Hosting (`dashboard/public/`), que lee a través de las Cloud Functions.

## 3. Privacidad

- **Sin identificadores persistentes.** No viaja ningún ID de dispositivo ni de usuario. El `batch_id` es un UUID aleatorio por lote y sirve solo para deduplicar.
- **Cálculo en el dispositivo.** Si una métrica nueva requiere seguir a un individuo en el tiempo, se descarta o se calcula en el dispositivo y solo viaja la categoría agregada (como hace `RetentionBuckets`).
- **Límite conocido.** Con pocos usuarios activos por día, un agregado diario puede corresponder a una sola persona. "Agregado" no equivale a "anónimo", y la documentación no debe afirmar lo contrario.

## 4. Continuidad de los datos históricos

- **Las claves son un contrato.** Los identificadores de métricas no se renombran: renombrar una clave parte la serie en dos.
- **La clave de una herramienta es `Tool.metricsKey`.** Por defecto coincide con `Screen.route`. Si cambia la ruta, se fija `metricsKey` con el valor anterior en `ToolRegistry`. `ToolMetricsKeysTest` congela el conjunto de claves.
- **El backend tiene su propia lista de herramientas conocidas** (`TOOL_ROUTE_MAP` y `EXTRA_KNOWN_TOOLS` en `backend/functions/src/index.ts`). Una clave que no figure ahí se guarda como `other`. Agregar una herramienta exige actualizar ambos lados.
- **Aprobación explícita.** Cualquier cambio que rompa la continuidad, como quitar o fusionar herramientas o cambiar la semántica de un contador, necesita la aprobación del autor y una nota en `docs/metrics-glossary.md`.

## 5. Documentación honesta

- **Los sesgos y limitaciones se documentan,** aunque el resultado sea incómodo. Van en `docs/metrics-glossary.md`, en los postmortems o en los ADRs.
- **Todo dato publicado es reproducible.** Una cifra en el README o en `docs/` (rendimiento, uso, cobertura) tiene que poder reproducirse con un commit, un comando o un dato crudo. Si no se puede, no se publica.
