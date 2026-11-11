# Glosario de Métricas y Telemetría

[English](metrics-glossary.en.md)

Este documento define la semántica exacta de los contadores utilizados en el sistema de telemetría propia de MiniToolbox. Su objetivo es evitar errores de interpretación estadística al redactar informes o documentos públicos.

---

## 1. Métricas de Actividad General

### `app_open`
- **Qué mide:** Cada vez que se crea `MainActivity`, incluida una recreación por cambio de configuración (tema o idioma). Volver a la app desde segundo plano sin que la actividad se destruya no cuenta.
- **Cuándo incrementa:** En el `LaunchedEffect(Unit)` de `MainActivity.onCreate` (`MainActivity.kt:58`).
- **Limitación Histórica:** **[SESGO DETECTADO]** Entre el 24/09/2025 y el 17/08/2026, este contador duplicaba la primera apertura del día por un bug de colisión con `daily_active`. Toda la serie histórica en ese rango tiene un sesgo de aproximadamente +1 apertura por dispositivo/día.
- **Uso correcto:** Frecuencia de uso bruta. No sirve para contar usuarios.

### `daily_active` (DAU)
- **Qué mide:** Dispositivos activos únicos por día.
- **Cuándo incrementa:** Una vez por día calendario, según el reloj local del dispositivo, en la primera apertura de ese día. No es una ventana de 24 horas: abrir a las 23:59 y a las 00:01 cuenta dos días.
- **Guarda:** Guarda el último día contado en `SharedPreferences` (`metrics_daily_once`, `Metrics.kt:122`).
- **Uso correcto:** Es la métrica más fiel para medir el tamaño de la audiencia diaria.

---

## 2. Métricas de Instalación y Versiones

### `versions_first_seen`
- **Qué mide:** La primera vez que un dispositivo se reporta utilizando una versión específica.
- **Cuándo incrementa:** Una vez por cada versión de la app que pase por el dispositivo.
- **Semántica:** Si un usuario instala la v1.0, actualiza a la v1.1 y luego a la v1.2, este contador sumará **3 unidades** en total (una por cada versión).
- **ADVERTENCIA:** **NO mide usuarios únicos totales.** El número total acumulado de este contador está inflado por el factor de actualización (promedio de versiones instaladas por usuario).

### `versions`
- **Qué mide:** Dispositivos-día por versión. Cada dispositivo suma **1** por día a la versión que tiene instalada, sin importar cuántas veces abra la app (`ensureDailyVersionAndLangMarks` en `AggregatesRepository.kt`). No equivale a `app_open`.
- **Uso correcto:** Identificar qué porcentaje de la base de usuarios sigue en versiones antiguas. La suma de todas las versiones en un período da los dispositivos-día de ese período.

---

## 3. Métricas de Funcionalidades

### `tools`
- **Qué mide:** Aperturas de herramientas.
- **Cuándo incrementa:** En el `NavGraph`, cuando la ruta actual cambia a una que pertenece a una herramienta (`NavGraph.kt:69-78`). Un debouncer descarta repeticiones de la misma herramienta dentro de los 5 segundos (`TOOL_USAGE_METRIC_COOLDOWN_MS`).
- **Limitación:** Pasar de una subpantalla a otra de la misma herramienta (por ejemplo, de la lista de Pomodoro al detalle) vuelve a contar si pasaron más de 5 segundos. Está en el backlog como `tool-count-per-route`.
- **Estructura:** Mapa de `tool_id` -> `count`.
- **Nota:** No mide tiempo de permanencia, solo intención de uso.
- **Clave:** Desde la v1.3.2 proviene de `Tool.metricsKey`, un identificador estable e independiente de la ruta de navegación. Con la migración a navegación type-safe, las subpantallas se unifican bajo la clave principal de su herramienta: `pomodoro_list` y `pomodoro/detail` reportan bajo `pomodoro`, `water_stats` bajo `water`, y las subtelas de divisor de gastos bajo `meetings`.
- **Herramientas retiradas (2026-10-30):** Se eliminaron del catálogo de la app las herramientas `zodiac_sign`, `multiverse_me`, `quick_calcs`, `basic_phrases` (cuya clave histórica en el backend era `quotes`), `countries_info`, `percentage`, `coin_flip`, `todo`, `age_calculator`, `countdown`, `guess_capital`, `decimal_binary`, `password_generator` y `dice`. Sus claves se conservan en `TOOL_ROUTE_MAP` del backend y en `TOOL_CATALOG` del dashboard para mantener la lectura de datos históricos, pero dejan de recibir eventos a partir de las versiones posteriores a este cambio.
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
- **Qué mide:** Toques en los widgets, no actualizaciones. Estructura: mapa de clave -> `count`.
- **Claves:**
  - `widget_water_normal`, `widget_water_mini`: botones de sumar y quitar agua (`AguaCallback.kt`).
  - `widget_flashlight`, `widget_flashlight_mini`: encender o apagar la linterna.
  - `widget_shortcuts`: se cuenta en `MainActivity` para **cualquier** apertura con ruta de inicio, incluidas las que vienen de notificaciones. No mide solo el widget de favoritos (backlog: `widget-shortcuts-mislabel`).
