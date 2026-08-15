# Postmortem: la alarma del Pomodoro que sonaba pero no se veía

**Proyecto:** MiniToolbox (`com.joasasso.minitoolbox`) — Kotlin + Jetpack Compose
**Módulo:** Temporizador Pomodoro (`tools/organizacion/pomodoro`)
**Plataforma de prueba:** Google Pixel 8, Android 17 (API 37, "CinnamonBun"), `targetSdk 37`
**Duración de la investigación:** una serie de builds probados en dispositivo real, con inspección directa del estado del sistema de notificaciones vía `adb`.

## TL;DR

El pomodoro tenía una pantalla de alarma a pantalla completa, al estilo de la app Reloj de Android, para apagar el timer sin tener que navegar dentro de la app. El audio sonaba perfecto. La UI de alarma, no: ni el heads-up ni el salto automático a pantalla completa aparecían nunca, con el teléfono bloqueado o desbloqueado, y con todos los diagnósticos de permisos y canales en verde.

La causa raíz era una sola línea: `NotificationCompat.Builder.setSilent(true)`. Esa llamada no se limita a sacar el sonido — mete la notificación en un grupo con política "sólo alerta el resumen", y como nunca se publica un resumen, el sistema suprime heads-up y full-screen intent por diseño. El nombre de la API sugiere "sin sonido"; el comportamiento real es "sin alertas visuales tampoco".

En el camino se probó y descartó una hipótesis distinta (una posible interferencia de Media3 con la notificación del foreground service) que resultó no ser el problema en este caso, aunque el mecanismo que describe es real y se dejó como salvaguarda preventiva. También se confirmó, ya con el bug principal resuelto, un comportamiento de plataforma no documentado como "bug" sino como política: desde Android 14, el sistema no dispara el full-screen intent si la pantalla está encendida y desbloqueada y la notificación de todos modos va a hacer heads-up. Ese caso está resuelto por diseño de Android, no por código de la app.

---

## Contexto: qué existía antes de este problema

El pomodoro ya tenía resuelta, en una ronda de trabajo anterior, una serie de bugs de fondo distinta: alarmas inexactas, wakelocks, condiciones de carrera, y — el más relevante para este documento — el "background audio hardening" de Android 17, que silencia en secreto la reproducción de audio de apps sin capacidades *while-in-use*. La solución fue migrar de un `Service` con `MediaPlayer` a un `MediaSessionService` de la librería **media3**, con `ExoPlayer` configurado con `AudioAttributes.USAGE_ALARM`. Ese fix quedó confirmado por test y no es el tema de este documento, pero es imprescindible tenerlo presente: la arquitectura de audio elegida (media3) es la que después generó una hipótesis de investigación completa, explicada más abajo.

Sobre esa base se construyó lo nuevo:

- **`PomodoroAlarmActivity`**: una `Activity` standalone, deliberadamente fuera del `NavHost` de `MainActivity`, para no depender de que un deep-link funcione en un arranque en frío — que es justo el escenario donde más hace falta que la pantalla aparezca sola.
- **`FullScreenIntentPermission`**: un helper para chequear y pedir el permiso `USE_FULL_SCREEN_INTENT`, que desde Android 14 no se autoconcede a apps que no sean de llamadas o alarmas como función central.
- **`PomodoroNotification.buildAlarmNotification()`** modificado para apuntar su `setContentIntent()` y `setFullScreenIntent()` a `PomodoroAlarmActivity`, con logging de diagnóstico (`canUseFullScreenIntent()`, importancia real del canal).

Con todo eso desplegado, el comportamiento observado era el siguiente: el audio sonaba, pero ni el heads-up ni el salto a pantalla completa aparecían nunca, en ningún escenario probado — pantalla bloqueada, desbloqueada con la app en background, con o sin interacción del usuario durante los 30 segundos de timbrado.

---

## Por qué esto es un problema difícil de depurar

Las tres piezas que normalmente explican por qué una notificación no alerta —permiso denegado, canal con importancia degradada, Do Not Disturb activo— estaban confirmadas como correctas por el propio logging de la app:

```
canFullScreen=true channelImportance=4 (HIGH=4) dndFilter=ALL (sin restricciones)
```

Y se descartaron con evidencia, no por suposición: el modo vibrador y el volumen se revisaron por log; el canal específico se inspeccionó a mano en Ajustes, no sólo el interruptor general de la app; y se confirmó que heads-up de otras apps (WhatsApp, Instagram) sí funcionaban en el mismo teléfono, en la misma sesión.

