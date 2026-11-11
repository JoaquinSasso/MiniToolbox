# Backlog

Solo trabajo **pendiente**, con evidencia verificable. Cuando un ítem se resuelve, su línea se borra en el mismo commit: el registro de lo hecho es el historial de git y el PR.

Formato: `- [ ] id: problema. Evidencia (archivo:línea). Criterio de hecho.`
Las líneas citan el estado de `master @ 58105f5` (2026-10-02). Antes de trabajar un ítem, verificá que siga vigente.

## P0: corregir antes del próximo release

- [ ] `backend-read-credential`: la lectura ya acepta solo `READ_METRICS_API_KEY`, pero esa clave no se rotó y `ingest` no tiene rate limiting. Evidencia: `isReadAuthorized` en `backend/functions/src/index.ts`; `ingest` no limita por origen. Hecho: clave de lectura rotada (con el dashboard reconfigurado) y rate limiting en `ingest`.

## P1: calidad de datos de telemetría

<<<<<<< HEAD
- [ ] `dashboard-metricsauth-rewrite`: el rewrite de `/api/metricsAuth` ya está en `dashboard/firebase.json`, pero falta desplegar Hosting (`firebase deploy --only hosting` desde `dashboard/`, manual) y comprobar que el panel de lotes muestra datos en el sitio desplegado. Antes del rewrite la ruta caía en `**` y devolvía `index.html`. Hecho: panel con datos en el sitio desplegado.
- [ ] `entry-source-leak`: si el deep link apunta a la herramienta en la que ya está el usuario (por ejemplo, tocar el widget de Agua con Agua abierta), `launchSingleTop` no cambia la ruta, el `LaunchedEffect` del NavGraph no corre y el origen queda pendiente: se atribuye a la próxima herramienta que se abra navegando. Evidencia: `MainActivity.kt:117,128`, `NavGraph.kt:79,87` (en `c165f35`; el arranque en frío ya no pasa por ese camino, solo `onNewIntent`). Hecho: el origen pendiente se descarta (o se consume) cuando la navegación no cambia de destino, con test.
=======
- [ ] `widget-shortcuts-mislabel`: se cuenta `widget_shortcuts` para cualquier deep link, incluidas las notificaciones. Evidencia: `MainActivity.kt:136`. Hecho: solo se cuenta si el origen es `MetricsSource.WIDGET` desde el widget de favoritos, y queda documentado en el glosario.
- [ ] `tool-count-per-route`: navegar entre subpantallas de una herramienta vuelve a contarla. Evidencia: `NavGraph.kt:94-112` compara `route`, no la herramienta. Hecho: se cuenta solo cuando cambia la herramienta, con un test.
- [ ] `about-as-tool`: `About` está registrada como herramienta y cuenta en `tools`. Evidencia: `ToolRegistry.kt:147-152`. Hecho: requiere decisión del autor (DECISIONS §4).
- [ ] `widget-optout-bypass`: quitar agua desde el widget llama a `AggregatesRepository.incrementWidgetUse` directamente, sin pasar por `isMetricsEnabled`, así que suma la métrica aunque el usuario haya desactivado las métricas (sumar agua sí lo respeta). Evidencia: `AguaCallback.kt:46` contra `AguaCallback.kt:26`. Hecho: ambos callbacks usan `widgetUse`, con un test que verifique el opt-out.
- [ ] `debug-traffic-in-prod`: las builds debug envían al endpoint de producción y el payload no lleva `build_type`. Evidencia: `MetricsConfig.kt`, `UploadMetricsWorker.kt:196-203`, `dev/DevInspector.kt:173-176`. Hecho: `build_type` en el payload y el backend descarta o separa `debug`.
- [ ] `uploader-transient-resend`: tras `MAX_ATTEMPTS` se limpia el lote sin `commitSent`, así que se reenvía con un `batch_id` nuevo (posible doble conteo) y `dropped_batches` reporta una pérdida que no ocurrió. Evidencia: `UploadMetricsWorker.kt:148-153`. Hecho: comportamiento definido y testeado en `UploadWorkerTest`.
- [ ] `dashboard-export`: el export Markdown omite `daily_active`, `tools_dau`, `tool_entry` y `retention`, y rotula `versions_first_seen` como "Instalaciones nuevas". Evidencia: `dashboard/public/index.html:271,622,1011-1032`. Hecho: export completo, con rótulos alineados al glosario.
- [ ] `dashboard-metricsauth-rewrite`: el dashboard pide `/api/metricsAuth`, pero Hosting no tiene ese rewrite y la ruta cae en `**` (devuelve `index.html`), así que el panel de lotes muestra "Sin datos" en producción. No se probó contra el sitio desplegado. Evidencia: `dashboard/public/index.html:799` contra `dashboard/firebase.json:5-30`. Hecho: rewrite agregado y panel con datos en el sitio desplegado.
>>>>>>> 45f0556 (docs(metrics): corregir el glosario contra el código y agregar metrics-glossary.en.md)

