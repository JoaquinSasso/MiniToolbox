# Glosario de Métricas y Telemetría

Este documento define la semántica exacta de los contadores utilizados en el sistema de telemetría propia de MiniToolbox. Su objetivo es evitar errores de interpretación estadística al redactar informes o documentos públicos.

---

## 1. Métricas de Actividad General

### `app_open`
- **Qué mide:** Cada vez que la `MainActivity` se crea o se reinicia (start).
- **Cuándo incrementa:** En el `LaunchedEffect` de la pantalla principal.
- **Limitación Histórica:** **[SESGO DETECTADO]** Entre el 24/09/2025 y el 17/08/2026, este contador duplicaba la primera apertura del día por un bug de colisión con `daily_active`. Toda la serie histórica en ese rango tiene un sesgo de aproximadamente +1 apertura por dispositivo/día.
- **Uso correcto:** Frecuencia de uso bruta. No sirve para contar usuarios.

### `daily_active` (DAU)
- **Qué mide:** Dispositivos activos únicos por día.
- **Cuándo incrementa:** Exactamente una vez cada 24hs (según reloj local del dispositivo) al abrir la app.
- **Guarda:** Se apoya en una marca de tiempo en `SharedPreferences`.
- **Uso correcto:** Es la métrica más fiel para medir el tamaño de la audiencia diaria.

---

## 2. Métricas de Instalación y Versiones

### `versions_first_seen`
- **Qué mide:** La primera vez que un dispositivo se reporta utilizando una versión específica.
- **Cuándo incrementa:** Una vez por cada versión de la app que pase por el dispositivo.
- **Semántica:** Si un usuario instala la v1.0, actualiza a la v1.1 y luego a la v1.2, este contador sumará **3 unidades** en total (una por cada versión).
- **ADVERTENCIA:** **NO mide usuarios únicos totales.** El número total acumulado de este contador está inflado por el factor de actualización (promedio de versiones instaladas por usuario).

### `versions`
- **Qué mide:** Uso total por versión (equivalente a `app_open` pero segmentado).
- **Uso correcto:** Identificar qué porcentaje de la base de usuarios sigue en versiones antiguas.

---

## 3. Métricas de Funcionalidades

### `tools`
- **Qué mide:** Interacciones con herramientas específicas (clic en el menú o apertura).
- **Estructura:** Mapa de `tool_id` -> `count`.
- **Nota:** No mide tiempo de permanencia, solo intención de uso.

### `widgets`
- **Qué mide:** Actualizaciones o interacciones con Glance Widgets en la pantalla de inicio.
- **Uso correcto:** Medir el valor del ecosistema de widgets.

### `ads`
- **Qué mide:** Impresiones de anuncios (banner, intersticial, etc.).
- **Nota:** Sirve para comparar contra los datos de AdMob y detectar discrepancias de carga.

---

## 4. Métricas de Localización

### `lang_primary` / `lang_secondary`
- **Qué mide:** Idiomas configurados en el sistema del usuario.
- **Cuándo incrementa:** Una vez por día por dispositivo.
- **Uso correcto:** Priorizar traducciones y soporte local.

---

## Resumen de Integridad
Para cualquier cifra pública (README, Portfolio, LinkedIn), se debe priorizar:
1. **Google Play Console** para "Instalaciones Totales" y "Calificaciones".
2. **`daily_active`** para "Usuarios Activos" (solo serie post 17/08/2026).
3. **`tools`** para "Funcionalidades más populares".

**Nunca utilizar `versions_first_seen` como equivalente a "Usuarios" sin aclarar la semántica de actualización.**