Esto es lo que hace que el caso sea interesante como documentación: **ninguna de las señales habituales de diagnóstico apuntaba al problema real**, porque todas miden el permiso o el canal, y la causa real estaba en un tercer lugar que casi nadie audita — el objeto `Notification` en sí, específicamente su comportamiento de agrupamiento.

---

## Investigación, hipótesis por hipótesis

### Hipótesis descartada sin testear: Background-Started Foreground Service Limitations (BFSL)

La hipótesis de partida, heredada de la ronda de trabajo anterior, era que la diferencia entre la notificación del pomodoro y las de WhatsApp/Instagram era la vía de publicación: la nuestra se publica dentro de un foreground service arrancado *desde background* (`AlarmManager → BroadcastReceiver → startForegroundService()`), y Android 17 le niega capacidades a ese tipo de servicios salvo exención puntual — el mismo mecanismo BFSL que ya había afectado al audio antes de migrar a media3.

Para probarlo se instrumentó el servicio para publicar la misma notificación por dos vías en paralelo: la normal, vía `ServiceCompat.startForeground()`, y una segunda vía `NotificationManagerCompat.notify()` directo, sin pasar por el FGS. La lógica era: si sólo la segunda alertaba, confirmaba BFSL.

El test nunca llegó a ejecutarse tal como estaba planteado, y es una suerte que así fuera: la implementación pasaba el **mismo objeto** `Notification` a las dos llamadas, con el `setSilent(true)` ya adentro. Las dos copias iban a fallar exactamente igual, por el mismo motivo, y el resultado se habría leído erróneamente como "el camino de publicación no es la causa" — una conclusión válida, pero llegada por el camino equivocado.

### Hipótesis 1 (propia): interferencia de Media3 con la notificación del foreground service

`PomodoroAlarmService` extiende `MediaSessionService`, que trae adentro un gestor de notificaciones propio: en cuanto el `Player` pasa a reproducir, media3 construye su propia notificación de estilo "Now playing" y llama a `startForeground()` con **su** id por defecto (1001) y un canal de importancia baja creado por la librería.

El mecanismo por el que esto podría romper todo es real y está documentado en el comportamiento de `ActiveServices` del framework: un servicio tiene una sola notificación de primer plano, y si se llama a `startForeground()` con un id distinto del que ya tenía, el sistema **cancela** la notificación anterior antes de publicar la nueva. La hipótesis era que la notificación de alarma se publicaba correctamente y, milisegundos después, media3 se la llevaba puesta.

Se implementó la contramedida — un override vacío de `onUpdateNotification()` en el servicio, para impedir que media3 tomara el control de la notificación del FGS — junto con el fix real (ver más abajo). El primer test posterior a este cambio no mostró ninguna mejora, lo cual en retrospectiva tenía una explicación mucho más simple.

### El primer test post-fix no probó nada, porque el build no llegó al dispositivo

Antes de sacar ninguna conclusión sobre por qué el fix "no funcionaba", se pidió inspeccionar el estado real del sistema con dos herramientas de `adb`:

```bash
adb logcat -c
# arrancar el pomodoro, esperar a que suene
adb shell dumpsys notification --noredact > notif.txt   # durante el timbrado
adb logcat -d > log.txt                                  # después
```

`dumpsys` le pide a un servicio del sistema — en este caso `NotificationManagerService` — que vuelque su estado interno completo: qué notificaciones hay publicadas en ese instante exacto, en qué canal, con qué flags, y con qué decisión de agrupamiento y alerta. Es la única forma de ver la notificación **como la ve el sistema**, en vez de inferirlo desde el código que la construyó.

El log reveló el problema real de este ciclo: apareció la línea

```
PomodoroAlarmService: DIAGNÓSTICO: notify() directo publicado con id=2004
```

Ese bloque de diagnóstico se había eliminado del código nuevo. Su presencia en el log significaba que el APK instalado en el teléfono era una build anterior a los cambios. Lo mismo confirmó el dump de notificaciones: el campo `groupAlertBehavior` de la notificación de alarma valía `1` (`GROUP_ALERT_SUMMARY`), que es exactamente lo que produce `setSilent(true)` — y ese código ya no debía estar presente.

La lección operativa acá es tan importante como la técnica: **antes de invalidar un diagnóstico, hay que confirmar que el fix realmente se está ejecutando.** Un `Build → Clean Project` y una reinstalación resolvieron el problema de proceso; el de código seguía sin probarse.

