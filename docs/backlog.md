# Backlog

Solo trabajo **pendiente**, con evidencia verificable. Cuando un ítem se resuelve, su línea se borra en el mismo commit: el registro de lo hecho es el historial de git y el PR.

Formato: `- [ ] id: problema. Evidencia (archivo:línea). Criterio de hecho.`
Las líneas citan el estado de `master @ 58105f5` (2026-10-02). Antes de trabajar un ítem, verificá que siga vigente.

## P0: corregir antes del próximo release

- [ ] `metrics-key-basic-phrases`: el PR #29 cambió la clave `quotes` a `basic_phrases`, que el backend no conoce y guardaría como `other`. También se perdieron los favoritos guardados como `quotes`. Evidencia: `ToolRegistry.kt:194-201`, `backend/functions/src/index.ts:131,166-171,183-187`, `CategoriesScreen.kt:133`. Hecho: `metricsKey = "quotes"` en `BasicPhrases` (o la herramienta se quita, previa decisión), favoritos migrados o resueltos con `Screen.fromRouteString`, y un test que falle si alguna `metricsKey` no está en la lista de herramientas del backend.
- [ ] `backend-read-credential`: los endpoints de lectura aceptan también la credencial de escritura. Evidencia: `index.ts:847-853,900-901,959-960`. Hecho: la lectura solo acepta `READ_METRICS_API_KEY`, la clave de lectura se rotó y hay rate limiting en `ingest`.
- [ ] `requiresapi-notifications`: `@RequiresApi(TIRAMISU)` silencia el lint de API para toda la navegación (minSdk 28). Además, en API 28-32 se pide `POST_NOTIFICATIONS` (probablemente muestra "denegado" en Agua). Evidencia: `MainActivity.kt:44`, `NavGraph.kt:77`, `PomodoroScreen.kt:82,113-117`, `AguaReminderScreen.kt:92,307-313`, `PomodoroAlarmActivity.kt:123`. Hecho: sin esas anotaciones, el permiso se pide solo con `SDK_INT >= 33` y `lintDebug` pasa.
- [ ] `receiver-uncaught-exceptions`: hay `try/finally` sin `catch` en scopes ad-hoc dentro de receivers; una excepción mata el proceso. Evidencia: `PomodoroAlarmReceiver.kt:149-155`, `ResetAguaReceiver.kt:16-24`, `AguaNotification.kt:26-33`. Hecho: `catch` con log que re-lanza `CancellationException`.

## P1: calidad de datos de telemetría

- [ ] `widget-shortcuts-mislabel`: se cuenta `widget_shortcuts` para cualquier deep link, incluidas las notificaciones. Evidencia: `MainActivity.kt:136`. Hecho: solo se cuenta si el origen es `MetricsSource.WIDGET` desde el widget de favoritos, y queda documentado en el glosario.
- [ ] `tool-count-per-route`: navegar entre subpantallas de una herramienta vuelve a contarla. Evidencia: `NavGraph.kt:101-117` compara `route`, no la herramienta. Hecho: se cuenta solo cuando cambia la herramienta, con un test.
- [ ] `about-as-tool`: `About` está registrada como herramienta y cuenta en `tools`. Evidencia: `ToolRegistry.kt:258-263`. Hecho: requiere decisión del autor (DECISIONS §4).
- [ ] `debug-traffic-in-prod`: las builds debug envían al endpoint de producción y el payload no lleva `build_type`. Evidencia: `MetricsConfig.kt`, `UploadMetricsWorker.kt:196-203`, `dev/DevInspector.kt:173-176`. Hecho: `build_type` en el payload y el backend descarta o separa `debug`.
- [ ] `uploader-transient-resend`: tras `MAX_ATTEMPTS` se limpia el lote sin `commitSent`, así que se reenvía con un `batch_id` nuevo (posible doble conteo) y `dropped_batches` reporta una pérdida que no ocurrió. Evidencia: `UploadMetricsWorker.kt:148-153`. Hecho: comportamiento definido y testeado en `UploadWorkerTest`.
- [ ] `dashboard-export`: el export Markdown omite `daily_active`, `tools_dau`, `tool_entry` y `retention`, y rotula `versions_first_seen` como "Instalaciones nuevas". Evidencia: `dashboard/public/index.html:271,622,1011-1032`. Hecho: export completo, con rótulos alineados al glosario.
- [ ] `glossary-mismatch`: el glosario dice que `versions` equivale a `app_open`, pero el código lo cuenta una vez por día. Además, `app_open` no cuenta "al reiniciar". Evidencia: `metrics-glossary.md:10,32` contra `AggregatesRepository.kt:121-129` y `MainActivity.kt:67-71`. Hecho: glosario corregido.

