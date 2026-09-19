# SCRATCHPAD.md

> **Actualizado:** 2026-09-04 | **Base:** master @ e939132
> **Fuente:** cruce de `docs/auditoria_opus.md` contra código real.

<!-- Agentes: actualizar status al avanzar. Agregar hallazgos nuevos al final de cada sección. -->
<!-- Status: [ ] pendiente | [/] en progreso | [x] hecho | [—] descartado -->
<!-- Decisiones de producto van en PRODUCT_BACKLOG.md, no acá. -->

---

## P0 — Bugs activos

- [x] `goAsync-receivers` — `PomodoroAlarmReceiver.kt`, `ResetAguaReceiver.kt`, `AguaNotification.kt`, `PomodoroActionReceiver.kt`, `PomodoroBootReceiver.kt` — Corrutinas en `onReceive()` sin `goAsync()`. Fix: `goAsync()` + `pendingResult.finish()` al completar todas las tareas asíncronas.
- [x] `double-money` — `ExpensesDataStore.kt`, `DebtEngine.kt` — Dinero modelado y calculado en centavos enteros (`Long`). Residuo distribuido determinísticamente; 0 centavos perdidos.

## P1 — Deuda de arquitectura

### Corrutinas y errores

- [x] `catch-swallowing` — Eliminada captura indiscriminada de `Throwable` en todo el código Kotlin (0 ocurrencias restantes). En corrutinas y funciones suspendidas (`MetricsUploader.kt`, `AggregatesRepository.kt`, widgets de linterna), `CancellationException` se propaga limpiamente preservando la cancelación cooperativa. En DataStores y utilidades, se capturan excepciones tipadas (`IOException`, `SerializationException`, `JSONException`, `ParseException`).
- [x] `manual-coroutine-scopes` — CoroutineScopes manuales estandarizados con `SupervisorJob()`, `CoroutineExceptionHandler` y manejo de excepciones en `Metrics.kt`, `FavoriteToolsWidget.kt` (con `goAsync()`), `BillingClientWrapper.kt`, `AguaReminderScreen.kt` y `PomodoroAlarmReceiver.kt`.

### Divisor de gastos

- [x] `calcular-deudas-untestable` — `DebtEngine.kt`, `ReunionDetailScreen.kt` — Lógica de deudas extraída a `DebtEngine` puro sin dependencias de `Context`/Android, cubierto con `DebtEngineTest`.
- [x] `flow-snapshots` — `ReunionDetailScreen.kt`, `AgregarGastoScreen.kt`, `EditarGastoScreen.kt`, `ReunionesScreen.kt`, `ExpensesDataStore.kt` — Migrado a MVVM con `ReunionesViewModel`, `ReunionDetailViewModel`, `GastoFormViewModel` y operaciones transaccionales atómicas en DataStore dentro de `edit {}`. Cero `firstOrNull()`, flujos `StateFlow` con `collectAsStateWithLifecycle()`, canales de eventos de un solo disparo y 100% de cobertura de tests unitarios (Robolectric + CoroutinesTest).

### AR Ruler

- [x] `ar-ruler-vm` — `ArRulerModels.kt`, `ArRulerMath.kt`, `ArRulerViewModel.kt`, `ArRulerSceneViewScreen.kt` — Migrado a MVVM canónico con `ArRulerViewModel` y `StateFlow` consumido mediante `collectAsStateWithLifecycle()`. Desacoplado de ARCore nativo con reconciliación declarativa de `Anchor` en la vista. Motor matemático puro y filtros extraídos a `ArRulerMath` con 100% de cobertura de tests unitarios (`ArRulerMathTest` y `ArRulerViewModelTest`) en JVM.

### Navegación

- [x] `nav-back-animation` — `NavGraph.kt`, `ArRulerSceneViewScreen.kt`, `MagnifierScren.kt` — Animación de retroceso / predictive back sin reducción de escala (no shrinking / scale-to-center). Transición horizontal fluida de ancho completo (100% scale) con curva `FastOutSlowInEasing`, efecto paralaje sutil y eliminación de `BackHandler` incondicionales que interceptaban eventos forzando el fallback de ventana.
- [ ] `type-safe-nav` — `NavGraph.kt` (478 líneas, ~45 rutas manuales por string) — Sin type-safe navigation. Prerrequisitos cumplidos: Navigation Compose 2.10 + kotlinx.serialization. Migrar elimina limitación conocida #4 (`quotes` != `basic_phrases`).

## P2 — Deuda de build y dependencias

- [ ] `toml-cleanup` — `gradle/libs.versions.toml` — Aliases redundantes de Compose. Tres `version.ref` explícitos (`ui-unit` 1.9.1, `runtime-saveable` 1.9.1, `ui-graphics` 1.9.3) que el BOM debería fijar. Plugin `kotlin-serialization` con versión `2.2.10` hardcodeada en vez de `version.ref = "kotlin"` (2.2.20).
- [ ] `remove-gson` — `BasicPhrasesScreen.kt` — Único consumidor de Gson. El resto usa `kotlinx.serialization`. Migrar elimina dependencia + reglas ProGuard.
- [ ] `baseline-profile` — Sin `baseline-prof.txt` ni módulo macrobenchmark. App con 33 herramientas y arranque a catálogo; mejora típica 20-40% en arranque en frío.
- [ ] `splashscreen-compat` — `core-splashscreen` en catálogo pero no en `build.gradle.kts`. `installSplashScreen()` no se llama. Atributos `windowSplashScreen*` son API 31 nativa, no funcionan en API 28-30 (minSdk=28). Warnings suprimidos en lint-baseline.

## P3 — Deuda de UI

- [ ] `remember-saveable` — ~300+ `remember {}` vs ~22 `rememberSaveable`. En herramientas donde el usuario acumula trabajo (divisor de gastos, pomodoro, marcador de truco, QR, selector de grupos), el estado se pierde si el sistema mata el proceso. Priorizar: generador de equipos (58 usos), generador de QR (41 usos).
- [ ] `lazy-keys` — ~5 de 20 llamadas a `items(...)` con `key =`. Sin clave, estado se reasigna por posición en listas editables (favoritos, gastos, tareas, timers).

## Proceso

- [ ] `branch-protection` — Verificar en GitHub Settings > Branches > master que la branch protection rule esté activa. Los commits recientes usan PRs (#9 a #20) pero no se pudo confirmar la regla desde el entorno local. Nota del autor: intentó configurarla y tuvo errores.
- [ ] `rate-limiting` — Cloud Function sin rate limiting por instalación. App Check protege contra APKs no atestados pero no limita frecuencia. Riesgo bajo con volumen actual (3-5 aperturas/día).

---

## Siguiente paso sugerido
 
**`type-safe-nav`** (P1) — `NavGraph.kt` (381 líneas, ~45 rutas manuales por string) — Migrar a rutas type-safe con `@Serializable` objects/classes usando Navigation Compose 2.10, eliminando rutas mágicas y unificando el contrato de navegación.