- **Limitación histórica:** Desde `22f6c43` (24/09/2025) hasta la rama `fix/water-widget-metrics-optout`, quitar agua desde el widget sumaba la métrica aunque el usuario hubiera desactivado las métricas. Como hasta la rama `fix/upload-respects-optout` el envío tampoco revisaba el opt-out, esos toques pudieron llegar al servidor.
- **Uso correcto:** Comparar el uso entre widgets de agua y de linterna. `widget_shortcuts` no sirve para medir el widget de favoritos.

### `ads` (retirada)
- **Qué medía:** Impresiones de anuncios (banner, intersticial, rewarded).
- **Estado:** Retirada junto con los anuncios y el modo PRO. La primera versión posterior a la 1.4.0 ya no la envía (`schema_version` 4), y el backend ignora el campo si un cliente viejo lo sigue mandando: no lo escribe en Firestore ni lo devuelve en `metricsDaily` ni en `metricsSummary`.
- **Datos históricos:** Los documentos de `metrics_daily` anteriores al cambio conservan el mapa `ads`; no se borraron. Para consultarlos hay que leer Firestore directamente, porque la API ya no los expone.
- **Clave relacionada:** `pro` en `tools` no fue nunca una herramienta: era la pantalla de compra del modo PRO. Puede aparecer en datos históricos y se excluye al analizar el uso de herramientas.

---

## 3.b. Metadatos del lote

### `schema_version`
- **Qué es:** Versión del formato del payload enviado por el cliente.
- **Valores:** `1` (v1.3.2 inicial), `2` (agrega `tools_dau`), `3` (agrega `tool_entry` y `retention`), `4` (quita `ads`).
- **Nota:** El backend hoy no persiste este número; viaja en el payload pero no se guarda en Firestore.
- **Para qué sirve:** Separar datos históricos si alguna vez cambia la semántica de un campo. Es imposible de agregar retroactivamente: sin este número, un cambio de significado mezcla para siempre los datos viejos con los nuevos.

### `client_health`
- **Qué mide:** Salud del propio pipeline de envío en el dispositivo.
- **Campos:** `dropped_batches`, `sanitized_keys`, `consecutive_failures`.
- **Para qué sirve:** Detectar dispositivos con problemas de envío. Sin esto, un dispositivo que dejó de reportar es indistinguible de uno que no se usa.
- **Nota:** No forma parte de los agregados diarios; se registra a nivel de lote.

### `auth_method` (retirada)
- **Qué medía:** Cómo se autenticó cada lote: `appcheck` o `api_key`.
- **Estado:** La ingesta acepta solo App Check (`docs/decisions/appcheck-only-ingest.md`), así que el campo dejó de escribirse en `metrics_ingest_batches` y `metrics_ingest_logs`.
- **Rollup `metrics_auth`:** Conserva el nombre y la forma por continuidad. Desde el cambio, `appcheck` y `by_version.<versión>.appcheck` equivalen a lotes aceptados. Los meses anteriores también tienen `api_key`, que queda en Firestore pero `metricsAuth` ya no devuelve. Para un total de lotes aceptados que valga también para esos meses, usar `batches`.
- **Lotes rechazados:** No se cuentan en Firestore. Se ven en Cloud Logging como `ingest_unauthorized`, con la `app_version` declarada.

---

## 4. Métricas de Localización

### `lang_primary` / `lang_secondary`
- **Qué mide:** Idiomas configurados en el sistema del usuario.
- **Cuándo incrementa:** Una vez por día por dispositivo, junto con `versions`. `lang_secondary` solo cuenta si el segundo idioma es distinto del primero.
- **Nota:** Como con `versions`, la suma de `lang_primary` en un período da los dispositivos-día de ese período.
- **Uso correcto:** Priorizar traducciones y soporte local.

---

## Nota sobre el opt-out

Desactivar las métricas en Acerca de corta el registro y el envío. Se cancelan los envíos encolados y lo acumulado que no se había enviado se descarta: se marca como enviado sin enviarlo (`AggregatesRepository.discardUnsent`), así que reactivarlas sube solo lo registrado después. El worker repite el chequeo antes de enviar, por si ya estaba encolado.

- **Limitación histórica:** hasta la rama `fix/upload-respects-optout`, el envío no revisaba el opt-out. Lo acumulado antes de desactivar se subía igual, y en cada salida de la app se encolaba un envío.
- **Límite conocido:** si un envío ya estaba en vuelo (la petición HTTP salió) cuando el usuario desactiva, ese lote llega al servidor.

---

## Nota sobre zonas horarias

El campo `day` se calcula con el **reloj local del dispositivo** (`SimpleDateFormat("yyyy-MM-dd")` sin zona fija). Un mismo día agrupa hasta 24 husos distintos.

Es tolerable para conteos diarios, pero invalida cualquier análisis horario y cualquier comparación de días exactos entre dispositivos. Los bordes de día tienen un solapamiento de ±1 día que no se puede deshacer a posteriori.

---

## Resumen de Integridad
Para cualquier cifra pública (README, Portfolio, LinkedIn), se debe priorizar:
1. **Google Play Console** para "Instalaciones Totales" y "Calificaciones".
2. **`daily_active`** para "Usuarios Activos" (solo serie post 17/08/2026).
3. **`tools_dau`** para "Funcionalidades más populares", con `tools` como medida de intensidad. Si solo se dispone de `tools` (por ejemplo, en el export Markdown del dashboard), aclarar que son aperturas y no personas.

**Nunca utilizar `versions_first_seen` como equivalente a "Usuarios" sin aclarar la semántica de actualización.**