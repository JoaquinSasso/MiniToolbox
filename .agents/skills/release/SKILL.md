---
name: release
description: >-
  Procedimiento para preparar una versión de MiniToolbox para Google Play: subir la versión,
  verificar el build de release con R8, generar el AAB firmado y etiquetar el commit. Usar
  cuando el autor pida preparar, versionar o publicar una release.
---

# Preparar una release

El repo no tiene tags anteriores a esta skill, así que no se sabe qué commit corresponde a cada versión publicada. Desde ahora, cada versión que se sube a Play lleva su tag.

## Pasos

1. **Partí de `master` actualizado, sin cambios locales**: `git checkout master; git pull; git status`.
2. **Revisá la continuidad de métricas.** Corré la verificación de la skill `metrics-pipeline` y confirmá que toda `metricsKey` de `ToolRegistry` exista en `KNOWN_TOOLS` del backend. Si falta alguna, frená: esa herramienta reportaría como `other`.
3. **Subí la versión.** En una rama `chore/release-X.Y.Z`, editá `app/build.gradle.kts` (`defaultConfig`): `versionCode` +1 y `versionName = "X.Y.Z"`. Commit: `chore(release): X.Y.Z (<versionCode>)`.
4. **Verificá el build de release.** CI no compila con R8, así que este es el único chequeo del minificado:
   ```powershell
   .\gradlew.bat testDebugUnitTest lintDebug assembleRelease
   ```
5. **Generá el AAB firmado.** Requiere `app/keystore.properties` (no versionado). Si no existe, el build sale sin firmar: avisale al autor y no inventes credenciales.
   ```powershell
   .\gradlew.bat bundleRelease
   ```
   El archivo queda en `app/build/outputs/bundle/release/`.
6. **Cerrá con el autor.** La subida a Play Console la hace el autor. Pasale la ruta del AAB y un resumen de cambios desde el último tag (`git log --oneline <tag-anterior>..HEAD`).
7. **Etiquetá cuando el autor confirme que la versión se publicó:**
   ```powershell
   git tag -a vX.Y.Z -m "MiniToolbox X.Y.Z (versionCode N)"
   ```
   El push del tag (`git push origin vX.Y.Z`) y la GitHub Release se hacen solo si el autor lo pide.

## No hacer

- No reutilices un `versionCode`: Play lo rechaza.
- No commitees `keystore.properties`, `*.jks`, `*.aab` ni `*.apk` (están en `.gitignore`).
- No hagas deploy del backend como parte de la release de la app. Si la release depende de un cambio de backend, ese deploy va **antes** de publicar la app y lo hace el autor.
