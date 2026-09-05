# Auditoría técnica completa — MiniToolbox

**Repositorio:** `JoaquinSasso/MiniToolbox` (`master`, commit `5a9b9a4`) · **Fecha de revisión:** 25 de agosto de 2026
**Alcance:** clon completo, 135 archivos Kotlin (22.837 LOC en `main`, 549 en `test`), backend TypeScript, dashboard, documentación, configuración de build y estado real del CI vía API de GitHub.

**Nota metodológica:** el sandbox no tiene Android SDK, así que no pude ejecutar Gradle. Donde una afirmación depende de ejecución, lo digo y dejo el comando de verificación. El estado del CI sí lo consulté contra la API real de GitHub Actions, no lo inferí.

**Fuera de alcance por pedido tuyo:** los GIFs del README. Sé que están pendientes por decisión y no por olvido, y que dependen de cerrar los cambios de UI/UX primero.

---

## 0. Cierre de Fases 0 y 1 — verificación

| # | Corrección | Estado | Evidencia |
|---|---|---|---|
| C1 | `.artifacts/` y rutas de Windows | ✅ | `git grep "C:/Users"` vacío; `.artifacts/` fuera del índice; `docs/decisions/protobuf-lite-migration.md` creado |
| C2 | Regresión de `UploadConfig` | ✅ | `git grep "UploadConfig"` vacío; `MetricsConfig` como fuente única |
| C3 | Fechas del postmortem y dashboard | ✅ | Postmortem: 17/08/2026, rango 24/09/2025–17/08/2026, commit `927ac8e`. Banner corregido |
| C4 | Número de usuarios del README | ✅ | "+115 descargas orgánicas (Google Play Console)". Elegiste el dato conservable y verificable |
| C5 | `StaticFieldLeak` | ✅ | `metricsRepoFactory` como factory; baseline bajó de 229 a 226 issues |
| C6 | Correcciones de `architecture.md` | ✅ | 16 en `data/`; `GW -->\|widgetUse\| MET`; commit y fecha de verificación actualizados |
| C7 | Branch protection y PRs | ❌ | Los 20 runs de CI son `push`. Cero PRs desde el #4 |
| — | GIFs | ⏸️ | Pendiente por decisión |

**Extra no pedido y bien hecho:** `docs/metrics-glossary.md`. Documenta la semántica exacta de cada contador, incluyendo la advertencia de que `versions_first_seen` no mide usuarios únicos y la marca de sesgo histórico en `app_open`. Es contenido de portfolio por sí mismo: prácticamente ningún proyecto personal documenta la semántica de sus propias métricas.

**Corrección que me debo a mí mismo:** en la auditoría original conté 19 strings sin traducir al español. Es un falso positivo mío: los 13 que faltan son IDs de AdMob, de billing y la URL de política de privacidad. No son texto de interfaz. La cobertura de traducción ES/EN está completa. Lo único que corresponde es marcarlos `translatable="false"` para que dejen de contarse.

---

## 1. Hallazgos críticos

### 🔴 A1. `master` está en rojo, y el test que lo tumba afirma lo contrario de lo correcto

El run del último commit (`5a9b9a4`, el rediseño de la regla AR) falla. El paso que rompe es **Run Unit Tests**, y `Lint` queda en `skipped` como consecuencia.

El culpable es `app/src/test/.../metrics/MetricsTest.kt`:

```kotlin
appOpen(context)
dailyOpenOnce(context)

verify(mockRepo, never()).incrementAppOpen()      // ← esto es falso
verify(mockRepo, timeout(5000)).incrementDailyActive()
```

`appOpen()` **sí llama** a `incrementAppOpen()` — es literalmente su única responsabilidad (`Metrics.kt:83-88`). El test asegura que no lo hace.

¿Por qué pasó alguna vez? Porque `io { }` lanza sobre `CoroutineScope(Dispatchers.IO)`, fire-and-forget. `verify(never())` se evalúa de inmediato; si la corrutina de IO todavía no corrió, el aserto pasa. Es una **carrera pura**: el test verde no significa que el código esté bien, significa que el planificador fue lento. En los últimos runs falló tres veces y pasó seis, sobre commits que no tocan métricas.

Dos problemas, no uno:

1. **El aserto es incorrecto.** Lo que había que verificar es que `dailyOpenOnce` NO incrementa `app_open` — que es exactamente lo que hace el segundo test del archivo. El primero duplica el propósito y le agrega un aserto falso sobre `appOpen`.
2. **La suite tiene concurrencia no determinista.** Instalar `UnconfinedTestDispatcher` en `Dispatchers.Main` no controla `Dispatchers.IO`, que es donde corre el código bajo prueba. Ese es el error de diseño de fondo.

