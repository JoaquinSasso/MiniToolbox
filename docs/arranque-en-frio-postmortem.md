# Postmortem: arranque en frío lento al abrir una herramienta desde un widget

**Proyecto:** MiniToolbox (`com.joasasso.minitoolbox`), Kotlin y Jetpack Compose
**Área:** `MainActivity` y `NavGraph.kt`. Afecta a todos los intents que abren una herramienta: widgets de agua y de favoritos, y notificaciones.
**Dispositivo de prueba:** Google Pixel 8, Android API 37, build `benchmark` (R8, no debuggable)
**Fecha:** 2026-11-11
**Commits:** `c53de30` (build de medición), `b6fc929` (instrumentación), `c165f35` (arreglo), `5f3f3d1` (script)

## Resumen

Al tocar el widget de agua con la app cerrada, la pantalla de agua tardaba **522 ms** (mediana de 15 corridas) en quedar dibujada por completo. Abrir la app desde el ícono tardaba 208 ms. La diferencia no estaba en el proceso ni en la `Application`: el `NavHost` arrancaba siempre en el catálogo, lo componía y lo dibujaba, y recién después navegaba a la herramienta con la animación de 280 ms.

El arreglo arma la pila `[Categories, destino]` antes de la primera composición del `NavHost`. Así, el primer frame ya es la herramienta. Con la misma build, el mismo dispositivo y el mismo script, la mediana bajó a **183 ms**, 339 ms menos. Abrir desde el ícono no cambió: 208 ms antes y 218 ms después, con rangos que se superponen.

## Cómo se manifestaba

El usuario percibía que la app "demora mucho en abrir para lo que es", sobre todo desde un widget. Visualmente: splash, un instante del catálogo y luego la herramienta entrando con un deslizamiento lateral.

`am start -W` no mostraba nada raro. Su `TotalTime` (TTID, tiempo hasta el primer frame) era igual desde el ícono que desde el widget (unos 210 ms en ambos casos), porque el primer frame era el catálogo en los dos casos. La métrica estándar de arranque no veía el problema, y por eso hizo falta instrumentar primero (ver "Problemas encontrados", punto 3).

## Causa raíz

En `MainActivity` (antes de `c165f35`):

1. `NavHost` tenía `startDestination = Screen.Categories` fijo.
2. La ruta del intent (`EXTRA_START_ROUTE_JSON`) se guardaba en un estado de Compose y se aplicaba en un `LaunchedEffect(startRoute)`.
3. Un `LaunchedEffect` corre después de la primera composición. Para ese momento, el catálogo ya se había compuesto (`CategoriesScreen`, con su `LazyVerticalGrid`, los favoritos y el buscador) y se había dibujado.
4. `navController.navigate(destino)` disparaba entonces la transición de entrada del `NavHost`: un `slideIntoContainer` de 280 ms.

Desglose aproximado de los 522 ms: unos 210 ms hasta el primer frame (igual que desde el ícono), 280 ms de animación y el resto en componer la herramienta durante la transición. Es una estimación por diferencia, no una traza: no se separó con Perfetto.

## Cambios

### 1. Build de medición (`c53de30`)

No se podía medir con lo que había:

- La build **debug** es debuggable y no pasa por R8. ART no optimiza igual el código debuggable, así que los tiempos no representan los de Play.
- La build **release** sale sin firmar si no existe `keystore.properties`, y no se puede instalar.

Se agregó el build type `benchmark`: `initWith(release)`, firmado con la clave de debug, `<profileable android:shell="true"/>` en `app/src/benchmark/AndroidManifest.xml`, y `METRICS_ENDPOINT` vacío. Con el endpoint vacío, `MetricsConfig.isConfigured` queda en `false` y las mediciones no mandan eventos al backend de producción. Firmar con la clave de debug permite instalarla encima de la build debug sin borrar los datos del teléfono.

### 2. Instrumentación: time to full display (`b6fc929`)

`MainActivity` llama a `ReportDrawnWhen`, que dispara `reportFullyDrawn()` cuando se cumplen dos condiciones:

- `navController.visibleEntries` tiene una sola entrada, o sea que no hay ninguna transición en curso;
- esa entrada es el destino pedido por el intent, o cualquier pantalla si no se pidió ninguno.

El sistema lo registra en logcat como `Fully drawn ... +NNNms` (TTFD). Esto queda en producción: también es la métrica que usa Play Console para "tiempo hasta la visualización completa".

### 3. Arreglo (`c165f35`)

- `MainActivity.onCreate` resuelve el destino del intent **antes** de `setContent`, y solo si `savedInstanceState == null`: si la Activity se recrea, `NavController` restaura su propia pila. En el mismo lugar registra el origen de la métrica (`PendingEntrySource`, `widgetUse`), que antes se registraba dentro del `LaunchedEffect`.
- `MiniToolboxNavGraph` arma el `NavGraph` en un `remember`, fuera del `NavHost`, y en el mismo bloque llama a `prepareStartBackStack`. Esa función fija el lifecycle owner, el `ViewModelStore` y el grafo, y navega al destino. Cuando `NavHost` se compone por primera vez, la pila ya es `[Categories, destino]`: el primer frame muestra la herramienta, sin animación y sin componer el catálogo.
- `NavHost` vuelve a fijar el mismo grafo. `NavControllerImpl.setGraph` (navigation 2.10.1) solo reinicia la pila si la instancia es distinta (`if (_graph != graph)`); con la misma instancia, reemplaza los nodos y conserva la pila. `StartBackStackTest.volverAFijarElMismoGrafo_noReiniciaLaPila` cubre esa suposición.
- Si la Activity ya existe (`onNewIntent`), el camino no cambia: `LaunchedEffect(startRoute)` y navegación con animación.

### 4. Script reproducible (`5f3f3d1`)

`scripts/measure-cold-start.sh <launcher|widget> [corridas]` hace `force-stop` y `am start -W -S` en cada corrida. En modo `widget` usa el mismo intent que arma `AguaWidget`. Imprime TTID y TTFD por corrida, más la mediana.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| `startDestination = destino` cuando hay deep link | La pila queda con un solo elemento y "atrás" cierra la app en lugar de volver al catálogo. Es un cambio de comportamiento que nadie pidió. |
| Mantener el `LaunchedEffect` y desactivar solo la animación de la primera navegación | Saca los 280 ms, pero el catálogo se sigue componiendo y dibujando un frame, que además se ve como un parpadeo. También agrega un flag de estado a las transiciones del `NavHost`. |
| Generar un baseline profile | Acelera el código en frío, pero no elimina la navegación extra. Es un trabajo aparte, que ya está en el backlog (`baseline-profile-invalid`). |
| Mover trabajo de `MiniToolboxApp.onCreate` a segundo plano | Puede pesar, pero no se midió. El TTID desde el ícono (unos 210 ms) no muestra un problema evidente ahí. Quedó en el backlog sin tocar. |

## Problemas encontrados en el camino

1. **Sin keystore no hay build release instalable.** Se resolvió con el build type `benchmark` (cambio 1).
2. **App Check se declara por build type.** La primera compilación de `benchmark` falló en `compileBenchmarkKotlin` con `Unresolved reference 'appcheck'`: la dependencia estaba solo como `releaseImplementation` y `debugImplementation`, y `AppCheckSetup` vive en `src/release` y `src/debug`. Se agregó `"benchmarkImplementation"(libs.firebase.appcheck.playintegrity)` y `src/release/java` como fuente de la build `benchmark`.
3. **TTID no veía el problema.** La primera medición con `am start -W` dio 223, 220 y 227 ms desde el widget, lo mismo que desde el ícono. Ver "Cómo se manifestaba". Por eso se instrumentó TTFD (cambio 2) antes de tocar la navegación.
4. **Error de lint introducido por este trabajo.** La primera versión del arreglo usaba un `remember { ... }` que devolvía `Unit` para preparar la pila, y `lintDebug` falló con `RememberReturnType`. Se movió la llamada a un `.also {}` sobre el `remember` que construye el grafo. No se agregó nada al baseline de lint.
5. **Test intermitente ajeno al cambio.** En la primera corrida completa fallaron 2 tests de `GastoFormViewModelTest` con `Dispatchers.Main is used concurrently with setting it`. Al re-ejecutar sin cambios, pasaron. Ya figuraba en el backlog como `flaky-gasto-form-test`.

