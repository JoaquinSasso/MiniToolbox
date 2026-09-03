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
- **Clave:** Desde la v1.3.2 proviene de `Tool.metricsKey`, un identificador estable e independiente de la ruta de navegación. Antes se usaba la ruta cruda.
- **Limitación histórica:** **[SESGO DETECTADO]** Hasta la v1.3.2, las aperturas por deep link (notificación, widget, acceso directo) se registraban **dos veces**: una en `MainActivity` y otra en el `NavGraph` al navegar. El sesgo afecta sólo a las herramientas con notificaciones o widgets, principalmente `water` y `pomodoro`. La caída visible en esas series a partir de la v1.3.2 es la corrección del sesgo, no una caída de uso.
- **Limitación histórica:** **[DATOS PERDIDOS]** Los dispositivos que abrían el detalle de un Pomodoro desde una notificación quedaban con el envío bloqueado de forma permanente (ver `metrics-pipeline-blockage-postmortem.md`). Toda la serie previa a agosto de 2026 subrepresenta a los usuarios de esa herramienta.

### `tools_dau`
- **Qué mide:** Dispositivos-día por herramienta. Cada dispositivo aporta como mucho **1** por herramienta y por día, sin importar cuántas veces la abrió.
- **Estructura:** Mapa de `tool_id` -> `count`.
- **Disponible desde:** v1.3.2. No existe serie anterior y no se puede reconstruir.
- **Uso correcto:** Es el **denominador** que le da sentido a `tools`. Permite calcular:
  - *Penetración:* `tools_dau[t] / daily_active` — qué proporción de la audiencia usa la herramienta.
  - *Intensidad:* `tools[t] / tools_dau[t]` — cuántas veces la usa quien la usa.
- **ADVERTENCIA:** `tools` por sí solo no distingue entre muchas personas usando algo una vez y pocas personas usándolo mucho. Sin `tools_dau`, cualquier ranking de "herramientas populares" es ambiguo.

### `tool_entry`
- **Qué mide:** Aperturas de cada herramienta, desglosadas por el lugar desde el que se entró.
- **Estructura:** Anidado, `tool_entry[tool_id][origen]` -> `count`.
- **Orígenes posibles:** `nav` (navegación interna), `notification`, `widget`, `shortcut`, `unknown`.
- **Disponible desde:** v1.3.2.
- **Semántica:** El total por herramienta coincide con `tools[t]`, ya que se registra en el mismo punto y con la misma deduplicación por cooldown.
- **Uso correcto:** Evaluar si las notificaciones y los widgets aportan uso real, y relativizar la importancia de la posición en el menú para las herramientas que se abren mayormente desde fuera de la app.
- **Nota:** Un origen no reconocido se agrupa en `unknown` en lugar de crear una clave nueva.

### `retention`
- **Qué mide:** Cruce entre la **antigüedad** del dispositivo y su **intensidad** de uso.
- **Estructura:** Anidado, `retention[antigüedad][intensidad]` -> `count`. Una marca por dispositivo y por día.
- **Categorías de antigüedad:** `age0_6`, `age7_29`, `age30_89`, `age90_179`, `age180p` (días desde el primer día registrado).
- **Categorías de intensidad:** `d1`, `d2_3`, `d4_7`, `d8_14`, `d15_28` (días activos dentro de una ventana de 28).
- **Disponible desde:** v1.3.2.
- **Cómo preserva el anonimato:** El cálculo ocurre **en el dispositivo**, con datos que nunca se envían (la lista de días activos y el primer día visto). Al servidor sólo viaja un `1` en una de 25 categorías posibles. No hay identificadores ni fechas individuales.
- **ADVERTENCIA — NO es retención por cohortes.** No responde "de los que instalaron en la semana X, cuántos siguen a los 7 días": eso exigiría seguir individuos en el tiempo. Lo que da es la distribución de intensidad cruzada con antigüedad.
- **Limitación:** Reinstalar la app o borrar sus datos **reinicia el historial**. Un usuario de dos años vuelve a aparecer como `age0_6.d1`. Sesga la distribución hacia abajo y no tiene corrección sin identificadores persistentes.
- **Limitación:** La ventana tarda 28 días en madurar. Toda instalación nueva cae en los tramos bajos por construcción, así que leer estos datos durante el primer mes de vida de una versión lleva a conclusiones falsas.

### `widgets`
- **Qué mide:** Actualizaciones o interacciones con Glance Widgets en la pantalla de inicio.
- **Uso correcto:** Medir el valor del ecosistema de widgets.

### `ads`
- **Qué mide:** Impresiones de anuncios (banner, intersticial, etc.).
- **Nota:** Sirve para comparar contra los datos de AdMob y detectar discrepancias de carga.

---

## 3.b. Metadatos del lote

### `schema_version`
- **Qué es:** Versión del formato del payload enviado por el cliente.
- **Valores:** `1` (v1.3.2 inicial), `2` (agrega `tools_dau`), `3` (agrega `tool_entry` y `retention`).
- **Para qué sirve:** Separar datos históricos si alguna vez cambia la semántica de un campo. Es imposible de agregar retroactivamente: sin este número, un cambio de significado mezcla para siempre los datos viejos con los nuevos.

### `client_health`
- **Qué mide:** Salud del propio pipeline de envío en el dispositivo.
- **Campos:** `dropped_batches`, `sanitized_keys`, `consecutive_failures`.
- **Para qué sirve:** Detectar dispositivos con problemas de envío. Sin esto, un dispositivo que dejó de reportar es indistinguible de uno que no se usa.
- **Nota:** No forma parte de los agregados diarios; se registra a nivel de lote.

### `auth_method`
- **Qué mide:** Cómo se autenticó cada lote: `appcheck` o `api_key`.
- **Dónde vive:** `metrics_ingest_batches`, `metrics_ingest_logs` y el rollup mensual `metrics_auth`.
- **Para qué sirve:** Es el criterio para retirar la autenticación por API key. Mientras haya tráfico con `api_key`, eliminar esa rama del backend dejaría a esos dispositivos bloqueados de forma permanente.

---

## 4. Métricas de Localización

### `lang_primary` / `lang_secondary`
- **Qué mide:** Idiomas configurados en el sistema del usuario.
- **Cuándo incrementa:** Una vez por día por dispositivo.
- **Uso correcto:** Priorizar traducciones y soporte local.

---

## Nota sobre zonas horarias

El campo `day` se calcula con el **reloj local del dispositivo** (`SimpleDateFormat("yyyy-MM-dd")` sin zona fija). Un mismo día agrupa hasta 24 husos distintos.

Es tolerable para conteos diarios, pero invalida cualquier análisis horario y cualquier comparación de días exactos entre dispositivos. Los bordes de día tienen un solapamiento de ±1 día que no se puede deshacer a posteriori.

---

## Resumen de Integridad
Para cualquier cifra pública (README, Portfolio, LinkedIn), se debe priorizar:
1. **Google Play Console** para "Instalaciones Totales" y "Calificaciones".
2. **`daily_active`** para "Usuarios Activos" (solo serie post 17/08/2026).
3. **`tools`** para "Funcionalidades más populares".

**Nunca utilizar `versions_first_seen` como equivalente a "Usuarios" sin aclarar la semántica de actualización.**