Por qué es crítico y no importante: un badge de CI en rojo en el README de un portfolio es peor que no tener CI. Comunica que el proyecto tiene verificación automática **y que está fallando y a nadie le importa**. Un revisor que abre el repo hoy ve eso antes que cualquier otra cosa.

**Fix inmediato:** borrar el aserto falso del primer test. **Fix correcto:** hacer inyectable el dispatcher de `Metrics.kt` (un `internal var metricsDispatcher: CoroutineDispatcher = Dispatchers.IO`, en la misma línea que las costuras que ya existen) para que el test use `StandardTestDispatcher` y controle el avance del tiempo virtual en vez de esperar con `timeout(5000)`.

### 🔴 A2. Seguís pusheando directo a `master`

Los 20 runs de Actions son `push` o `workflow_dispatch`. Ninguno es `pull_request`, aunque los dos workflows tienen el trigger configurado. Los últimos PRs son el #1 al #4, todos cerrados hace tiempo.

Esto no es un detalle de proceso: es la causa directa de A1. Con branch protection exigiendo el check de `Android CI`, el commit de la regla AR no habría podido entrar a `master` con la suite en rojo. El CI está detectando problemas **después** de que ya son públicos, que es la mitad de su valor.

Es lo más barato de toda esta auditoría — quince minutos en Settings → Branches — y es lo que hace que todo el trabajo de la Fase 1 realmente rinda.

---

## 2. Hallazgos importantes

### 🟠 B1. La arquitectura sigue sin capa de dominio, y el divisor de gastos sigue igual

Es la deuda que ya conocés, la reconocés en el README, y la ubicaste bien en Fase 2. La menciono para dejar el estado exacto medido hoy:

- **2 ViewModels reales** (`CategoryViewModel`, `MinesViewModel`) para 32 herramientas.
- **322 `remember { }` contra 7 `rememberSaveable`.** Esa proporción importa más de lo que parece: significa que casi todo el estado de la app se pierde si el sistema mata el proceso en background. En herramientas efímeras da igual; en el divisor de gastos, el pomodoro o el marcador de truco, no.
- `calcularDeudas(reunion, context)` sigue en `ReunionDetailScreen.kt:546`, recibiendo `Context` y devolviendo `List<String>` ya formateados. Sigue sin ser testeable.
- Los tres archivos del divisor (`ReunionDetailScreen`, `AgregarGastoScreen`, `EditarGastoScreen`) siguen tomando snapshots con `firstOrNull()` sobre un `Flow` reactivo, en las líneas 97, 91 y 91.
- Dinero sigue en `Double` (`ExpensesDataStore.kt:27`).
- Sin inyección de dependencias. 20+ `object` globales con estado.

Nada de esto empeoró. Simplemente sigue ahí y sigue siendo lo que más pesa en una evaluación técnica.

### 🟠 B2. La regla AR se rediseñó bien, pero reintrodujo el patrón que estamos tratando de eliminar

El commit `5a9b9a4` reescribe 1.123 líneas y el resultado es notoriamente mejor que lo anterior: modos Segmento y Polilínea, anclas relativas (un solo `Anchor` por medición con puntos en coordenadas locales — decisión correcta y no obvia, porque reduce la deriva de tracking), captura diferida al siguiente frame porque el `HitResult` solo es válido dentro del frame que lo generó, `FLAG_KEEP_SCREEN_ON` con liberación en `onDispose`, y ARCore declarado como opcional en el manifiesto para no romper dispositivos sin soporte.

Los comentarios del código son de buena calidad. Este, por ejemplo, documenta una trampa real de Kotlin/JVM:

> `// OJO: no llamar a esta función setMode. La propiedad var mode ya genera un setMode() en la JVM y las firmas colisionan.`

El problema es estructural:

```kotlin
private class ARulerVM { ... }
// ...
val vm = remember { ARulerVM() }
```

Se llama `VM` pero **no es un `ViewModel`**: es una clase suelta retenida con `remember`, dentro de un archivo de 1.007 líneas que también contiene el `Composable`, el callback de frame, la proyección 3D→2D, el dibujo en `Canvas`, el `LabelPainter` y el diálogo de ayuda.

Consecuencias concretas:

- **El estado no sobrevive a la recreación de la Activity.** Hoy se salva porque la app fuerza orientación vertical, pero cualquier recreación por presión de memoria borra las mediciones. En una herramienta donde el usuario acumula trabajo (varias mediciones sobre una habitación), perderlo es un fallo de producto, no un detalle técnico.
- **Nada de esto es testeable.** `dist3()`, la lógica de `commitDraft`/`undo`, y `chooseTickStep()` son funciones puras que se testearían en minutos si estuvieran fuera del archivo de UI.
- **Es exactamente el patrón que el README declara estar migrando.** Un revisor que vea el commit más reciente del repo y encuentre una clase llamada `VM` que no es un ViewModel va a concluir que la migración declarada no está ocurriendo.

Es la herramienta con más potencial de portfolio de toda la app y el trabajo de rediseño ya está hecho. Extraer `ARulerVM` a un `ViewModel` de verdad y sacar la geometría a un archivo aparte es medio día y convierte esto en tu segunda pieza ejemplar después del buscaminas.

### 🟠 B3. El `build.gradle.kts` sigue con dependencias duplicadas — y ahora sé exactamente cuáles

Verifiqué el catálogo resolviendo alias contra artefactos. Hay **tres artefactos con más de un alias apuntándolos**:

| Artefacto | Alias que lo declaran | Versiones |
|---|---|---|
| `androidx.compose.foundation:foundation` | `androidx-foundation`, `foundation`, `androidx-compose-foundation-foundation` | BOM, `1.9.1`, `1.9.3` |
| `androidx.compose.material3:material3` | `material3`, `androidx-material3` | BOM, `1.5.0-alpha06` |
| `androidx.compose.runtime:runtime` | `androidx-runtime`, `runtime` | `1.9.1`, `1.9.3` |

Los seis están declarados simultáneamente en `dependencies { }`. Además `androidx.camera.view` aparece dos veces, y hay cuatro versiones hardcodeadas fuera del catálogo (`appcompat:1.7.1`, `fragment-ktx:1.8.9`, `media:1.7.1`, `work-runtime-ktx:2.10.5`).

Lo importante no es el ruido, es que **cada `version.ref` explícito anula el BOM**. Tenés un `composeBom = "2025.10.00"` cuyo trabajo es alinear versiones, y al lado tres versiones distintas de Compose forzadas a mano, incluyendo un `material3 = "1.5.0-alpha06"` en un build de producción. Gradle resuelve al mayor, así que probablemente funcione — pero es una resolución accidental, no una decisión.

Cualquier revisor que sepa Gradle abre este archivo y en diez segundos ve que el catálogo de versiones se está usando como lista de imports.

**Bonus barato:** `gson` está declarado como dependencia y se usa en **un solo archivo** (`BasicPhrasesScreen.kt`). Todo el resto del proyecto usa `kotlinx.serialization`. Migrar ese archivo elimina una dependencia entera.

### 🟠 B4. El endpoint de ingesta sigue sin protección real

La clave ya no está en el repositorio (Fase 0), pero sigue embebida en el APK y el backend sigue sin:

- **Firebase App Check** con atestación de Play Integrity. Cero coincidencias en todo el repo.
- **Rate limiting por instalación** en la Cloud Function. Cero coincidencias.

Una clave dentro de un APK es extraíble con `apktool` en dos minutos. Hoy cualquiera que descargue tu app puede escribir en tu Firestore, y las métricas son el argumento central de tu portfolio.

No lo pongo en críticos porque el riesgo práctico con tu volumen actual es bajo y porque App Check es una tarea de Fase 2 con dependencias. Pero conviene que esté en el roadmap escrito, no en la memoria.

### 🟠 B5. Manejo de errores: 40 de 66 `catch` siguen tragando la excepción

`catch (_: Throwable)` y `catch (_: Exception)` en 40 de 66 bloques. Además de ocultar fallas, capturar `Throwable` dentro de corrutinas se traga la `CancellationException`, que es el mecanismo por el que las corrutinas se cancelan.

Combinado con lo siguiente: hay **8 usos de `CoroutineScope(...)` creado a mano** fuera de la UI, de los cuales solo dos usan `SupervisorJob`:

| Archivo | Línea | Tiene `SupervisorJob` |
|---|---|---|
| `metrics/Metrics.kt` | 80 | ❌ |
| `PomodoroAlarmReceiver.kt` | 97 | ✅ |
| `PomodoroAlarmReceiver.kt` | 122, 142, 198 | ❌ |
| `AguaReminderScreen.kt` | 586 | ❌ |
| `ResetAguaReceiver.kt` | 12 | ❌ |
| `BillingClientWrapper.kt` | 131 | ❌ |
| `FavoriteToolsWidget.kt` | 156, 168 | ❌ |
| `ProSilentInitializer.kt` | 23 | ✅ |

