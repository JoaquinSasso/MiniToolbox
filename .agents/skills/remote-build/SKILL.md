---
name: remote-build
description: >-
  Procedimiento para compilar, testear, generar APK/AAB e instalar MiniToolbox usando la PC de
  escritorio (JOAPC, Windows) cuando se trabaja desde la notebook con Ubuntu. Usar al correr
  cualquier comando pesado de Gradle (assemble, bundle, lint, tests) estando en la notebook, o
  cuando el autor pida instalar la app en el teléfono desde ahí.
---

# Compilar en JOAPC desde la notebook

La notebook (Ubuntu, hostname `JOANOTEBOOK`) se usa para editar. Las tareas pesadas de Gradle se delegan a JOAPC por SSH, que compila bastante más rápido. El autor trabaja desde la notebook cuando alguien más usa JOAPC o cuando no está en su casa (ahí el camino es Tailscale); en ambos casos el build sigue yendo a JOAPC. Si hay otra persona usando JOAPC, el build compite con lo que esté corriendo ahí: no lo canceles ni toques otros procesos.

Esta skill solo aplica si `hostname` es `JOANOTEBOOK`. En JOAPC o en la notebook con Windows, usá los comandos normales de `AGENTS.md`.

## Qué va a JOAPC y qué se queda local

| Tarea | Dónde |
|---|---|
| `assembleDebug`, `assembleRelease`, `bundleRelease`, `lintDebug`, `testDebugUnitTest` | JOAPC (primero), local si JOAPC no responde |
| Un solo test (`--tests "*X"`) | Local, salvo que ya esté el caché de Gradle frío |
| `git`, edición, `npm run lint` / `npm run build` del backend, lecturas | Local |

Cada ejecución mantiene el comportamiento de `AGENTS.md`: el reporte final dice dónde corrió cada comando.

## Datos fijos

El SSH está configurado en `~/.ssh/config` de la notebook (no versionado), con dos alias hacia el mismo usuario de Windows:

| Alias | Camino | Cuándo |
|---|---|---|
| `joapc` | LAN (`192.168.0.108`) | Estando en casa |
| `joapc-ts` | Tailscale (`100.108.158.91`) | Fuera de casa |

Variables usadas abajo. Son rutas de **JOAPC**, no de la notebook:

```bash
H=joapc                                                      # o joapc-ts
M='C:\Users\nico_\AndroidStudioProjects\MiniToolbox-build'   # copia de compilación en JOAPC
MF=C:/Users/nico_/AndroidStudioProjects/MiniToolbox-build    # la misma, con barras para tar/scp
JBR='C:\Program Files\Android\Android Studio\jbr'            # JDK 21 que exige el daemon de Gradle
```

`MiniToolbox-build` es una copia **solo de compilación**. El repo real de JOAPC (`...\MiniToolbox`) no se toca: puede estar en otra rama con trabajo propio. Nunca edites código en la copia: se pisa en cada sincronización.

## Paso 1. Elegir el camino

```bash
ssh -n joapc "echo OK" || ssh -n joapc-ts "echo OK"
```

- Responde `joapc`: `H=joapc`.
- Solo responde `joapc-ts`: `H=joapc-ts`.
- No responde ninguno: ir al **Paso 5** (compilar local) y decirlo en el reporte.

Si el SSH falla con `Permission denied`, no insistas ni pruebes otros usuarios: avisá al autor. La clave de esta notebook tiene que estar en `administrators_authorized_keys` o `authorized_keys` de JOAPC.

## Paso 2. Sincronizar el código

Se envía el árbol de trabajo actual (incluye cambios sin commitear) como un `tar` por SSH. JOAPC no tiene `rsync`. `git ls-files -co --exclude-standard` respeta `.gitignore`, así que no viaja `build/`, `.gradle/`, `.git/`, `local.properties` ni `keystore.properties` (unos 5,5 MB en 367 archivos al escribir esta skill).

```bash
git ls-files -co --exclude-standard | tar -cf - -T - | ssh $H "tar -xf - -C $MF"
```

`tar` no borra lo que ya no existe. Para los archivos versionados que borraste localmente:

```bash
git ls-files -d | while read -r f; do ssh -n $H "del /q \"$M\\${f//\//\\}\""; done
```

Limitación: un archivo **sin versionar** que borraste o renombraste queda viejo en JOAPC. Si Gradle falla por una clase duplicada o un recurso fantasma, borrá la copia y sincronizá de nuevo: `ssh -n $H "rmdir /s /q $M"`, y repetí el Paso 2 (y el 2b).