## P1: calidad de código, build y CI

- [ ] `lint-hidden-errors`: el baseline oculta 25 errores (20 `LocalContextGetResourceValueCall` y 5 `NonObservableLocale`) y tiene unas 67 entradas obsoletas. Evidencia: `app/lint-baseline.xml`; se ve con lint sin baseline. Hecho: errores corregidos y baseline regenerado.
- [ ] `countries-main-thread`: el dataset de países se lee y parsea en el main thread, y hay tres loaders duplicados que no cierran el stream. Evidencia: `CountriesInfoScreen.kt:60-62`, `GuessCapitalScreen.kt:314`, `GuessFlagScreen.kt:275`. Hecho: un `CountriesRepository` con `Dispatchers.IO` y caché.
- [ ] `baseline-profile-invalid`: el profile está escrito a mano y referencia clases inexistentes. Evidencia: `app/src/main/baseline-prof.txt:12-13,32-34`. Hecho: módulo `:baselineprofile` generado con Macrobenchmark y un número de arranque en frío medido.
- [ ] `backend-tests`: el backend no tiene tests, aunque `metrics-fixtures/keys.json:2` dice que los hay. Evidencia: `backend/functions/package.json` (sin script `test`). Hecho: vitest sobre `validate.ts` consumiendo `keys.json`, ejecutado en `backend.yml`.
- [ ] `ci-release-build`: CI no compila release con R8. Evidencia: `.github/workflows/android.yml`. Hecho: un job `assembleRelease`.
- [ ] `coverage`: no hay medición de cobertura. Hecho: Kover con reporte en el CI.
- [ ] `app-scope`: hay 11 `CoroutineScope(...)` ad-hoc y el `appScope` es privado. Evidencia: `utils/MiniToolboxApp.kt:23`, `git grep -n "CoroutineScope("`. Hecho: un scope de aplicación inyectable, usado en receivers, widgets y métricas.
- [ ] `dependency-injection`: hooks `@VisibleForTesting` globales en lugar de DI. Evidencia: `metrics/Metrics.kt:61-90`. Hecho: Hilt (o un contenedor manual) y hooks eliminados.
- [ ] `agp10-optouts`: 7 flags que AGP 10 elimina, y plugin `kotlin-android` deprecado. Evidencia: `gradle.properties:26-34`. Hecho: Kotlin integrado en AGP y el build sin esos flags.
- [ ] `unused-deps-keep-rules`: dependencias sin uso directo y reglas `-keep` sobre paquetes enteros (una de ellas, AppLovin, ni siquiera es dependencia). Evidencia: `app/build.gradle.kts:175,184,210`, `app/proguard-rules.pro:24,28,34,41`. Hecho: dependencias quitadas tras revisar `:app:dependencies` y reglas acotadas, con `assembleRelease` funcionando.
- [ ] `protobuf-lite`: ejecutar el ADR `docs/decisions/protobuf-lite-migration.md`, que sigue pendiente.
- [ ] `release-tags`: no hay tags que liguen las versiones de Play a commits. Hecho: tag de la versión publicada actual y siguientes (skill `release`).
- [ ] `dead-code`: archivos sin referencias. Evidencia: `data/BMIDataStore.kt`, `data/RachasDataStore.kt`, `data/EventsDataStore.kt`, `data/vCardDataStore.kt`, `Metrics.kt:168` (`versionHeartbeat`), `assets/Country.proto`, `assets/countries_dataset_multilanguage.pb` (idéntico a `countries_dataset.pb`), `res/values*/noise_generator_strings.xml`, y el import de `emptyList` de protobuf en `CategoriesScreen.kt:72`. Hecho: borrados y build verde.
- [ ] `mines-engine-invariant`: el motor no valida `mines <= celdas - zona segura`, y su test está con `@Ignore`. Evidencia: `MinesweeperEngine.kt:238-247`, `MinesEngineTest.kt:77`. Hecho: el motor valida y el test se reactiva.
- [ ] `auto-backup`: el manifest no declara `allowBackup` ni `dataExtractionRules`, y los XML de reglas son plantillas sin usar. Evidencia: `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`. Hecho: una decisión explícita sobre qué se respalda (como mínimo, excluir el estado de métricas).
- [ ] `accessibility`: los instrumentos dibujados con `Canvas` no tienen semántica. Evidencia: `git grep -n "semantics"` devuelve un solo uso (`ui/components/Stepper.kt:35`). Hecho: `semantics` en brújula, nivel, regla y lupa, verificado con TalkBack.