Los de `BroadcastReceiver` son especialmente delicados: lanzar una corrutina desde `onReceive()` sin `goAsync()` significa que el proceso puede morir antes de que el trabajo termine. En el receiver del pomodoro eso se traduce en una alarma que a veces no suena — que es precisamente la clase de bug que ya documentaste en el postmortem.

### 🟠 B6. Módulo único de 22.837 líneas

Sin cambios. `NavGraph.kt` tiene 348 líneas y 42 `composable()` escritos a mano. Sigue siendo la mejora con mejor relación señal/esfuerzo del proyecto para una evaluación técnica.

---

## 3. Propuestas de mejora de implementación

Esto no son errores: son formas mejores o más modernas de hacer lo que ya hacés.

### 💡 M1. Navegación type-safe — resuelve dos problemas de una vez

Estás en Navigation Compose **2.9.5**, que soporta rutas tipadas con `kotlinx.serialization` (que ya tenés como dependencia). En vez de:

```kotlin
composable(Screen.ArRuler.route) { ArRulerSceneViewScreen(...) }
toolUse(context, route)                    // el ID analítico ES la ruta
```

pasarías a:

```kotlin
@Serializable data object ArRuler : ToolRoute
composable<ArRuler> { ArRulerSceneViewScreen(...) }
```

Por qué importa acá específicamente: **elimina la clase entera de bug que tenés documentada como limitación conocida.** El caso `quotes` → `BasicPhrasesScreen` existe porque la ruta es un `String` libre que hace de identificador analítico. Con rutas tipadas, el ID de telemetría se vuelve un campo explícito de `Tool`, independiente de la navegación, y un test de un renglón garantiza que catálogo e identificadores no puedan divergir.

Y de paso mata `TOOL_ROUTE_MAP` en el backend, esas ~50 entradas que hoy normalizan en tiempo de lectura lo que el cliente emitió mal.

Es una de esas mejoras que un revisor reconoce al instante como "está al día con el framework".

### 💡 M2. Baseline Profile — la mejora medible más barata que te queda

No hay `baseline-prof.txt` ni módulo de baseline profiles. Para una app con 33 herramientas y arranque a un catálogo con búsqueda, un Baseline Profile típicamente mejora el tiempo de arranque en frío entre un 20% y un 40%.

Por qué esto encaja perfecto con tu portfolio: **tenés un benchmark de Protobuf con tabla comparativa que es tu mejor pieza de contenido técnico.** Un Baseline Profile con `macrobenchmark` te da una segunda tabla del mismo tipo — antes/después, con números reales — sobre un eje distinto (arranque en vez de parseo). Dos mediciones independientes construyen una narrativa que una sola no construye: "mido antes de optimizar" deja de ser una anécdota y pasa a ser un método.

Costo: un módulo de benchmark, una regla de macrobenchmark, y una tarea de Gradle. Un día largo.

### 💡 M3. Un `SensorRepository` unificado

Brújula, nivel de burbuja y fotómetro leen `SensorManager` cada uno por su cuenta y cada uno implementa su propio filtrado. Son 368 interacciones combinadas, el 14,6% del uso real.

Un `SensorRepository` que exponga `Flow<SensorReading>` con `callbackFlow`, filtro paso-bajo compartido y fusión de sensores:
- elimina la triplicación,
- da un componente reutilizable y testeable (con `Flow` de prueba, sin hardware),
- y es exactamente el tipo de abstracción que se puede mostrar en una entrevista.

Además habilita algo que hoy no tenés: **calibración compartida**. La brújula sin corrección de declinación magnética da un norte que puede estar varios grados corrido según la ubicación. Un repositorio unificado es el lugar natural para resolverlo una vez.

### 💡 M4. `Dispatchers` inyectable en `Metrics.kt`

Ya tenés dos costuras de test (`metricsRepoFactory`, `metricsTestScheduleHook`) documentadas como temporales hasta Hilt. Agregar una tercera para el dispatcher es coherente, cuesta tres líneas, y resuelve A1 de raíz en vez de parchear el aserto.

### 💡 M5. Migrar `BasicPhrasesScreen` de Gson a `kotlinx.serialization`

Un archivo. Elimina una dependencia completa. Y de paso corregís el nombre de la ruta (`quotes` → `basic_phrases`), que es la limitación conocida #4 de tu propia documentación de arquitectura.

