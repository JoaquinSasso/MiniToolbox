# MiniToolbox: reglas para agentes

App Android (Kotlin, Jetpack Compose, módulo único `:app`), publicada en Google Play. Tiene telemetría propia: Firebase Cloud Functions en TypeScript, Firestore y un dashboard estático. Es el proyecto principal del portfolio del autor, así que la calidad del código y la honestidad de la documentación pesan tanto como la funcionalidad.

Este archivo se carga en todas las conversaciones. Los procedimientos largos viven en skills (`.agents/skills/`) y se cargan solo cuando hacen falta.

## Restricciones del proyecto

@[Decisiones de arquitectura](DECISIONS.md)

## Comandos (Windows, PowerShell, desde la raíz)

| Para qué | Comando |
|---|---|
| Verificación completa (lo mismo que corre el CI) | `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` |
| Un solo test | `.\gradlew.bat testDebugUnitTest --tests "*NombreDelTest"` |
| Build release con R8 (si tocaste dependencias, ProGuard o reflexión) | `.\gradlew.bat assembleRelease` |
| Backend (desde `backend/functions`) | `npm ci; npm run lint; npm run build` |

- El backend **no tiene tests**: no existe `npm test`. No digas que los corriste.
- El lint usa `app/lint-baseline.xml`. **Nunca agregues issues nuevos al baseline** para hacer pasar el build: corregilos.
- Si Gradle falla por la JVM, el daemon necesita JDK 21 (`gradle/gradle-daemon-jvm.properties`). El de Android Studio está en `C:\Program Files\Android\Android Studio\jbr`.

## Flujo de trabajo

1. **Una unidad por vez.** Si te piden varias cosas, elegí una, decí por qué y cerrala. Las demás quedan anotadas en una línea cada una. Si la tarea no entra en una sesión, decilo y proponé un recorte.
2. **Rama antes de tocar código:** `git checkout master; git pull; git checkout -b <tipo>/<descripcion>`, con tipo `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci` o `build`. Una rama por unidad que se pueda revertir sola.
3. **Leé el código real antes de opinar o editar.** No supongas firmas, nombres de clases ni rutas de archivo.
4. **Verificá** con los comandos de arriba. Si cambiaste lógica, agregá o actualizá tests.
5. **Commit** en formato Conventional Commits (`tipo(alcance): descripción`). **No hagas push ni abras PRs** salvo que el autor lo pida.
6. **Reporte final:** rama, archivos tocados, qué comando corriste y su resultado (por ejemplo, "174 tests, 0 fallas"), qué **no** verificaste y lo que queda pendiente.

## Código nuevo o refactorizado

- **MVVM.** El estado y la lógica van en un `ViewModel` que expone `StateFlow`. La UI lo consume con `collectAsStateWithLifecycle()`. Los eventos de un solo disparo van por `Channel`. Para el procedimiento completo, usá la skill `mvvm-screen`.
- **Persistencia.** DataStore solo detrás de un repositorio. Nada de `preferencesDataStore` ni `.edit {}` dentro de un Composable. Las mutaciones de lectura y escritura van dentro de un único `edit {}`, para que sean atómicas.
- **Corrutinas.** No crees `CoroutineScope(...)` nuevos: usá `viewModelScope`. Si hace falta un scope de proceso, el lugar es el `appScope` de `MiniToolboxApp` (hoy es privado: exponerlo es un ítem del backlog). En un `BroadcastReceiver`, `goAsync()` con `catch` explícito; `try/finally` sin `catch` mata el proceso si algo falla. Re-lanzá siempre `CancellationException`. La lectura de assets y el I/O van en `Dispatchers.IO`, nunca dentro de un `LaunchedEffect` sin cambiar de dispatcher.
- **Compatibilidad de API (minSdk 28).** No uses `@RequiresApi` para silenciar el lint: chequeá `Build.VERSION.SDK_INT` en el punto exacto. `POST_NOTIFICATIONS` solo se pide en API 33 o superior.
- **Compose.** Los textos van con `stringResource`, no con `LocalContext.current.getString`. Toda lista dinámica lleva `key = {}`, con prefijo de contexto si hay varios bloques `items()` en el mismo contenedor.
- **Strings.** Todo texto visible va en `res/values` (inglés) y en `res/values-es`.

## Documentación honesta (regla estricta)

- No escribas en ningún archivo una afirmación que no verificaste en esta sesión: nada de "100% de cobertura" (no hay herramienta de cobertura), mejoras de performance sin un benchmark en el repo, ni "toda la deuda saldada".
- Las fechas, números y causas de los postmortems se citan con su hash de commit o el comando que los reproduce.
- Sin rutas absolutas de tu máquina en archivos versionados: siempre rutas relativas al repo.
- Si cambiás algo que contradice `docs/architecture.md` o `docs/metrics-glossary.md`, actualizalos en el mismo commit.

## Backlog

`docs/backlog.md` lista solo lo **pendiente**, con evidencia `archivo:línea`.

- Antes de empezar, fijate si la tarea ya está ahí.
- Al terminar un ítem, **borrá la línea** en el mismo commit. El historial de git y el PR son el registro; no marques ítems como hechos.
- Lo que encuentres de paso se agrega como ítem nuevo, con evidencia. No lo arregles sin preguntar.
- Si te pasan una auditoría o un informe externo, verificá cada hallazgo contra el código actual antes de pasarlo al backlog.

## Cómo comunicarte

- Español. Empezá por las fallas y los riesgos, después lo que funciona. Si una idea es mala, decilo en la primera frase.
- Explicá el porqué y nombrá el costo de cada decisión, junto con las alternativas que descartaste.
- Si te equivocaste, decilo directo y corregí. Si te cuestionan sin un dato nuevo, sostené tu postura con argumentos.
- Distinguí los errores que introdujiste vos de los que ya existían en el repo.
- Prosa breve; tablas solo para comparar. Sin emojis.

## Skills del proyecto (`.agents/skills/`)

| Skill | Cuándo usarla |
|---|---|
| `metrics-pipeline` | Antes de tocar cualquier cosa de métricas: cliente, backend, dashboard o fixtures |
| `tool-catalog-change` | Al agregar, quitar, renombrar o fusionar una herramienta |
| `mvvm-screen` | Al migrar una pantalla a ViewModel o crear una herramienta nueva |
| `release` | Al preparar una versión para Google Play |

## Trampas conocidas

- **BOM alpha.** El BOM de Compose es alpha (`compose-bom-alpha`) a propósito, por la API expresiva de Material3. Todas las librerías de Compose tienen que salir del BOM: mezclar versiones produce `AbstractMethodError` en runtime.
- **Builds debug y producción.** Las builds debug envían métricas al mismo endpoint que producción. No dispares eventos de prueba (`dev/DevInspector.kt`) contra producción.
- **Tests con ViewModel.** En tests de ViewModel, cancelá `viewModelScope` en `@After` (ver `AguaViewModelTest` y el commit `3bacff0`). Si no, las corrutinas se filtran entre tests.
