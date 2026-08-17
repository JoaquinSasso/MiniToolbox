
# 🏛️ Arquitectura de MiniToolbox

Este documento describe la estructura general y las decisiones de arquitectura de la aplicación **MiniToolbox**.

---

## ✨ Tecnologías principales

- **Kotlin**: Lenguaje principal del proyecto.
- **Jetpack Compose**: Framework para la UI declarativa.
- **Material Design 3**: Lineamientos visuales.
- **ViewModel**: Gestión del estado y ciclo de vida.
- **DataStore**: Persistencia ligera de configuraciones y datos.
- **Coroutines**: Tareas asincrónicas y reactividad básica.
- **Protobuf Lite**: Serialización binaria eficiente para datasets estáticos (países).

---

## 📂 Estructura de paquetes

```
com.joasasso.minitoolbox
 ├── nav                  # Navegación y definición de pantallas (NavGraph, Screen)
 ├── tools                # Todas las herramientas agrupadas
 │    ├── calculadoras     # Calculadoras y conversores
 │    ├── generadores      # Generadores aleatorios y selectores
 │    ├── info             # Herramientas informativas (países, signos, edad...)
 │    ├── juegos           # Minijuegos
 │    ├── medicion         # Herramientas de medición (nivel, luz, regla...)
 │    ├── recordatorios    # Pomodoro, agua, rachas
 │    └── data             # Repositorios, modelos de datos, DataStore, Protobuf
 ├── ui.components         # Componentes UI reutilizables (ej: TopBar)
 ├── ui.theme              # Definición de tema, colores, tipografía
 ├── viewmodel             # ViewModel central para categorías
 ├── MainActivity.kt        # Entry point de la app
 └── NavGraph.kt            # Navegación principal
```

---

## 🧠 Principios de diseño

- **Single Activity + Compose Navigation**  
  La app se estructura como una única `MainActivity` con navegación entre pantallas mediante un `NavGraph` y `Screen.kt`.

- **Paquetes por dominio funcional**  
  Cada tipo de tool tiene su propio subpaquete, lo que facilita el mantenimiento y la escalabilidad.

- **Componentes reutilizables**  
  Elementos comunes como el `TopAppBar` se centralizan en `ui.components` para evitar duplicación de código.

- **Persistencia eficiente**  
  - `DataStore` se usa para configuraciones ligeras como Pomodoro, agua, QR, etc.
  - `Protobuf Lite` se usa para el dataset de países (binario preprocesado optimizado).

---

## 🚀 Flujos principales

### 🔹 Navegación
Se gestiona con un `NavGraph` y un `Screen.kt` que definen los destinos. Las herramientas se activan desde un menú de categorías.

### 🔹 Persistencia
- **DataStore**: usado para guardar preferencias y estados simples de herramientas.
- **Protobuf Lite**: se carga desde `assets/countries_dataset.pb`, parseando el binario a modelos de la app.

### 🔹 Gestión de estado
- `CategoryViewModel` coordina las categorías principales.
- Cada tool maneja su propio estado de UI con Compose State, sin un ViewModel individual (salvo casos específicos como Pomodoro con servicio).

---

## 📌 Decisiones técnicas clave

- Se eligió **Protobuf Lite** para el dataset de países por su balance entre tamaño y velocidad de parseo.
- Se unificó el `TopAppBar` en un componente único para facilitar el mantenimiento y asegurar consistencia visual.
- `DataStore` se eligió por sobre `SharedPreferences` para estar alineado con las recomendaciones modernas de Jetpack.

---

## 🔐 Gestión de Permisos

- **Higiene de permisos**: Un permiso se debe agregar únicamente en el mismo commit que el código que lo requiere. Nunca se deben declarar permisos "por si acaso" ni anticipando funcionalidades futuras.
- **Data Safety**: Los permisos huérfanos impactan negativamente en el formulario de Data Safety de Google Play, pueden ser motivo de rechazo en revisión y comprometen la confianza del usuario.
- **Auditoría**: Periódicamente se debe revisar el manifiesto mergeado para asegurar que ninguna librería esté inyectando permisos no deseados.

---