## Resultado

Método: build `benchmark` de cada commit, instalada en el Pixel 8, `adb shell cmd package compile -f -m verify com.joasasso.minitoolbox` (el mismo estado de compilación en ambos lados), una corrida de calentamiento descartada y 15 corridas por modo con `scripts/measure-cold-start.sh`. La medición "antes" se tomó con `b6fc929`, que ya tiene la instrumentación pero no el arreglo. La de "después", con `c165f35`.

| Modo | Métrica | Antes, mediana (rango) | Después, mediana (rango) |
|---|---|---|---|
| Widget de agua | TTFD | 522 ms (510-604) | 183 ms (172-210) |
| Widget de agua | TTID | 207 ms (195-287) | 176 ms (164-203) |
| Ícono del launcher | TTFD | 208 ms (197-258) | 218 ms (194-247) |
| Ícono del launcher | TTID | 202 ms (191-253) | 212 ms (188-241) |

TTFD crudo, en ms, en orden de corrida:

```
antes   widget:   519 510 523 541 545 604 514 523 520 551 521 510 522 529 522
despues widget:   210 174 186 172 190 179 197 173 183 192 195 183 195 177 177
antes   launcher: 258 218 224 203 232 197 220 206 225 208 203 203 213 207 198
despues launcher: 212 247 204 209 218 245 219 221 214 213 204 226 222 194 222
```

Desde el widget, el TTFD cae por debajo del arranque desde el ícono, porque la pantalla de agua es más liviana que el catálogo. La diferencia en el launcher (+10 ms de mediana) cae dentro del ruido: los rangos se superponen y ese camino no cambió.

Verificación funcional en el dispositivo:

- en frío desde el intent del widget, abre Agua;
- "atrás" vuelve al catálogo;
- con la app abierta, el mismo intent (`onNewIntent`) vuelve a abrir Agua.

`.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`: build correcta, 324 tests, 0 fallas, 1 omitido, lint sin errores nuevos.

## Límites de esta medición

- **Un solo dispositivo,** un Pixel 8, que es un equipo de gama alta. En equipos más lentos, los números absolutos van a ser mayores. La animación de 280 ms, en cambio, era un costo fijo en cualquier equipo.
- **`compile -m verify`** representa una instalación sin código compilado por adelantado. Una instalación desde Play con perfiles en la nube arranca más rápido, así que los números absolutos no son los de producción. La comparación vale porque ambos lados se midieron en el mismo estado.
- **No se vació la caché de páginas** entre corridas: hace falta root.
- **No se midió la build debug,** que es la que se suele usar al desarrollar y la que el autor podía estar percibiendo como lenta.
- **No se probó la restauración tras la muerte del proceso** con un destino abierto. El código no vuelve a navegar si `savedInstanceState != null`, pero ese camino no se ejercitó en el dispositivo.
- **Los widgets reales no se tocaron a mano:** el script reproduce su intent con `am start`. Las notificaciones de agua y del Pomodoro usan el mismo extra, pero no se probaron.

## Lecciones

- **Medir lo que ve el usuario, no lo que es fácil de medir.** TTID era idéntico en los dos caminos y habría llevado a concluir que no había problema. Para cualquier arranque que no termina en el primer frame, hace falta reportar TTFD.
- **Un `LaunchedEffect` no sirve para decidir la primera pantalla.** Corre después de la primera composición, así que todo lo que se compone antes se paga y se ve.
- **Medir exige una build medible.** Sin una build no debuggable, instalable y que no mande telemetría a producción, cualquier número habría sido engañoso o peligroso para los datos.
