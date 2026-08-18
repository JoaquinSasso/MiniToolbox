# Informe de Deuda Técnica: Migración a Protobuf-Lite

## Resumen
*   **Dificultad:** Media.
*   **Impacto:** Reducción estimada de ~1MB en el APK final y menor consumo de memoria en runtime.
*   **Estado Actual:** El proyecto utiliza `com.google.protobuf:protobuf-java` (versión full), la cual no está optimizada para Android y añade descriptores/reflexión innecesarios.

## Riesgos y Validaciones
> [!IMPORTANT]
> El API Lite elimina capacidades de reflexión. Se debe verificar que no existan inspecciones dinámicas de mensajes.

*   **Puntos de Uso Identificados:**
    *   [`GuessCapitalScreen.kt`](file:///C:/Users/nico_/StudioProjects/MiniToolbox/app/src/main/java/com/joasasso/minitoolbox/tools/entretenimiento/minijuegos/GuessCapitalScreen.kt#L314)
    *   [`GuessFlagScreen.kt`](file:///C:/Users/nico_/StudioProjects/MiniToolbox/app/src/main/java/com/joasasso/minitoolbox/tools/entretenimiento/minijuegos/GuessFlagScreen.kt#L274)
    *   [`CountriesInfoScreen.kt`](file:///C:/Users/nico_/StudioProjects/MiniToolbox/app/src/main/java/com/joasasso/minitoolbox/tools/info/CountriesInfoScreen.kt#L62)
*   **Dataset:** Validar que `countries.proto.bin` se deserializa correctamente con el nuevo `CountryList.parseFrom(bytes)`.

## Plan de Acción

### 1. Configuración de Dependencias
*   En `libs.versions.toml`: Reemplazar `protobuf-java` por `protobuf-javalite`.
*   En `app/build.gradle.kts`: **Eliminar** el bloque `configurations.configureEach { exclude(group = "com.google.protobuf", module = "protobuf-javalite") }` que bloquea la versión Lite.

### 2. Regeneración de Código
*   Regenerar `CountryOuterClass.java` usando el plugin de Protobuf con la opción `lite`.
*   *Nota:* Si se usa el plugin de Gradle `com.google.protobuf`, configurar `generateProtoTasks { all().each { task -> task.builtins { java { option 'lite' } } } }`.

### 3. Reglas de Optimización (R8)
*   Asegurar que `proguard-rules.pro` incluya las reglas necesarias para Protobuf-Lite para evitar errores de "NoSuchMethodError" tras la minificación.

### 4. Verificación
*   Ejecutar los minijuegos de Capitales y Banderas para confirmar que el dataset de países carga correctamente.
*   Comparar tamaño del APK antes y después del cambio.