## P2: documentación

- [ ] `readme-refresh`: el README está desactualizado o es inexacto. Evidencia: badge de Kotlin 2.2.20 (línea 8) contra 2.4.20 en `libs.versions.toml:5`, `METRICS_API_KEY` (línea 74) eliminado en `a95191b`, cifras de Play (líneas 96-97), "faltan tests" (línea 116), "hilos persistentes" del Pomodoro (línea 38), y la tabla de Protobuf medida con Lite sin benchmark en el repo (líneas 20-28). Hecho: README en inglés con cifras verificables y versión en español aparte.
- [ ] `architecture-doc`: `docs/architecture.md` se verificó por última vez el 23/08/2026. Lista ViewModels y limitaciones que ya cambiaron (líneas 28, 194, 201, 203, 206). Hecho: diagramas y conteos al día, con fecha y commit de verificación.
- [ ] `performance-doc`: `docs/performance_tests.md` atribuye las mediciones a Protobuf Lite, pero la app usa `protobuf-java`. Hecho: benchmark reproducible en el repo y documento regenerado.

## Producto (requiere decisión del autor)

- [ ] **Recorte del catálogo.** Candidatas a quitar: `zodiac_sign`, `multiverse_me`, `basic_phrases`, `quick_calcs`. Candidatas a fusionar: Azar (`coin_flip`, `dice`, `selector_wheel`, `group_selector`), Calculadoras (`percentage`, `decimal_binary`, `unit_converter`) y Países (`countries_info`, `guess_flag`, `guess_capital`). Rompe la continuidad de métricas: usar la skill `tool-catalog-change`.
- [ ] **Escáner QR y códigos de barras** (CameraX + ML Kit). Complementa el generador de QR y reutiliza CameraX.
- [ ] **Sonómetro** (`AudioRecord`, dBFS). Completa el grupo de instrumentos; requiere `RECORD_AUDIO` y documentar que no está calibrado.
- [ ] **Accesos directos de la app** (`ShortcutManagerCompat`) para Agua y Pomodoro. La fuente `MetricsSource.SHORTCUT` ya existe y nunca se emite.
- [ ] **Compartir resultados** desde la regla AR y el marcador de Truco. Hoy solo 3 pantallas usan `ACTION_SEND`.
- [ ] **Landscape** en la regla AR y el nivel de burbuja, vía `LockScreenOrientationIfAllowed`.
- [ ] **Medir el onboarding** de las herramientas de sensores (evento `onboarding_dismissed`), para saber si se lee.
- [ ] **GIFs del README.** Pendientes hasta cerrar los cambios de UI.
