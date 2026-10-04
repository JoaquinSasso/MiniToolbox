---
name: tool-catalog-change
description: >-
  Checklist de todos los puntos que hay que tocar al agregar, quitar, renombrar o fusionar
  una herramienta de MiniToolbox (registry, navegación, strings, métricas en cliente y
  backend, dashboard y tests). Usar siempre que cambie el catálogo de herramientas o la ruta
  de una pantalla.
---

# Cambiar el catálogo de herramientas

Una herramienta está definida en ocho lugares que no se validan entre sí en tiempo de compilación. Si te olvidás de uno, el build pasa pero la métrica termina en `other`, el deep link no resuelve o el dashboard no la muestra.

## Antes de empezar

- **Quitar, fusionar o renombrar rompe la continuidad de métricas** (`DECISIONS.md` §4). Hace falta la aprobación explícita del autor antes de tocar código. Pedila y esperá.
- **Leé una herramienta existente como referencia.** `ZodiacSign` es la más simple; `Water` tiene subpantallas.

## Puntos de contacto (en este orden)

| # | Archivo | Qué hacer |
|--:|---|---|
| 1 | `app/.../nav/Screen.kt` | Agregar `@Serializable @SerialName("<ruta>") data object X : Screen()` con `route`, y su caso en el `when` de `fromRouteString` |
| 2 | `app/.../NavGraph.kt` | Agregar `composable<Screen.X> { ... }` |
| 3 | `app/.../tools/ToolRegistry.kt` | Agregar la entrada `Tool(...)` con `name`, `category`, `subCategory`, `summary` y `svgResId` |
| 4 | `app/src/main/res/values{,-es}/strings_tools.xml` y `tool_sum.xml` | Nombre y resumen en inglés y en español |
| 5 | `app/src/main/res/values{,-es}/<herramienta>_strings.xml` | Los textos propios de la pantalla, en los dos idiomas |
| 6 | `app/src/main/res/drawable/` | El ícono referenciado en `svgResId` |
| 7 | `app/src/test/.../metrics/ToolMetricsKeysTest.kt` | Agregar la clave a `expectedKeys`. **Solo al agregar una herramienta**, nunca para "arreglar" un test roto por un renombre |
| 8 | `backend/functions/src/index.ts` | Agregar la clave a `TOOL_ROUTE_MAP` (o a `EXTRA_KNOWN_TOOLS`). Sin esto, el backend la guarda como `other` |
| 9 | `dashboard/public/index.html` | Agregar la entrada en `TOOL_CATALOG`, con su nombre visible |

Si la herramienta tiene subpantallas, agregalas también en `ToolRoutes.findToolByScreen` para que reporten bajo la clave principal, y en `ScreenSerializationTest` si reciben argumentos.

## Renombrar una ruta

No toques la clave. En la entrada de `ToolRegistry`, fijá `metricsKey = "<clave anterior>"`. Agregá la ruta vieja como alias en `Screen.fromRouteString`, porque los widgets y notificaciones ya emitidos traen la ruta anterior.

**Los favoritos se rompen aunque agregues el alias.** Se guardan como `screen.route` en `data/FavoritesDataStore.kt` y se resuelven por igualdad exacta (`CategoriesScreen.kt:133`, `widgets/FavoriteToolsWidget.kt:61`), no con `fromRouteString`. Un renombre hace desaparecer el favorito sin aviso. Antes de renombrar, migrá los valores guardados dentro de un `edit {}` o cambiá esas dos búsquedas para que usen `Screen.fromRouteString`.

## Quitar una herramienta

1. Con la aprobación del autor, borrá los puntos 1 a 6 y los recursos propios: strings, drawables y su DataStore en `data/`.
2. **No borres** la clave del backend (punto 8) ni del dashboard (punto 9): los datos históricos la siguen usando. En `ToolMetricsKeysTest`, quitala de `expectedKeys`.
3. Si un usuario la tenía como favorita, la ruta guardada deja de resolver. Hoy se descarta en silencio (`mapNotNull` en `CategoriesScreen.kt:133` y en `widgets/FavoriteToolsWidget.kt:60`). En una fusión, conviene migrar el favorito a la herramienta nueva.
4. Registrá en `docs/metrics-glossary.md` la fecha y el commit desde los que la clave deja de recibir datos.

## Fusionar herramientas

Es un "quitar" de cada herramienta original más un "agregar" de la nueva. Decidí con el autor si la nueva hereda la clave de una de las originales (y la serie continúa) o arranca una clave nueva. Documentá la decisión en el glosario.

## Verificación

```powershell
.\gradlew.bat testDebugUnitTest --tests "*ToolMetricsKeysTest" --tests "*ScreenSerializationTest"
.\gradlew.bat assembleDebug lintDebug
cd backend/functions; npm run lint; npm run build
```

Después, buscá en todo el repo los restos de la clave (en una herramienta quitada solo deberían quedar el backend, el dashboard y la documentación):

```powershell
git grep -n "<clave>"
```