### 💡 M6. Claves en listas Lazy

Solo 4 de 19 llamadas a `items(...)` pasan `key =`. Sin clave, Compose reasigna estado por posición: si el usuario reordena favoritos o borra un gasto del medio de la lista, el estado de los ítems (animaciones, expansión, foco) se mezcla. Es un bug latente de UI en las listas editables — favoritos, gastos, tareas, timers de pomodoro.

### 💡 M7. `androidx.core:core-splashscreen`

Hoy la splash es un tema legacy con atributos `windowSplashScreen*` en `values/themes.xml` que requieren API 31, contra un `minSdk = 28`. Están baselineados como `NewApi`. La librería de compatibilidad resuelve el warning **y** te da splash real en Android 9 y 10, que es donde probablemente esté una parte de tu base.

---

## 4. Mejoras de diseño e interfaz

### 🎨 D1. La pantalla de ayuda de cada herramienta está desaprovechada

`TopBarReusable` tiene un `onShowInfo` opcional que abre un diálogo de ayuda. Es un buen patrón y está bien implementado. Pero es **el único punto de onboarding de toda la app**: no hay ningún flujo de primera vez, ni en la app ni en las herramientas individuales.

Para las herramientas de sensores esto es un problema de producto concreto. La brújula necesita calibración en ocho, el nivel de burbuja necesita una superficie de referencia, la regla AR necesita mover el teléfono para que ARCore detecte planos. Un usuario que abre cualquiera de las tres y ve una pantalla que no responde como espera, se va — y tus datos dicen que el 34,3% del uso está en esas tres más la lupa.

**Propuesta de bajo costo:** un `AnimatedVisibility` con una tarjeta de una línea la primera vez que se abre cada herramienta ("Movete lentamente para detectar superficies"), persistida en el `FavoritesDataStore` que ya existe. No es un onboarding, es un empujón. Cuesta un componente reutilizable y una clave de DataStore.

La regla AR nueva ya hace algo de esto con `StatusBanner` y estados de tracking detallados. Es el modelo a replicar en las otras tres.

### 🎨 D2. Seis herramientas pierden el trabajo del usuario al salir

`ConversorUnidadesScreen`, `DecimalBinaryConverterScreen`, `GroupSelectorScreen`, `PasswordGeneratorScreen`, `PorcentajeScreen` y `QRCodeGeneratorScreen` no tienen ni DataStore ni `rememberSaveable`. Salir y volver borra todo.

En el conversor de unidades y el de porcentajes da lo mismo. En el **generador de equipos** (58 usos) no: alguien que armó equipos de 12 personas y sale a mirar un mensaje pierde el reparto. Y en el **generador de QR** (41 usos) tampoco: reescribir una URL larga en un teclado de teléfono es molesto.

`rememberSaveable` en esos dos casos son literalmente dos líneas.

### 🎨 D3. Solo tres herramientas pueden compartir su resultado

`Intent.ACTION_SEND` aparece en tres archivos: divisor de gastos, generador de QR y "En otro mundo". Ninguna otra herramienta puede exportar nada.

Los candidatos obvios, ordenados por uso:
- **Regla AR (170)** — compartir la foto con las mediciones dibujadas encima. Es la feature que más se comparte en apps de este tipo, y ya estás dibujando sobre un `Canvas`, así que el bitmap está a un `graphicsLayer().toImageBitmap()` de distancia.
- **Marcador de truco (79) y marcador genérico (45)** — compartir el resultado final de la partida.
- **Generador de contraseñas (35)** — copiar al portapapeles con `ClipData` marcado como sensible (`EXTRA_IS_SENSITIVE`), que es lo correcto para una contraseña y casi nadie lo hace.

Compartir es la función de crecimiento orgánico más barata que existe, y en tu caso además es contenido para el README: una captura de una medición AR compartida vale más que tres capturas de menú.

### 🎨 D4. El dashboard tiene los datos para responder preguntas que no responde

Tenés `daily_active`, `versions`, `versions_first_seen`, `tools`, `ads`, `widgets` y `lang`. Con eso ya se pueden construir dos vistas que hoy no existen y que cambiarían la conversación en una entrevista:

- **Retención por cohorte de versión.** Cruzando `versions_first_seen` con `versions` por día se puede estimar cuántos dispositivos que aparecieron en una versión siguen apareciendo N semanas después. Es la métrica que todo el mundo pregunta y que casi ningún proyecto personal tiene.
- **Ratio herramientas por sesión.** `tools / daily_active` te dice si la gente entra a hacer una cosa o explora. Con los datos actuales el ratio es ~1,14 herramientas por apertura, lo cual sugiere fuertemente que la app se usa como acceso directo a una herramienta específica, no como catálogo. **Eso tiene una implicación de producto directa: los widgets y los shortcuts importan más que la pantalla de categorías**, y hoy el 61,4% del uso de widgets es el de accesos directos.

### 🎨 D5. Solo español e inglés

Los datos muestran tráfico en `sr`, `de`, `fr`, `ru`, `tr`, `nl`, `th`, `fa` y `pl`. Son 27 usuarios-día combinados: poco. Pero el 16,7% usa la app en inglés y la app está en `values/` (inglés) + `values-es/`.

No propongo traducir a nueve idiomas. Propongo algo más barato y más visible: **soporte de RTL verificado**. `fa` aparece en tus datos y el árabe/persa/hebreo requieren `start`/`end` en vez de `left`/`right` en los modifiers. Es una revisión de una tarde y es un detalle que los revisores técnicos notan porque casi nadie lo hace.

### 🎨 D6. Orientación bloqueada globalmente en vertical

`MainActivity:58` fuerza `SCREEN_ORIENTATION_PORTRAIT` para toda la app. Es una decisión defendible que evita mucho trabajo, pero tiene un costo específico: **el nivel de burbuja y la regla AR son las dos herramientas donde el usuario naturalmente gira el teléfono**. Y en tablets y plegables, una app bloqueada en vertical se ve claramente como una app de teléfono estirada.

No digo que soportes landscape en las 32 herramientas. Digo que valdría la pena habilitarlo en esas dos, que el mecanismo (`LockScreenOrientationIfAllowed`) ya está preparado para hacerlo por pantalla, y que eso es lo que hace la diferencia entre "app de teléfono" y "app que pensó en el dispositivo".

---

## 5. Análisis del catálogo de herramientas

### Advertencia sobre los datos

Me pediste un análisis de los últimos 90 días. **Con los datos que tengo no se puede hacer por herramienta.** El export trae el uso de herramientas como agregado del período completo (327 días); solo las aperturas están desglosadas por día. No hay forma de reconstruir qué herramientas se usaron en los últimos 90 días sin una consulta nueva al dashboard.

Lo que sí puedo decir del período reciente:

| Ventana | Aperturas brutas | Por día | Corregido por el sesgo +2 |
|---|--:|--:|--:|
| Últimos 30 días | 145 | 4,8 | ~3,5 |
| Últimos 90 días | 350 | 3,9 | ~2,8 |
| Últimos 180 días | 764 | 4,2 | ~3,1 |

El tráfico es estable en el orden de 3 a 5 aperturas diarias. No hay caída ni crecimiento en el último semestre.

**Vale la pena que agregues al dashboard un filtro por rango de fechas para el desglose de herramientas.** Sin eso no podés responder la pregunta que me hiciste, que es exactamente la pregunta correcta.

El análisis que sigue usa el período completo, normalizado sobre las **2.519 interacciones** que corresponden a las 32 herramientas del catálogo actual (excluí `about` y `pro`, que son pantallas de sistema, y las 13 herramientas ya eliminadas).

Nota sobre `roman_numerals`: la inflación de la broma de tu amigo no afecta nada, porque esa herramienta ya no está en el catálogo.

### La distribución

| Herramienta | Usos | % | Acumulado |
|---|--:|--:|--:|
| minesweeper | 322 | 12,8% | 12,8% |
| magnifier | 194 | 7,7% | 20,5% |
| flashlight | 177 | 7,0% | 27,5% |
| ar_ruler | 170 | 6,7% | 34,3% |
| compass | 167 | 6,6% | 40,9% |
| bubble_level | 123 | 4,9% | 45,8% |
| water | 111 | 4,4% | 50,2% |
| ruler | 101 | 4,0% | 54,2% |
| meetings | 85 | 3,4% | 57,6% |
| truco_scoreboard | 79 | 3,1% | 60,7% |
| light_meter | 78 | 3,1% | 63,8% |
| *— corte del 64% —* | | | |
| dice | 63 | 2,5% | 66,3% |
| selector_wheel | 61 | 2,4% | 68,7% |
| group_selector | 58 | 2,3% | 71,0% |
| coin_flip | 56 | 2,2% | 73,2% |
| todo | 54 | 2,1% | 75,4% |
| guess_flag | 52 | 2,1% | 77,5% |
| pomodoro_list | 52 | 2,1% | 79,5% |
| quick_calcs | 50 | 2,0% | 81,5% |
| decimal_binary | 49 | 1,9% | 83,4% |
| multiverse_me | 45 | 1,8% | 85,2% |
| scoreboard | 45 | 1,8% | 87,0% |
| qr_generator | 41 | 1,6% | 88,6% |
| age_calculator | 39 | 1,5% | 90,2% |
| guess_capital | 37 | 1,5% | 91,7% |
| password_generator | 35 | 1,4% | 93,1% |
| quotes | 34 | 1,3% | 94,4% |
| zodiac_sign | 33 | 1,3% | 95,7% |
| percentage | 30 | 1,2% | 96,9% |
| unit_converter | 29 | 1,2% | 98,1% |
| countdown | 27 | 1,1% | 99,1% |
| countries_info | 22 | 0,9% | 100,0% |

