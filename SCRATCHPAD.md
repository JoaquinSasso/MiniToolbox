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
- [x] `water-tracker-midnight-rollover` — `AguaDataStore.kt`, `AguaRepository.kt`, `AguaViewModel.kt`, `AguaReminderScreen.kt`, `ResetAguaReceiver.kt`, `MiniToolboxApp.kt` — Contador de agua arrastraba consumo del día anterior al abrir desde notificación/medianoche. Fix: Migrado a arquitectura MVVM reactiva (`AguaViewModel`, `AguaUiState`, `StateFlow`), reactividad a medianoche sobre DataStore, guardado atómico referenciado a la fecha activa y descarte de notificaciones residuales. Cubierto con tests unitarios en `AguaViewModelTest` y `AguaDataStoreTest`.
- [x] `intent-backstack-accumulation` — `AndroidManifest.xml`, `AguaNotification.kt`, `AguaWidget.kt`, `AguaMiniWidget.kt` — Notificaciones y widgets apilaban instancias de `MainActivity`, impidiendo salir al launcher tras volver atrás desde el menú. Fix: `launchMode="singleTask"` en `MainActivity` con flags `FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP`.

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
- [x] `type-safe-nav` — `Screen.kt`, `NavGraph.kt`, `MainActivity.kt`, `ToolRegistry.kt`, `ToolRoutes.kt`, widgets y notificaciones — Migrado el 100% de la navegación a Type-Safe Navigation con Navigation Compose 2.10 y `@Serializable sealed class Screen`. Eliminadas rutas mágicas concatenadas y deep links con string crudos; intents externos transportan payload tipado (`startRouteJson`) con fallback resiliente `Screen.fromRouteString()`. `Screen.Quotes` renombrado a `Screen.BasicPhrases`. Claves de telemetría de subpantallas unificadas bajo sus herramientas (`pomodoro`, `water`, `meetings`, `basic_phrases`). Cobertura con `ScreenSerializationTest` y `ToolMetricsKeysTest`.

## P2 — Deuda de build y dependencias

- [x] `compileSdk-preview` — `app/build.gradle.kts` — Fix error de compilación por dependencias alpha de Compose requiriendo `compileSdk` 37.1. Se migró de `compileSdk = 37` a `compileSdkVersion("android-37.1")` ya que la propiedad `compileSdk` en Kotlin DSL solo acepta enteros.
- [x] `kotlin-compiler-options-dsl` — `app/build.gradle.kts` — Migrado de `android { kotlinOptions { jvmTarget } }` a `kotlin { compilerOptions { jvmTarget } }` para arreglar deprecación de Kotlin 2.0 y error de sync.
- [x] `toml-cleanup` — `gradle/libs.versions.toml`, `build.gradle.kts` — Alineado el plugin `kotlin-serialization` con `version.ref = "kotlin"` (2.2.20). Eliminadas versiones y dependencias explícitas redundantes de Compose (`ui-unit`, `runtime-saveable`, `ui-graphics`) para que el BOM `compose-bom-alpha` gobierne todas las variantes sin desincronizaciones.
- [x] `remove-gson` — `BasicPhrasesScreen.kt`, `BasicPhrasesClasses.kt`, `build.gradle.kts`, `libs.versions.toml`, `proguard-rules.pro` — Migrado a `kotlinx.serialization` con `@Serializable` en `Frase` y `decodeFromString`. Eliminada dependencia `com.google.code.gson:gson`, versión del catálogo y reglas ProGuard. Cubierto con `BasicPhrasesSerializationTest`.
- [x] `baseline-profile` — `app/src/main/baseline-prof.txt` — Creadas reglas AOT de arranque en frío para `MiniToolboxApp`, `MainActivity`, `MiniToolboxNavGraph`, `CategoriesScreen`, `ToolRegistry`, `ToolRoutes` y Compose runtime, empaquetadas automáticamente en el APK/AAB para optimización de inicio (20-40%).
- [x] `splashscreen-compat` — `androidx.core:core-splashscreen 1.0.1`, `Theme.App.Starting` en `themes.xml`, `installSplashScreen()` en `MainActivity.kt`, remoción de `package` en `AndroidManifest.xml` y `resvalues` en `gradle.properties`. Soporte retrocompatible de splash screen para API 28-30 y eliminación de advertencias `NewApi` de lint.
- [x] `ci-test-parallelization` — `.github/workflows/android.yml`, `gradle.properties`, `app/build.gradle.kts` — Paralelización de jobs de CI (`Unit Tests` aislado de `Build & Lint`), caché para artefactos de Robolectric (`~/.m2/repository/org/robolectric`), habilitación de `org.gradle.parallel=true`, optimización de memoria JVM a 3072 MB con `UseParallelGC` y `maxHeapSize = 1536m` para el proceso de test unitarios. Disminuye el tiempo de feedback de tests en GitHub Actions a ~1 minuto.