### 2b. Primera vez en una copia nueva

`local.properties` (con `sdk.dir` de Windows y las claves de métricas) y `app\keystore.properties` no viajan por SSH desde la notebook: se copian **dentro de JOAPC** desde el repo real:

```bash
O='C:\Users\nico_\AndroidStudioProjects\MiniToolbox'
ssh -n $H "mkdir $M & copy /y $O\\local.properties $M\\local.properties & copy /y $O\\app\\keystore.properties $M\\app\\keystore.properties"
```

Nunca copies el `local.properties` de la notebook: su `sdk.dir` es una ruta de Linux y rompería el build en Windows.

## Paso 3. Compilar en JOAPC

Siempre `ssh -n` (si no, el SSH de Windows puede quedar colgado). Se fija `JAVA_HOME` al JDK 21 de Android Studio: el `java` del `PATH` de JOAPC es el JDK 24. El shell remoto es `cmd`, no PowerShell.

```bash
ssh -n -o ServerAliveInterval=15 $H "cd /d $M && set \"JAVA_HOME=$JBR\" && gradlew.bat assembleDebug --console=plain"
```

Cambiá la tarea por la que corresponda (`testDebugUnitTest lintDebug`, `assembleRelease`, `bundleRelease`). Corrida de referencia: `assembleDebug` en frío por LAN tardó 1 min 27 s. Un build largo conviene lanzarlo en segundo plano y esperar su salida, no con `sleep`.

El resultado es el de Gradle en JOAPC: reportá la línea `BUILD SUCCESSFUL` o `BUILD FAILED` y los tests, tal cual.

## Paso 4. Traer artefactos e instalar en el teléfono

### 4a. Traer el APK o el AAB

```bash
scp "$H:$MF/app/build/outputs/apk/debug/app-debug.apk" .
scp "$H:$MF/app/build/outputs/bundle/release/app-release.aab" .
```

Ojo con el tamaño: el APK debug pesa unos 140 MB. Guardalo fuera del repo (por ejemplo en el directorio temporal), no lo commitees.

### 4b. Instalar

El teléfono se instala por uno de dos caminos, en este orden:

1. **En casa, depuración inalámbrica desde JOAPC.** Si el teléfono ya está emparejado con el `adb` de JOAPC:
   ```bash
   ssh -n joapc 'C:\Users\nico_\AppData\Local\Android\Sdk\platform-tools\adb.exe devices'
   ```
   Si lista el teléfono como `device`, instalá desde JOAPC sin traer nada:
   ```bash
   ssh -n joapc "C:\Users\nico_\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r $M\\app\\build\\outputs\\apk\\debug\\app-debug.apk"
   ```
2. **Fallback (falla lo anterior, no estás en casa o el teléfono no aparece): traer el APK e instalar desde la notebook** con el teléfono por cable. El `adb` de la notebook no está en el `PATH`:
   ```bash
   ~/Android/Sdk/platform-tools/adb devices
   ~/Android/Sdk/platform-tools/adb install -r app-debug.apk
   ```
   Si `devices` no lista el teléfono (o dice `unauthorized`), pedile al autor que lo conecte y acepte la depuración USB. No sigas sin teléfono.

Recordá las trampas de `AGENTS.md`: las builds debug mandan métricas al mismo endpoint que producción, así que no dispares eventos de prueba desde la app instalada.

## Paso 5. Fallback local

Si JOAPC no responde, compilá en la notebook con los mismos comandos de `AGENTS.md`, pero con `./gradlew` en vez de `.\gradlew.bat`. Si Gradle falla por la JVM, el daemon necesita JDK 21 (`gradle/gradle-daemon-jvm.properties`); apuntá `JAVA_HOME` a uno. Decí en el reporte que el build fue local y por qué.

## Qué está verificado y qué no

Verificado al escribir la skill (rama `docs/skill-remote-build`, `master @ 3bfc25d`):

- SSH con clave a `joapc` y `joapc-ts`.
- Sincronización por `tar` y `assembleDebug` en la copia de JOAPC por LAN: `BUILD SUCCESSFUL`.
- Traer el APK con `scp`.

**No** verificado:

- Borrado de archivos versionados (`git ls-files -d`).
- Cualquier tarea que no sea `assembleDebug` en JOAPC (`testDebugUnitTest`, `lintDebug`, `assembleRelease`, `bundleRelease`) y el firmado con `keystore.properties`.
- El build por Tailscale (solo se probó que el SSH responde).
- La instalación con `adb`, inalámbrica o por cable: el teléfono no estaba conectado.