### Causa raíz confirmada: `setSilent(true)` y la supresión por agrupamiento

Con el build correcto instalado, se repitió la captura. Esta vez el dump mostró algo más rico que "no funciona": mostró que el sistema **sí** había preparado todo para lanzar el full-screen intent, y aun así no lo hizo.

```
fullscreenIntent=PendingIntent{... (allowlist: +30s0ms/NOTIFICATION_SERVICE/NotificationManagerService)}
groupKey=0|com.joasasso.minitoolbox|g:Aggregate_AlertingSection
groupAlertBehavior=1
flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY|SILENT
mIntercept=false
mSuppressedVisualEffects=0
```

La línea del `fullscreenIntent` es la más reveladora: el sistema le había adjuntado un *allowlist token* de 30 segundos emitido por el propio `NotificationManagerService` — el permiso temporal que el framework prepara específicamente para poder lanzar una `Activity` desde background. El sistema tenía luz verde para saltar a pantalla completa y decidió no hacerlo.

La razón está en las otras tres líneas, juntas. La notificación tiene un `groupKey` no nulo, no es el resumen (*summary*) de ese grupo, y su `groupAlertBehavior` dice "sólo alerta el resumen". Esas tres condiciones combinadas son, literalmente, la definición del método interno `Notification.suppressAlertingDueToGrouping()`, que tanto `NotificationManagerService` como SystemUI consultan para decidir si permiten heads-up y full-screen intent — con este resultado en particular registrado internamente bajo un motivo de diagnóstico dedicado (`NO_FSI_SUPPRESSIVE_GROUP_ALERT_BEHAVIOR`).

`setSilent(true)` hace tres cosas, no una: pone `sound = null` y `vibrate = null`, setea el *group alert behavior* en `GROUP_ALERT_SUMMARY`, y si la notificación no tiene grupo propio, la asigna al grupo `"silent"`. En este caso el grupo real terminó siendo distinto — `g:Aggregate_AlertingSection`, el agrupamiento automático que Android arma por su cuenta cuando una app tiene varias notificaciones activas al mismo tiempo — pero el mecanismo de supresión es el mismo sin importar de dónde venga el grupo: alcanza con que exista uno y con que el *alert behavior* diga "sólo el resumen".

**El fix es eliminar la línea:**

```kotlin
// Antes — root cause del bug
if (!withChannelSound) {
    builder.setSilent(true).setDefaults(0)
}

// Después
if (!withChannelSound) {
    builder.setDefaults(0)
}
```

El silencio deseado ya estaba garantizado por el canal (`CHANNEL_ALARM_SILENT`, creado con `setSound(null, null)` y `enableVibration(false)`), que desde Android 8 manda sobre el builder en todo lo que sea sonido y vibración. `setSilent(true)` era redundante para lo que se necesitaba, y activamente dañino para lo que nadie sabía que hacía.

Con el fix aplicado, el mismo dump mostró:

```
flags=ONGOING_EVENT|NO_CLEAR|FOREGROUND_SERVICE|HIGH_PRIORITY   # sin SILENT
groupAlertBehavior=0                                             # GROUP_ALERT_ALL
groupKey=0|com.joasasso.minitoolbox|2003|null|10533              # ya no comparte grupo
```

Y en el log, el evento de SystemUI que confirma que el heads-up quedó anclado en pantalla durante los 30 segundos completos del timbrado:

```
22:14:21.861  TouchableRegionManager: onHeadsUpPinnedModeChanged
...
22:14:51.859  TouchableRegionManager: onHeadsUpPinnedModeChanged   # 30s después: se retira
```

### La hipótesis de Media3, revisitada — y descartada

El mismo dump permitió cerrar la duda sobre la primera hipótesis. Si media3 hubiera estado reemplazando la notificación, tendría que existir en el sistema una notificación con id `1001` y canal `default_channel_id`, publicada por el paquete de la app. Buscando ese canal en el dump completo, la única coincidencia pertenecía a otra aplicación (una app de grabación de audio del sistema) — no a MiniToolbox. Y la notificación `2003` de la app conservaba el flag `FOREGROUND_SERVICE` intacto durante todo el timbrado, lo que confirma que nunca fue reemplazada.

