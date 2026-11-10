# MiniToolbox

[English](README.md)

[![Android CI](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/android.yml/badge.svg)](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/android.yml)
[![Backend CI](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/backend.yml/badge.svg)](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/backend.yml)
[![Google Play](https://img.shields.io/badge/Google%20Play-MiniToolbox-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.joasasso.minitoolbox)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple?logo=kotlin)](gradle/libs.versions.toml)
[![minSdk](https://img.shields.io/badge/minSdk-28-blue)](app/build.gradle.kts)

App nativa de Android que reúne 18 herramientas de uso cotidiano: instrumentos con sensores, una regla de realidad aumentada, un recordatorio de agua, un temporizador Pomodoro, un divisor de gastos y algunos juegos. Está escrita en Kotlin con Jetpack Compose, publicada en Google Play, y reporta su uso a través de un pipeline de telemetría propio (Cloud Functions, Firestore y un dashboard estático) en lugar de un SDK de analítica.

Lo interesante no son las herramientas en sí, sino lo que las rodea: trabajo en segundo plano que tiene que sobrevivir a la gestión de energía de Android, un pipeline de métricas diseñado para no identificar a nadie, y postmortems escritos de los bugs por los que pasaron ese pipeline y la alarma del Pomodoro.

## Herramientas

El catálogo está en [`ToolRegistry.kt`](app/src/main/java/com/joasasso/minitoolbox/tools/ToolRegistry.kt). La app está en inglés y en español.

| Categoría | Herramientas |
|---|---|
| Medir | Regla de realidad aumentada, regla, nivel burbuja, brújula, medidor de luz |
| Esenciales | Linterna, lupa, conversor de unidades (métrico e imperial) |
| Organizar | Temporizador Pomodoro, recordatorio de agua, divisor de gastos |
| Generar | Generador de QR, generador de equipos, selector de opciones |
| Jugar | Anotador de Truco, marcador, buscaminas, adivina la bandera |

Widgets de pantalla de inicio (Glance): progreso de agua (dos tamaños), linterna (dos tamaños) y herramientas favoritas.

El modo PRO con anuncios se quitó en [`35c84b6`](https://github.com/JoaquinSasso/MiniToolbox/commit/35c84b6): `master` no tiene anuncios ni compras dentro de la app. Las builds hasta la versión 1.4.0 todavía los incluyen.

## Lo destacado de la ingeniería

### Telemetría sin identificadores

Las métricas se agregan en el dispositivo y solo salen de él contadores diarios.

- **Cliente:** agregados diarios en DataStore ([`metrics/storage/`](app/src/main/java/com/joasasso/minitoolbox/metrics/storage)), enviados con WorkManager ([`metrics/uploader/`](app/src/main/java/com/joasasso/minitoolbox/metrics/uploader)).
- **Autenticación:** solo Firebase App Check (Play Integrity en las builds release). El endpoint de ingesta rechaza cualquier otra cosa ([ADR](docs/decisions/appcheck-only-ingest.md)).
- **Backend:** Cloud Functions en TypeScript ([`backend/functions/src/`](backend/functions/src)) que escriben en Firestore. Las [reglas de Firestore](backend/firestore.rules) niegan toda lectura y escritura directa.
- **Dashboard:** sitio estático en Firebase Hosting ([`dashboard/public/`](dashboard/public)) que lee a través de las Cloud Functions.
- **Privacidad:** no viaja ningún ID de dispositivo ni de usuario. Cada lote lleva un UUID aleatorio que solo sirve para deduplicar. La retención se calcula en el dispositivo, y el servidor recibe una de 25 categorías en lugar de un historial de uso.

Los límites también están documentados: con apenas un puñado de dispositivos activos por día, un agregado diario puede describir a una sola persona, así que "agregado" no equivale a "anónimo". El significado exacto y los sesgos conocidos de cada contador están en el [glosario de métricas](docs/metrics-glossary.md).

### Una alarma de Pomodoro que suena con la pantalla apagada

Alarma exacta de `AlarmManager`, después [`PomodoroAlarmReceiver`](app/src/main/java/com/joasasso/minitoolbox/tools/organizacion/pomodoro), y después un foreground service de tipo `mediaPlayback` que reproduce la alarma con Media3/ExoPlayer y abre una activity a pantalla completa. Un receiver de arranque reprograma las alarmas pendientes.

Llegar a eso implicó resolver seis problemas independientes apilados, cada uno tapando al siguiente. El [informe del diagnóstico](docs/pomodoro-background-alarm-fix.md) recorre cada síntoma, su causa y su arreglo, probado en un Pixel 8 con Android 17 usando `adb`, `dumpsys alarm` y Doze forzado.

### Postmortems

Cada uno cita el commit que introdujo o corrigió el problema.

- [La alarma del Pomodoro que sonaba pero no se veía](docs/pomodoro-alarma-postmortem.md): una sola llamada a `setSilent(true)` suprimía el heads-up y el full-screen intent.
- [Doble conteo de `app_open`](docs/metrics-double-count-postmortem.md): alrededor de +1 apertura por dispositivo y por día entre el 24/09/2025 y el 17/08/2026, corregido en `927ac8e`.
- [Bloqueo permanente del envío](docs/metrics-pipeline-blockage-postmortem.md): los dispositivos que abrían un Pomodoro desde una notificación dejaban de enviar métricas para siempre.

### Regla de realidad aumentada

ARCore con SceneView. La matemática de la medición está separada de la UI en `ArRulerMath`, con tests unitarios, y el estado de la pantalla vive en `ArRulerViewModel`.

## Qué dicen los datos de uso

Fuente: export Markdown del dashboard del 15/05/2026 al 10/11/2026 (180 días). Son contadores, no usuarios, y las builds debug reportan al mismo endpoint que producción, así que el dispositivo del autor está incluido.

- **Idioma.** El español es el idioma principal del sistema en el 87,3% de los dispositivos-día y el inglés en el 11,7% (418 dispositivos-día; `lang_primary` cuenta una vez por dispositivo y por día).
- **Una herramienta domina.** El recordatorio de agua concentra 740 de las 1407 interacciones con herramientas (52,6%). Esa proporción está inflada: antes de la v1.3.2, abrirlo desde una notificación o un widget se contaba dos veces.
- **Recorte del catálogo.** En octubre de 2026 ([#40](https://github.com/JoaquinSasso/MiniToolbox/pull/40)) se quitaron 14 herramientas de poco uso. Juntas suman 103 de las 1407 interacciones de este período (7,3%). Sus claves de métricas se conservan en el backend para que la serie histórica se siga pudiendo leer.

## Arquitectura

Un solo módulo de Gradle (`:app`), por decisión; ver [DECISIONS.md](DECISIONS.md). La descripción completa, con diagramas, está en [docs/architecture.md](docs/architecture.md).

- **UI:** Jetpack Compose con Material 3 (las APIs expresivas del BOM alpha de Compose), Navigation Compose con rutas type-safe y Glance para los widgets.
- **Estado:** MVVM, adoptado de forma progresiva. Diez `ViewModel` con `StateFlow` ya respaldan el recordatorio de agua, el divisor de gastos, el conversor de unidades, el buscaminas, la regla AR, el generador de equipos y el catálogo. Las herramientas más viejas todavía guardan el estado en el Composable y se migran cuando se las toca.
- **Persistencia:** Preferences DataStore detrás de repositorios. El dataset de países del juego de banderas es un binario Protobuf en `assets/` y se lee sin conexión.
- **Concurrencia:** corrutinas y Flow de Kotlin, WorkManager para los envíos.

## Tests y CI

- **296 tests unitarios en la JVM** (1 omitido), con `./gradlew testDebugUnitTest`. Usan JUnit 4, Robolectric donde hace falta un `Context` o DataStore, `kotlinx-coroutines-test` y Mockito. Cubren el pipeline de métricas, los ViewModels y los motores puros (buscaminas, liquidación de deudas, conversión de unidades, armado de equipos, matemática AR). Las convenciones están en [docs/testing.md](docs/testing.md).
- **Lo que no está cubierto:** no hay tests de UI ni instrumentados, no se mide la cobertura y el backend no tiene tests.
- **[Android CI](.github/workflows/android.yml):** tests unitarios, `assembleDebug` y `lintDebug` en cada push y pull request a `master`. El lint corre contra un baseline de issues heredados; los nuevos se corrigen, no se agregan.
- **[Backend CI](.github/workflows/backend.yml):** `npm ci`, `npm run lint`, `npm run build`.

## Limitaciones conocidas

La lista completa, con evidencia de archivo y línea, está en [docs/backlog.md](docs/backlog.md). Las principales:

- No hay inyección de dependencias; los tests reemplazan dependencias con hooks `@VisibleForTesting`.
- La app usa `protobuf-java` completo; la migración a Lite está planificada en un [ADR](docs/decisions/protobuf-lite-migration.md) pero no se hizo.
- Las builds debug envían métricas al endpoint de producción, y el payload no indica qué build lo envió.
- Los instrumentos dibujados con `Canvas` (brújula, nivel, regla, lupa) no tienen semántica de accesibilidad.
- El CI no compila un APK release, así que los problemas de R8 solo aparecen localmente.

## Compilar localmente

Requisitos: Android Studio y JDK 21 (el daemon de Gradle está fijado en 21 en `gradle/gradle-daemon-jvm.properties`).

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

La telemetría es opcional. Para habilitarla, copiá `local.properties.template` a `local.properties` y completá `METRICS_ENDPOINT`. Las builds debug además necesitan su token de debug de App Check registrado en la consola de Firebase. Sin el endpoint, la app funciona igual y no envía nada.

Backend, desde `backend/functions`:

```bash
npm ci && npm run lint && npm run build
```

## Estructura del repositorio

| Ruta | Contenido |
|---|---|
| `app/` | App Android (Kotlin, Compose) |
| `backend/` | Cloud Functions (TypeScript), reglas e índices de Firestore |
| `dashboard/` | Dashboard estático de métricas (Firebase Hosting) |
| `docs/` | Arquitectura, glosario de métricas, postmortems, ADRs, backlog |
| `metrics-fixtures/` | Contrato de claves de métricas, testeado por la app (el backend todavía no lo testea) |

## Capturas

Se están volviendo a sacar después de los últimos cambios de UI.

<!--
Archivos esperados en Screenshots/ (reemplazar por la tabla cuando existan):
home.png, water.png, pomodoro.png, ar_ruler.png, expense_splitter.png, minesweeper.png, bubble_level.png, widgets.png
-->

## Contacto

- LinkedIn: [Joaquín Sasso](https://www.linkedin.com/in/joasasso/)
- Google Play: [MiniToolbox](https://play.google.com/store/apps/details?id=com.joasasso.minitoolbox)

Las contribuciones son bienvenidas; ver [CONTRIBUTING.md](CONTRIBUTING.md). Licencia MIT ([LICENSE](LICENSE)).
