# SCRATCHPAD.md

> **Actualizado:** 2026-09-04 | **Base:** master @ e939132
> **Fuente:** cruce de `docs/auditoria_opus.md` contra código real.

<!-- Agentes: actualizar status al avanzar. Agregar hallazgos nuevos al final de cada sección. -->
<!-- Status: [ ] pendiente | [/] en progreso | [x] hecho | [—] descartado -->
<!-- Decisiones de producto van en PRODUCT_BACKLOG.md, no acá. -->

---

## P0 — Bugs activos

- [ ] `goAsync-receivers` — `PomodoroAlarmReceiver.kt`, `ResetAguaReceiver.kt` — Corrutinas en `onReceive()` sin `goAsync()`. Causa raíz documentada de alarmas que no suenan (ver `docs/pomodoro-alarma-postmortem.md`). Fix: `goAsync()` + `pendingResult.finish()` al completar. Dos archivos, lógica intacta.
- [ ] `double-money` — `ExpensesDataStore.kt:27` — Dinero almacenado como `Double`. Aritmética de punto flotante pierde centavos en cálculo de deudas.

## P1 — Deuda de arquitectura

### Corrutinas y errores

- [ ] `catch-swallowing` — 33+ bloques `catch (_: Exception)` / `catch (_: Throwable)`. Los de `Throwable` en corrutinas tragan `CancellationException`, rompen cancelación cooperativa. Archivos clave: `Metrics.kt`, `BillingClientWrapper.kt`, `ExpensesDataStore.kt`.
- [ ] `manual-coroutine-scopes` — ~11 `CoroutineScope(...)` manuales, solo 2 con `SupervisorJob`. En `Metrics.kt:io {}` un fallo en una métrica cancela el scope entero.

### Divisor de gastos

- [ ] `calcular-deudas-untestable` — `ReunionDetailScreen.kt:546` — `calcularDeudas(reunion, context)` mezcla lógica de negocio con presentación. Recibe `Context`, devuelve `List<String>` formateados.
- [ ] `flow-snapshots` — `ReunionDetailScreen.kt:97`, `AgregarGastoScreen.kt:92`, `EditarGastoScreen.kt:91` — Snapshots con `firstOrNull()` sobre Flows reactivos. Pierde reactividad, puede mostrar datos stale.

### AR Ruler

- [ ] `ar-ruler-vm` — `ArRulerSceneViewScreen.kt` (1.078 líneas) — `ARulerVM` es clase privada con `remember {}`, no un `ViewModel` real. Estado no sobrevive recreación de Activity. Funciones puras (`dist3()`, `commitDraft()`, `chooseTickStep()`) intesteables.

### Navegación

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

**`goAsync-receivers`** — Único ítem con bug de producto confirmado y postmortem existente. Fix quirúrgico en dos archivos. Después: `catch-swallowing` en los mismos archivos (mismo tema: corrutinas mal manejadas).