## P1: calidad de código, build y CI

- [ ] `lint-hidden-errors`: el baseline oculta errores (`LocalContextGetResourceValueCall` y `NonObservableLocale`) y tiene entradas obsoletas. Evidencia: `app/lint-baseline.xml`; se ve con lint sin baseline. Hecho: errores corregidos y baseline regenerado.
- [ ] `countries-main-thread`: el loader del dataset de países en `GuessFlagScreen` no cierra el `InputStream` ni cachea el resultado. Evidencia: `GuessFlagScreen.kt:275`. Hecho: un `CountriesRepository` con `Dispatchers.IO`, `.use {}` y caché.
- [ ] `baseline-profile-invalid`: el profile está escrito a mano y referencia clases inexistentes. Evidencia: `app/src/main/baseline-prof.txt:32-34`. Hecho: módulo `:baselineprofile` generado con Macrobenchmark y un número de arranque en frío medido. El build type `benchmark` y `scripts/measure-cold-start.sh` ya existen (`docs/arranque-en-frio-postmortem.md`).
- [ ] `backend-tests`: el backend no tiene tests, aunque `metrics-fixtures/keys.json:2` dice que los hay. Evidencia: `backend/functions/package.json` (sin script `test`). Hecho: vitest sobre `validate.ts` consumiendo `keys.json`, ejecutado en `backend.yml`.
- [ ] `startup-app-oncreate`: `MiniToolboxApp.onCreate` instala App Check, crea el canal de notificaciones y reprograma la alarma diaria de agua en el hilo principal en cada arranque, incluso cuando el proceso arranca por un receiver. Su costo no se midió. Evidencia: `utils/MiniToolboxApp.kt:25,34-35`. Hecho: una traza de Perfetto del arranque con la build `benchmark` que diga si vale la pena diferirlo y, si vale, el cambio con la medición antes y después.
- [ ] `sh-line-endings`: no hay `.gitattributes`, y con `core.autocrlf=true` `scripts/measure-cold-start.sh` puede quedar con CRLF al clonar, lo que rompe bash. Evidencia: `git ls-files .gitattributes` vacío. Hecho: `*.sh text eol=lf` en `.gitattributes`.
- [ ] `ci-release-build`: CI no compila release con R8. Evidencia: `.github/workflows/android.yml`. Hecho: un job `assembleRelease`.
- [ ] `coverage`: no hay medición de cobertura. Hecho: Kover con reporte en el CI.
- [ ] `app-scope`: hay 17 `CoroutineScope(...)` ad-hoc (`git grep -n "CoroutineScope(" -- app/src/main`, 2026-11-10; incluye el de `utils/ReceiverWork.kt`) y el `appScope` es privado. Evidencia: `utils/MiniToolboxApp.kt:23`. Hecho: un scope de aplicación inyectable, usado en receivers, widgets y métricas.
- [ ] `dependency-injection`: hooks `@VisibleForTesting` globales en lugar de DI. Evidencia: `metrics/Metrics.kt:61-90`. Hecho: Hilt (o un contenedor manual) y hooks eliminados.
- [ ] `agp10-optouts`: 7 flags que AGP 10 elimina, y plugin `kotlin-android` deprecado. Evidencia: `gradle.properties:26-34`. Hecho: Kotlin integrado en AGP y el build sin esos flags.
- [ ] `unused-deps-keep-rules`: dependencias sin uso directo y una regla `-keep` sobre un paquete entero. Evidencia: `app/build.gradle.kts:175,184,210`, `app/proguard-rules.pro:41`. Hecho: dependencias quitadas tras revisar `:app:dependencies` y reglas acotadas, con `assembleRelease` funcionando.
- [ ] `orphan-monetization-storage`: al quitar el modo PRO y los anuncios quedaron archivos huérfanos en los dispositivos que actualizan: SharedPreferences `pro_credits_prefs`, `interstitial_prefs` y `tool_usage_prefs`, y los DataStore `calculos_rapidos` y `pro_prefs`. No se leen ni se escriben, solo ocupan espacio. Evidencia (en `570c576`, antes de borrarlos): `utils/pro/CreditAccessManager.kt:35`, `utils/ads/InterstitialManager.kt:23`, `utils/ads/ToolUsageTracker.kt:8`, `utils/pro/ProRepository.kt:10`, `data/ProDataStore.kt:13`. Hecho: limpieza única al arrancar (`deleteSharedPreferences` y borrado de los `.preferences_pb`), con test en Robolectric.
- [ ] `protobuf-lite`: ejecutar el ADR `docs/decisions/protobuf-lite-migration.md`, que sigue pendiente.
- [ ] `release-tags`: no hay tags que liguen las versiones de Play a commits. Hecho: tag de la versión publicada actual y siguientes (skill `release`).
- [ ] `flaky-gasto-form-test`: `GastoFormViewModelTest` falla de forma intermitente con `IllegalStateException: Dispatchers.Main is used concurrently with setting it` en `resetMain`. Falló en 2 tests y pasó al re-ejecutar sin cambios (`.\gradlew.bat testDebugUnitTest`, 2026-10-31). Evidencia: `GastoFormViewModelTest.kt:50-53`. Hecho: el `tearDown` espera a que terminen las corrutinas del ViewModel antes de `resetMain` y el test pasa en 20 corridas seguidas.
- [ ] `mines-engine-invariant`: el motor no valida `mines <= celdas - zona segura`, y su test está con `@Ignore`. Evidencia: `MinesweeperEngine.kt:238-247`, `MinesEngineTest.kt:77`. Hecho: el motor valida y el test se reactiva.
- [ ] `auto-backup`: el manifest no declara `allowBackup` ni `dataExtractionRules`, y los XML de reglas son plantillas sin usar. Evidencia: `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`. Hecho: una decisión explícita sobre qué se respalda (como mínimo, excluir el estado de métricas).
- [ ] `ar-ruler-typo`: el nombre en inglés de la regla AR dice "Aumented Reality Ruler". Evidencia: `app/src/main/res/values/strings_tools.xml:18`. Hecho: "Augmented Reality Ruler".
- [ ] `accessibility`: los instrumentos dibujados con `Canvas` no tienen semántica. Evidencia: `git grep -n "semantics"` devuelve un solo uso (`ui/components/Stepper.kt:35`). Hecho: `semantics` en brújula, nivel, regla y lupa, verificado con TalkBack.