**Once herramientas concentran el 64% del uso. Las veintiuna restantes se reparten el 36%, ninguna pasando el 2,5%.**

Y la observación que más importa: el clúster de sensores y hardware — lupa, linterna, regla AR, brújula, nivel, regla, fotómetro — suma **1.010 interacciones, el 40% del uso real**. Es a la vez lo más usado y lo que mejor demuestra criterio técnico. Ese alineamiento es raro y sigue siendo el activo estratégico del proyecto.

### Qué sacar

Advertencia honesta antes de la lista: **con 2 a 5 usuarios activos diarios, estos números tienen muy poco poder estadístico.** La diferencia entre 22 y 45 usos en 327 días puede ser una persona. No estoy midiendo preferencia de mercado; estoy midiendo un patrón débil sobre una muestra chica. Usá esto como una señal más, no como veredicto.

Dicho eso, la lista para eliminar o fusionar:

**Eliminar directamente (4):**

| Herramienta | Usos | Por qué |
|---|--:|---|
| `zodiac_sign` | 33 | Contenido, no ingeniería. No demuestra nada técnico y desentona con una app de utilidades |
| `quotes` | 34 | Es `BasicPhrasesScreen` con nombre equivocado. Si la eliminás, resolvés de paso tu limitación conocida #4 y sacás la dependencia de Gson |
| `multiverse_me` | 45 | Novedad de una sola vez. El uso es de exploración, no de retorno |
| `countries_info` | 22 | La menos usada de todas. Ver la nota abajo |

Sobre `countries_info`: sé que duele porque es la herramienta detrás de tu benchmark de Protobuf. Pero **el benchmark vive en `docs/performance_tests.md` y en el README, no en la herramienta**. La historia técnica sobrevive intacta — y de hecho mejora: *"medí, elegí Protobuf, y después removí la feature porque los datos decían que casi nadie la usaba"* es una decisión de producto más fuerte que mantenerla por apego al trabajo hecho. El dataset lo siguen usando `guess_flag` y `guess_capital`, así que no perdés la infraestructura.

**Fusionar (7 herramientas → 2):**

- **`dice` (63) + `coin_flip` (56) + `selector_wheel` (61) + `group_selector` (58)** → una sola herramienta "Aleatorio" con modos. Son 238 usos combinados, lo cual la pondría segunda en el ranking, y son cuatro interfaces distintas para la misma primitiva: elegir al azar. Cuatro entradas menos en el catálogo y una demostración de criterio de producto.
- **`decimal_binary` (49) + `percentage` (30) + `unit_converter` (29)** → una "Calculadora" con pestañas. 108 usos combinados.

**Resultado: de 32 a 21 herramientas.** No por minimalismo estético. Con 32 herramientas y 2 ViewModels, el mensaje es "hice muchas cosas rápido". Con 21 bien arquitecturadas, el mensaje es "sé qué mantener". La segunda lectura es la que te sirve.

### Qué agregar — dos herramientas, no más

El criterio que usé: que encaje con el clúster de sensores (donde está el 40% de tu uso y toda tu ventaja técnica), que demuestre algo que tu catálogo actual no demuestra, y que sea alcanzable sin abrir un frente nuevo de mantenimiento.

**1. Escáner de códigos QR y de barras**

Ya tenés generador de QR (41 usos), ya tenés CameraX integrado (la lupa y el fotómetro lo usan), ya tenés permiso de cámara. Falta el lado que la gente realmente usa: escanear.

