# PRODUCT_BACKLOG.md

> **Propósito:** decisiones de producto, features, y nice-to-haves. Los agentes agregan
> ítems aquí cuando los encuentran durante trabajo técnico. No mezclar con deuda técnica
> (eso va en `SCRATCHPAD.md`).
>
> **Última actualización:** 2026-09-04
>
> **Fuente inicial:** sección 4-5 de `docs/auditoria_opus.md`, filtrada contra código real.

<!-- Status: [ ] pendiente | [?] requiere decisión del autor | [x] decidido | [—] descartado -->

---

## Catálogo de herramientas

- [?] **Eliminar herramientas de bajo uso.** La auditoría sugiere eliminar `zodiac_sign` (33 usos), `quotes` (34), `multiverse_me` (45), `countries_info` (22). Eliminar `quotes` de paso resuelve la limitación conocida #4 y saca Gson. Eliminar `countries_info` no afecta el benchmark de Protobuf (el dataset lo siguen usando `guess_flag` y `guess_capital`). Fuente: auditoria_opus.md §5.
- [?] **Fusionar herramientas de azar.** `dice` + `coin_flip` + `selector_wheel` + `group_selector` → una "Aleatorio" con modos (238 usos combinados). Fuente: auditoria_opus.md §5.
- [?] **Fusionar calculadoras.** `decimal_binary` + `percentage` + `unit_converter` → una "Calculadora" con pestañas (108 usos combinados). Fuente: auditoria_opus.md §5.
- [?] **Agregar escáner QR/barcode.** ML Kit sobre CameraX. Completa el par con el generador existente. Infraestructura de cámara ya existe (lupa, fotómetro). Fuente: auditoria_opus.md §5.
- [?] **Agregar sonómetro.** AudioRecord + PCM + dBFS. Completa el clúster de instrumentos. Requiere permiso RECORD_AUDIO y documentar limitación de calibración. Fuente: auditoria_opus.md §5.

## UI/UX

- [ ] **Onboarding por herramienta.** AnimatedVisibility con tarjeta de primera vez en herramientas de sensores (brújula: calibración en ocho, nivel: superficie de referencia, AR: mover para detectar planos). Persistir en DataStore. El patrón ya existe en AR Ruler con StatusBanner. Fuente: auditoria_opus.md D1.
- [ ] **Compartir resultado desde más herramientas.** Candidatos por uso: regla AR (captura con mediciones → `graphicsLayer().toImageBitmap()`), marcador de truco (resultado final), generador de contraseñas (con `EXTRA_IS_SENSITIVE`). Hoy solo 3 herramientas tienen ACTION_SEND. Fuente: auditoria_opus.md D3.
- [ ] **Landscape en regla AR y nivel de burbuja.** El mecanismo `LockScreenOrientationIfAllowed` ya está preparado para hacerlo por pantalla. No para las 33 herramientas, solo para las dos donde el usuario naturalmente gira el teléfono. Fuente: auditoria_opus.md D6.
- [ ] **GIFs del README.** Pendiente por decisión. Depende de cerrar cambios de UI/UX. Fuente: auditoria_opus.md §0.

## Dashboard y métricas

- [ ] **Filtro por rango de fechas en el dashboard.** Sin esto no se puede responder "qué herramientas se usaron en los últimos 90 días". Fuente: auditoria_opus.md §5.
- [ ] **Retención por cohorte de versión.** Cruzar `versions_first_seen` con `versions` por día. Fuente: auditoria_opus.md D4.
- [ ] **Ratio herramientas por sesión.** `tools / daily_active`. Dato actual ~1,14; sugiere uso como acceso directo, no como catálogo. Implicación: widgets y shortcuts importan más que la pantalla de categorías. Fuente: auditoria_opus.md D4.

## Internacionalización

- [ ] **Soporte RTL verificado.** `fa` (persa) aparece en datos de uso. Requiere `start`/`end` en vez de `left`/`right` en modifiers. Revisión de una tarde. Fuente: auditoria_opus.md D5.