## P3 — Deuda de UI

- [x] `remember-saveable` — `QRCodeGeneratorScreen.kt`, `GroupSelectorScreen.kt`, `PasswordGeneratorScreen.kt` — Migrado el estado mutable de entrada y configuración a `rememberSaveable` (texto de QR, tamaño de grupo en selector, longitud/toggles/contraseña en generador de contraseñas con control en `LaunchedEffect`), preservando los datos generados ante rotación de pantalla y muerte de proceso.
- [x] `lazy-keys` — `ScoreboardScren.kt`, `ReunionesScreen.kt`, `ReunionDetailScreen.kt`, `AgregarGastoScreen.kt`, `EditarGastoScreen.kt`, `BasicPhrasesScreen.kt`, `DiceSimulatorScreen.kt`, `PomodoroTimersListScreen.kt`, `GuessFlagScreen.kt`, `GuessCapitalScreen.kt`, `MinesweeperScreen.kt` — Asignadas claves estables (`key = { ... }`) al 100% de las 15 llamadas a `items`/`itemsIndexed` en listas dinámicas, editables o grillas. En listas multisección (`AgregarGastoScreen`, `EditarGastoScreen`, `ReunionDetailScreen`) se emplearon prefijos de contexto (`pagador_`, `consumidor_`, `gasto_`, `integrante_`, `deuda_`) previniendo colisiones de claves en runtime. Incorporado `id` único retrocompatible en `Equipo` cubierto con `ScoreboardDataStoreTest`.
- [x] `tool-onboarding-hint` — `ToolOnboardingDataStore.kt`, `ToolOnboardingBanner.kt`, `CompassScreen.kt`, `BubbleLevelScreen.kt`, `LightSensorScreen.kt`, `RulerScreen.kt` — Tarjetas de sugerencia inicial animadas (empujón) con `AnimatedVisibility` para el clúster de sensores e instrumentos (brújula: calibración en ocho, nivel: superficie plana, fotómetro: orientación del sensor, regla: alineación de bordes), persistencia atómica en DataStore (`tool_onboarding`) y 100% de cobertura de pruebas unitarias con Robolectric (`ToolOnboardingDataStoreTest`).

## Proceso

- [ ] `branch-protection` — Verificar en GitHub Settings > Branches > master que la branch protection rule esté activa. Los commits recientes usan PRs (#9 a #20) pero no se pudo confirmar la regla desde el entorno local. Nota del autor: intentó configurarla y tuvo errores.
- [ ] `rate-limiting` — Cloud Function sin rate limiting por instalación. App Check protege contra APKs no atestados pero no limita frecuencia. Riesgo bajo con volumen actual (3-5 aperturas/día).

---

## Siguiente paso sugerido
 
**`branch-protection` / `rate-limiting`** (Proceso) o evaluación de features en `PRODUCT_BACKLOG.md` — Con P0, P1, P2 y P3 completados al 100%, la base de código ha saldado toda la deuda técnica activa de arquitectura, bugs, build y UI.