- **Encaja perfecto con el catálogo:** completa un par obvio y aprovecha infraestructura que ya existe.
- **Es la utilidad más buscada de esta categoría.** En una app de utilidades, escanear QR es la herramienta que la gente busca antes que ninguna.
- **Técnicamente demuestra algo nuevo:** ML Kit Barcode Scanning corriendo on-device sobre `CameraX` + `ImageAnalysis`, con manejo de ciclo de vida y de rendimiento en tiempo real. Es visión por computadora sin backend, offline, y sin recolectar nada. Encaja con tu narrativa privacy-first.
- **Costo:** una dependencia (`play-services-mlkit-barcode-scanning`), una pantalla, reutilizando el `PreviewView` que ya usa la lupa. Dos días.

**2. Medidor de nivel de sonido (sonómetro)**

Es la pieza que le falta al clúster de instrumentos. Tenés fotómetro (luz), brújula (magnetómetro), nivel (acelerómetro), regla AR (cámara + profundidad). El micrófono es el único sensor común que no estás usando.

- **Completa la colección**, y "conjunto de instrumentos de medición" es una identidad de producto más fuerte que "colección de utilidades".
- **Técnicamente es más interesante de lo que parece:** requiere `AudioRecord` con captura PCM cruda, cálculo de RMS, conversión a dBFS y calibración a dBA. La calibración es el problema difícil e interesante — el micrófono de cada teléfono tiene una respuesta distinta, así que hay que ofrecer un offset ajustable y ser honesto sobre la precisión. **Documentar esa limitación con rigor es exactamente el tipo de contenido que ya sabés producir** (lo hiciste con el postmortem del pomodoro).
- **Se lleva bien con lo que ya tenés:** un `Flow` de lecturas encaja directo en el `SensorRepository` de M3.
- **Costo:** permiso `RECORD_AUDIO`, una pantalla con `Canvas` para el gráfico en tiempo real (ya tenés ese patrón en el nivel de burbuja y ahora en la regla AR). Dos o tres días.

**Lo que no recomiendo agregar**, para que quede explícito el criterio: nada que necesite backend, cuenta de usuario, sincronización o una API externa. Todo tu argumento de privacidad y de rendimiento offline se apoya en que la app no depende de nadie. La primera herramienta que rompa eso te cuesta más de lo que aporta.

---

## 6. Prioridades

**Esta semana, sin discusión:**

1. Arreglar `MetricsTest` y sacar `master` del rojo (A1).
2. Activar branch protection (A2). Quince minutos, y es lo que hace que el punto 1 no vuelva a pasar.

**Antes de empezar Fase 2:**

3. Limpiar las dependencias duplicadas del `build.gradle.kts` (B3). Tres horas, y es lo primero que mira alguien que sabe Gradle.
4. `rememberSaveable` en generador de equipos y generador de QR (D2). Dos líneas.
5. Claves en las listas Lazy editables (M6).

**Fase 2, en el orden que ya tenías, con dos agregados:**

6. Extraer `ARulerVM` a un ViewModel real y sacar la geometría del archivo de UI (B2). Es medio día y te da tu segunda pieza ejemplar, sobre la herramienta que más te distingue.
7. Navegación type-safe (M1), que conviene hacer *antes* del contrato de eventos tipado, porque resuelve la mitad del problema.

**Cuando quieras un golpe de efecto medible:**

8. Baseline Profile con macrobenchmark (M2). Es tu segunda tabla de números reales, y dos mediciones independientes convierten "mido antes de optimizar" de anécdota en método.

---

## Cierre

Las Fases 0 y 1 están cerradas salvo los GIFs y la protección de rama. El repositorio pasó de 178 MB a 6,2 MB, tiene tests que encontraron un bug real del motor, tiene CI, tiene un README que ya no afirma cosas falsas, y tiene documentación de métricas que la mayoría de los proyectos comerciales no tiene.

**Lo más urgente:** el CI en rojo. No por el bug — el bug es de una línea — sino porque un badge rojo en un README de portfolio comunica exactamente lo contrario de lo que el CI existe para comunicar.

**Lo más subestimado, otra vez:** el clúster de instrumentos. El 40% de tu uso está ahí, es lo más difícil de replicar de tu repo, y sigue siendo lo que menos protagonismo tiene en cómo presentás el proyecto. La regla AR nueva es buen código y nadie lo va a ver hasta que exista el GIF.

**La trampa que sigue vigente:** con 2 a 5 usuarios diarios, los datos de uso te sirven para descartar herramientas obviamente muertas, no para tomar decisiones finas. Usalos para podar el catálogo, no para justificar una hoja de ruta.
