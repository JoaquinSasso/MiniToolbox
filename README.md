# MiniToolbox

[Español](README.es.md)

[![Android CI](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/android.yml/badge.svg)](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/android.yml)
[![Backend CI](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/backend.yml/badge.svg)](https://github.com/JoaquinSasso/MiniToolbox/actions/workflows/backend.yml)
[![Google Play](https://img.shields.io/badge/Google%20Play-MiniToolbox-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.joasasso.minitoolbox)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple?logo=kotlin)](gradle/libs.versions.toml)
[![minSdk](https://img.shields.io/badge/minSdk-28-blue)](app/build.gradle.kts)

A native Android app that bundles 18 everyday tools: sensor instruments, an AR ruler, a water reminder, a Pomodoro timer, an expense splitter and a few games. It is written in Kotlin with Jetpack Compose, published on Google Play, and reports usage through a telemetry pipeline I built myself (Cloud Functions, Firestore and a static dashboard) instead of an analytics SDK.

The interesting parts are not the tools themselves but what is around them: background work that has to survive Android's power management, a privacy-first metrics pipeline, and written postmortems of the bugs that pipeline and the Pomodoro alarm went through.

## Tools

The catalog lives in [`ToolRegistry.kt`](app/src/main/java/com/joasasso/minitoolbox/tools/ToolRegistry.kt). The app ships in English and Spanish.

| Category | Tools |
|---|---|
| Measure | AR ruler, ruler, bubble level, compass, light meter |
| Essentials | Flashlight, magnifier, unit converter (metric and imperial) |
| Organize | Pomodoro timer, water reminder, expense splitter |
| Generate | QR generator, team generator, option selector |
| Play | Truco scoreboard, scoreboard, minesweeper, guess the flag |

Home-screen widgets (Glance): water progress (two sizes), flashlight (two sizes) and favorite tools.

The ad-supported PRO mode was removed in [`35c84b6`](https://github.com/JoaquinSasso/MiniToolbox/commit/35c84b6); `master` has no ads and no in-app purchases. Builds up to version 1.4.0 still include them.

## Engineering highlights

### Telemetry without identifiers

Usage metrics are aggregated on the device and only daily counters leave it.

- **Client:** daily aggregates in DataStore ([`metrics/storage/`](app/src/main/java/com/joasasso/minitoolbox/metrics/storage)), uploaded by WorkManager ([`metrics/uploader/`](app/src/main/java/com/joasasso/minitoolbox/metrics/uploader)).
- **Authentication:** Firebase App Check only (Play Integrity in release builds). The ingest endpoint rejects anything else ([ADR](docs/decisions/appcheck-only-ingest.md)).
- **Backend:** Cloud Functions in TypeScript ([`backend/functions/src/`](backend/functions/src)) writing to Firestore. The [Firestore rules](backend/firestore.rules) deny all direct reads and writes.
- **Dashboard:** static site on Firebase Hosting ([`dashboard/public/`](dashboard/public)) that reads through the Cloud Functions.
- **Privacy:** no device or user ID is sent. Each batch carries a random UUID used only for deduplication. Retention is bucketed on the device, so the server receives one of 25 categories instead of a usage history.

The limits are documented too: with only a handful of active devices per day, a daily aggregate can describe a single person, so "aggregated" does not mean "anonymous". The exact meaning and known biases of each counter are in the [metrics glossary](docs/metrics-glossary.en.md).

### Pomodoro alarm that rings with the screen off

`AlarmManager` exact alarm, then [`PomodoroAlarmReceiver`](app/src/main/java/com/joasasso/minitoolbox/tools/organizacion/pomodoro), then a `mediaPlayback` foreground service that plays the alarm with Media3/ExoPlayer and opens a full-screen activity. A boot receiver reschedules pending alarms.

Getting there took six independent problems stacked on top of each other, each one hiding the next. The [diagnosis write-up](docs/pomodoro-background-alarm-fix.md) covers each symptom, root cause and fix, tested on a Pixel 8 running Android 17 with `adb`, `dumpsys alarm` and forced Doze.

### Postmortems

Each one cites the commit that introduced or fixed the problem.

- [Pomodoro alarm that rang but was not shown](docs/pomodoro-alarma-postmortem.md): a single `setSilent(true)` call suppressed both the heads-up and the full-screen intent.
- [Double count of `app_open`](docs/metrics-double-count-postmortem.md): about +1 open per device per day between 2025-09-24 and 2026-08-17, fixed in `927ac8e`.
- [Permanent upload blockage](docs/metrics-pipeline-blockage-postmortem.md): devices that opened a Pomodoro from a notification stopped sending metrics for good.

### AR ruler

ARCore with SceneView. The measurement math is extracted from the UI into `ArRulerMath` and covered by unit tests, and the screen state lives in `ArRulerViewModel`.

### Bubble level from quaternions

The [bubble level](app/src/main/java/com/joasasso/minitoolbox/tools/herramientas/instrumentos/BubbleLevelScreen.kt) reads `TYPE_ROTATION_VECTOR`, a quaternion, converts it to a rotation matrix and takes the device's "up" vector from it. It does not use the Euler angles from `getOrientation()`, which hit gimbal lock when the phone stands upright. The same vector tells the level whether the phone is lying flat, in portrait or in landscape, and it switches mode on its own.

### Offline countries dataset

The flag game reads its 206 countries from a 23 KB Protobuf binary in `assets/`, with no network access. The dataset was first parsed as JSON (Moshi, then Gson) and moved to Protobuf in `03cdf3f`. The load-time comparison in [docs/performance_tests.md](docs/performance_tests.md) is not quoted here because it was measured with Protobuf Lite, while the app uses full `protobuf-java`, and there is no benchmark in the repo to reproduce it.

## What the usage data says

Source: dashboard Markdown export for 2026-05-15 to 2026-11-10 (180 days). These are counters, not users, and debug builds report to the same endpoint as production, so the author's own device is included.

- **Language.** Spanish is the primary system language in 87.3% of device-days and English in 11.7% (418 device-days; `lang_primary` counts once per device per day).
- **One tool dominates.** The water reminder accounts for 740 of 1,407 tool interactions (52.6%). That share is inflated: before v1.3.2, opening it from a notification or widget was counted twice.
- **Catalog cut.** In October 2026 ([#40](https://github.com/JoaquinSasso/MiniToolbox/pull/40)) 14 low-usage tools were removed. Together they account for 103 of the 1,407 interactions in this window (7.3%). Their metric keys are kept in the backend so the historical series still reads.

## Architecture

Single Gradle module (`:app`) by design; see [DECISIONS.md](DECISIONS.md). The full description, with diagrams, is in [docs/architecture.en.md](docs/architecture.en.md).

- **UI:** Jetpack Compose with Material 3 (expressive APIs from the alpha Compose BOM), Navigation Compose with type-safe routes, and Glance for widgets.
- **State:** MVVM is being adopted progressively. Ten `ViewModel`s exposing `StateFlow` already back the water reminder, expense splitter, unit converter, minesweeper, AR ruler, team generator and the catalog. Older tools still keep state inside the Composable and are migrated when touched.
- **Persistence:** Preferences DataStore behind repositories. The countries dataset for the flag game is a Protobuf binary in `assets/`, read offline.
- **Concurrency:** Kotlin coroutines and Flow, WorkManager for uploads.

## Testing and CI

- **296 JVM unit tests** (1 skipped), run with `./gradlew testDebugUnitTest`. They use JUnit 4, Robolectric where a `Context` or DataStore is needed, `kotlinx-coroutines-test` and Mockito. They cover the metrics pipeline, ViewModels, and the pure engines (minesweeper, debt settlement, unit conversion, team partitioning, AR math). Conventions are in [docs/testing.md](docs/testing.md).
- **Not covered:** there are no UI or instrumented tests, no coverage measurement, and the backend has no tests.
- **[Android CI](.github/workflows/android.yml):** unit tests, `assembleDebug` and `lintDebug` on every push and pull request to `master`. Lint runs against a baseline of inherited issues; new ones are fixed, not added to it.
- **[Backend CI](.github/workflows/backend.yml):** `npm ci`, `npm run lint`, `npm run build`.

## Known limitations

The full list, with file and line evidence, is in [docs/backlog.md](docs/backlog.md). The main ones:

- No dependency injection; tests swap dependencies through `@VisibleForTesting` hooks.
- The app uses full `protobuf-java`; the migration to Lite is planned in an [ADR](docs/decisions/protobuf-lite-migration.md) but not done.
- Debug builds send metrics to the production endpoint on purpose (the author's everyday build is a debug build), and the payload does not say which build sent it. See the [glossary](docs/metrics-glossary.en.md).
- The instruments drawn on `Canvas` (compass, level, ruler, magnifier) have no accessibility semantics.
- CI does not build a release APK, so R8 problems only show up locally.

## Building locally

Requirements: Android Studio and JDK 21 (the Gradle daemon is pinned to 21 in `gradle/gradle-daemon-jvm.properties`).

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Telemetry is optional. To enable it, copy `local.properties.template` to `local.properties` and set `METRICS_ENDPOINT`. Debug builds also need their App Check debug token registered in the Firebase console. Without the endpoint the app works the same and sends nothing.

Backend, from `backend/functions`:

```bash
npm ci && npm run lint && npm run build
```

## Repository layout

| Path | Contents |
|---|---|
| `app/` | Android app (Kotlin, Compose) |
| `backend/` | Cloud Functions (TypeScript), Firestore rules and indexes |
| `dashboard/` | Static metrics dashboard (Firebase Hosting) |
| `docs/` | Architecture, metrics glossary, postmortems, ADRs, backlog |
| `metrics-fixtures/` | Metric key contract, tested by the app (the backend does not test it yet) |

## Screenshots

Being retaken after the latest UI changes.

<!--
Expected files in Screenshots/ (replace the table when they exist):
home.png, water.png, pomodoro.png, ar_ruler.png, expense_splitter.png, minesweeper.png, bubble_level.png, widgets.png
-->

## Contact

- LinkedIn: [Joaquín Sasso](https://www.linkedin.com/in/joasasso/)
- Google Play: [MiniToolbox](https://play.google.com/store/apps/details?id=com.joasasso.minitoolbox)

Contributions are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md). MIT licensed ([LICENSE](LICENSE)).