## Producto (requiere decisión del autor)

- [ ] **Recorte del catálogo.** Candidatas a fusionar: Azar (`selector_wheel`, `group_selector`). Rompe la continuidad de métricas: usar la skill `tool-catalog-change`.
- [ ] **Escáner QR y códigos de barras** (CameraX + ML Kit). Complementa el generador de QR y reutiliza CameraX.
- [ ] **Sonómetro** (`AudioRecord`, dBFS). Completa el grupo de instrumentos; requiere `RECORD_AUDIO` y documentar que no está calibrado.
- [ ] **Accesos directos de la app** (`ShortcutManagerCompat`) para Agua y Pomodoro. La fuente `MetricsSource.SHORTCUT` ya existe y nunca se emite.
- [ ] **Compartir resultados** desde la regla AR y el marcador de Truco. Hoy solo 3 pantallas usan `ACTION_SEND`.
- [ ] **Landscape** en la regla AR y el nivel de burbuja, vía `LockScreenOrientationIfAllowed`.
- [ ] **Medir el onboarding** de las herramientas de sensores (evento `onboarding_dismissed`), para saber si se lee.
- [ ] **Conversiones recientes en el conversor de unidades**, con botón para invertir (patrón de PCalc). Hoy el estado vive solo en `ConversorUnidadesViewModel` y se pierde al cerrar la app: requiere un repositorio con DataStore. Evidencia: `ConversorUnidadesViewModel.kt` no recibe repositorio. Hecho: lista de recientes persistida y testeada, con decisión del autor sobre cuántas guardar.
- [ ] **Capturas y GIFs del README.** Las de `Screenshots/` son anteriores a los últimos cambios de UI, y `menu.png`, `habitos.png` y `edad.png` muestran herramientas retiradas. Evidencia: `git log -1 -- Screenshots/` contra `git log -1` de cada pantalla. Hecho: capturas nuevas con los nombres listados en el comentario de la sección de capturas de `README.md`, y las viejas borradas.