Conclusión: el mecanismo de la hipótesis 1 es real y está documentado en el comportamiento del framework, pero no era lo que estaba pasando en este proyecto. El override defensivo (`onUpdateNotification` vacío) se dejó en el código de todas formas — no tiene costo y cubre a la app si esa interferencia llegara a manifestarse en una versión futura de media3 o del sistema — pero no forma parte del fix real.

### Segundo hallazgo, tras resolver la causa raíz: la política de FSI en pantalla desbloqueada

Con el bug de agrupamiento resuelto, un test con la pantalla **encendida y desbloqueada** mostró el heads-up funcionando perfecto, pero sin salto a pantalla completa. Esto no era un bug nuevo: es una política de plataforma, vigente desde que Android 14 introdujo restricciones contra el abuso del full-screen intent.

SystemUI evalúa el full-screen intent con una máquina de decisión que depende del estado del dispositivo en el momento del disparo:

| Estado del dispositivo | ¿Dispara el FSI? |
|---|---|
| Pantalla apagada | Sí |
| Keyguard (bloqueo) visible | Sí |
| Modo *dreaming* / salvapantallas | Sí |
| Despierto y desbloqueado, y la notificación de todos modos va a hacer heads-up | **No** (`NO_FSI_EXPECTED_TO_HUN`) |

El razonamiento de la plataforma es directo: si el usuario está mirando la pantalla, secuestrarla entera es innecesario — el heads-up ya cumplió el objetivo de avisar. Se confirmó repitiendo el test con la pantalla bloqueada: la `Activity` de alarma apareció sobre el keyguard como se esperaba, sin más cambios de código.

---

## Resumen de la investigación

| # | Hipótesis | Origen | Resultado |
|---|---|---|---|
| 1 | BFSL: el FGS arrancado desde background pierde capacidades de alerta | Sesión de trabajo anterior | No testeada correctamente (test mal diseñado); descartada de hecho por la causa real encontrada después |
| 2 | Media3 reemplaza la notificación del FGS con la suya (`startForeground` con id distinto cancela la anterior) | Esta investigación | Mecanismo real y documentado, pero **no era la causa en este caso** — confirmado por ausencia de `id=1001` propio en el dump |
| 3 | `setSilent(true)` fuerza `GROUP_ALERT_SUMMARY`, y sin notificación de resumen el sistema suprime heads-up y full-screen | Esta investigación | **Causa raíz confirmada** vía `dumpsys notification` |
| 4 | Con la pantalla desbloqueada, Android no dispara FSI si la notificación de todos modos hará heads-up | Esta investigación | Comportamiento de plataforma por diseño (Android 14+), no un bug — confirmado con test en pantalla bloqueada |

---

## Cambios de código

### 1. `PomodoroNotification.kt` — la causa raíz

```kotlin
if (!withChannelSound) {
    // NO volver a poner setSilent(true) acá. setSilent(true) no se limita a
    // sacar el sonido: además setea el group alert behavior en
    // GROUP_ALERT_SUMMARY, y sin una notificación de resumen publicada, el
    // sistema suprime heads-up y full-screen intent para toda notificación
    // en esa condición. El silencio ya lo garantiza CHANNEL_ALARM_SILENT.
    builder.setDefaults(0)
}
```

### 2. `PomodoroAlarmService.kt` — orden de estado y salvaguarda preventiva

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ...
    // AlarmState.setActive(true) va ANTES de startForeground(): el full-screen
    // intent puede lanzar la Activity de alarma en el instante en que se
    // publica la notificación, y PomodoroAlarmActivity.onStart() se
    // autodestruye si detecta que la alarma no está activa. Publicar el
    // estado después crearía una carrera real.
    AlarmState.setActive(this, true)

    ensurePomodoroChannels(this)
    val notif = buildAlarmNotification(this, title, text, route)
    ServiceCompat.startForeground(this, NOTIF_ID_ALARM_SILENT, notif, /* ... */)
    // ...
}

/**
 * Salvaguarda preventiva: si media3 alguna vez intenta reemplazar la
 * notificación del FGS con la suya propia, este override lo impide. No es
 * la causa raíz del bug original, pero el mecanismo es real y el costo de
 * dejarlo es nulo.
 */
override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
    // no-op a propósito
}
```

### 3. `PomodoroAlarmActivity.kt` — consistencia visual con el resto de la app

La pantalla original envolvía su contenido en `MaterialTheme { }` puro, sin heredar la paleta ni la tipografía de la app (`MiniToolboxTheme`), y extendía `ComponentActivity`, lo que dejaba visible la action bar del tema base sin forma de ocultarla.

```kotlin
class PomodoroAlarmActivity : AppCompatActivity() {   // antes: ComponentActivity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()   // mismo patrón que MainActivity
        // ...
        setContent {
            MiniToolboxTheme {     // antes: MaterialTheme { } puro
                PomodoroAlarmRingingScreen(/* ... */)
            }
        }
    }
}
```

El layout interno se rediseñó para espejar la pantalla del timer: mismo `CircularWavyProgressIndicator` con idénticos parámetros de grosor y separación (con la onda animada para comunicar "sonando" en vez de progreso), y el botón de silenciar como `LargeFloatingActionButton` de 96.dp en la misma posición donde vive el botón de iniciar/detener del timer — para que la acción más frecuente caiga donde el usuario ya tiene el gesto entrenado.

---

## Metodología de verificación

Todo el diagnóstico posterior a la primera vuelta de logging propio se apoyó en dos herramientas de `adb` (parte del Android SDK, en `platform-tools`), corridas contra el dispositivo físico durante el evento en cuestión — no después:

```bash
# Volcar el estado completo del sistema de notificaciones en el instante exacto
adb shell dumpsys notification --noredact | grep -A 30 "id=2003"

# Capturar el log completo alrededor del evento
adb logcat -c   # limpiar antes de reproducir el caso
# reproducir el caso...
adb logcat -d > log.txt
```

El campo más útil resultó ser `groupAlertBehavior` dentro del `NotificationRecord`: es el único lugar donde la decisión real de alertar queda expuesta, y no tiene ningún equivalente accesible desde las APIs públicas que la app usa para diagnosticar (`canUseFullScreenIntent()`, `getNotificationChannel().importance`). Ninguna combinación de permiso correcto y canal correcto lo revela — hay que leer el objeto `Notification` tal como lo ve el sistema.

---

## Lecciones

**Los nombres de las APIs pueden mentir por omisión.** `setSilent(true)` hace exactamente lo que promete con el sonido, y además una tercera cosa que su nombre no sugiere en absoluto. La documentación oficial lo menciona, pero en una frase fácil de pasar por alto si uno llega buscando "cómo silencio esta notificación".

**Los diagnósticos de superficie (permiso, canal, DND) no cubren todo el espacio de fallas.** Los tres pueden estar perfectos y el problema seguir estando en un cuarto lugar — en este caso, un flag de agrupamiento que ninguna de esas tres señales toca. Cuando los diagnósticos habituales dan todos en verde y el síntoma persiste, el paso siguiente es inspeccionar el objeto real con las herramientas de la plataforma, no seguir generando hipótesis sobre el código que lo construye.

**Confirmar que el fix corrió antes de invalidarlo.** El primer ciclo de testing post-fix pareció refutar el diagnóstico. En realidad refutaba un build viejo. `adb logcat` lo dejó en evidencia con una sola línea que no debía estar ahí.

**No todo lo que "no funciona" es un bug.** El caso de pantalla desbloqueada sin full-screen intent no tenía nada que arreglar: es una decisión de diseño de Android 14+ contra el abuso del permiso, y el comportamiento correcto de la app era, precisamente, no pelear contra ella.

**Una hipótesis técnicamente sólida puede estar sencillamente equivocada para el caso concreto**, y eso se descubre con evidencia, no con más razonamiento sobre el mismo código. La hipótesis de media3 explica un mecanismo real del framework; simplemente no era lo que estaba pasando acá. Vale la pena dejar la salvaguarda cuando el costo es cero, pero hay que ser honesto sobre qué resolvió el bug y qué no.

---

## Referencias

- [`Notification.Builder#setGroupAlertBehavior`](https://developer.android.com/reference/android/app/Notification.Builder#setGroupAlertBehavior(int)) — documentación oficial del comportamiento de alerta por grupo.
- [Full-screen intents (Android 14+)](https://developer.android.com/about/versions/14/changes/notifications#full-screen-intents) — cambios de política y el permiso `USE_FULL_SCREEN_INTENT`.
- [Background Started Foreground Services (BFSL)](https://developer.android.com/about/versions/15/behavior-changes-15#fgs-bg-start-restrictions) — el mecanismo detrás de la hipótesis inicial, no confirmado como causa en este caso.
- [Background audio hardening (Android 17)](https://developer.android.com/about/versions/17/changes/bg-audio) — motivo de la migración previa a media3, mencionado como contexto arquitectónico.
