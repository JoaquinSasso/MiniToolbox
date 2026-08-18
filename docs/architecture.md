# Arquitectura de MiniToolbox

Este documento describe la arquitectura real del sistema MiniToolbox. Ha sido validado contra el código fuente para asegurar que representa la implementación existente, incluyendo sus asimetrías y limitaciones.

**Última verificación:** 18 de agosto de 2026
**Commit verificado:** `d43a3d53f2d6703d1cc04dd09aab114902cae5a7`

---

## 1. Vista General de Componentes

La aplicación sigue un modelo de **módulo único** (`:app`) con una orquestación centralizada en `MainActivity` mediante capas de Composición.

### Diagrama A: Capas y Gestión de Estado

Este diagrama muestra cómo conviven las dos estrategias de estado en la app. Se destaca visualmente la asimetría entre pantallas con arquitectura formal (ViewModel) y pantallas con lógica embebida (Local State).

```mermaid
graph TD
    subgraph UI_Layer ["Capa de UI (Jetpack Compose)"]
        MA[MainActivity] --> CG[ConsentGateProvider]
        CG --> PSP[ProStateProvider]
        PSP --> MT[MiniToolboxTheme]
        MT --> NG[NavGraph]
        
        subgraph Screens ["Pantallas / Tools"]
            direction TB
            VM_Screens["Screens con ViewModel<br/>(Categories, Minesweeper)"]
            State_Screens["Screens con Local State<br/>(Pomodoro, Agua, Calculadoras...)"]
            style VM_Screens fill:#e1f5fe,stroke:#01579b
            style State_Screens fill:#fff9c4,stroke:#fbc02d
        end
    end

    subgraph Logic_State ["Lógica y Estado"]
        VM[ViewModels]
        CS[Compose State]
    end

    subgraph Services_Background ["Servicios y Background"]
        subgraph Ads_Billing ["Ads & Billing"]
            BM[BillingClientWrapper]
            AM[AdsManager]
            IM[InterstitialManager]
            RM[RewardedManager]
            PR[ProRepository]
        end
        PAS[PomodoroAlarmService]
        WM[WorkManager - UploadMetrics]
        GW[Widgets Glance]
    end

    subgraph Data_Layer ["Capa de Datos"]
        DS[(19 DataStores Preferences)]
        PB[Protobuf - Countries Data]
    end

    VM_Screens -.-> VM
    State_Screens -.-> CS
    VM --> DS
    CS --> DS
    PAS --> DS
    WM --> DS
    GW --> DS
    NG --> Ads_Billing
    State_Screens -.-> PAS
    GW -- "Metrics" --> WM
```

---

## 2. Subsistema de Telemetría (Privacidad por Diseño)

El pipeline de métricas es el componente más diferenciador del sistema. Está diseñado para recolectar datos de uso sin comprometer la privacidad del usuario: **no existen identificadores de dispositivo, ni de sesión, ni rastro de eventos individuales.**

### Diagrama B: Pipeline de Telemetría End-to-End

```mermaid
graph TD
    subgraph App ["App (Android)"]
        E[Emisor: Metrics.kt] --> Gate{isMetricsEnabled?}
        Gate -- "No" --> End[Drop]
        Gate -- "Yes" --> AR[AggregatesRepository]
        AR -- "Delta Diario Local" --> MDS[(MetricsDataStore)]
        
        MDS -- "Lectura Payload" --> UM[UploadMetricsWorker]
        UM -- "Trigger" --> US[UploadScheduler]
    end

    subgraph Backend ["Backend (Google Cloud)"]
        UM -- "HTTPS POST (JSON)" --> CF_ingest[Cloud Function: ingest]
        CF_ingest -- "Increment Counters" --> FS[(Firestore)]
        
        FS -- "Solo Lectura (Rule: Deny All)" --> CF_read[Cloud Functions: metricsDaily / Summary]
    end

    subgraph Analytics ["Dashboard"]
        CF_read -- "X-API-Key" --> DB[Dashboard Web]
    end

    note[Firestore Rules niegan acceso directo.<br/>Todo acceso es vía Cloud Function.]
    style note fill:#f8bbd0,stroke:#c2185b
```

---

## 3. Sistema de Navegación y Catálogo

El sistema de navegación desacopla la definición técnica de la ruta de la representación visual en el catálogo.

### Diagrama C: Flujo de Navegación

```mermaid
graph TD
    Entry["Entry Points:<br/>Launcher, Widgets, Shortcuts"] --> MA[MainActivity]
    
    subgraph Navigation_System ["Sistema de Navegación"]
        S[Screen.kt<br/>42 Rutas Definidas]
        TR[ToolRegistry.kt<br/>33 Herramientas Catálogo]
        NG[NavGraph.kt<br/>42 Destinos Composable]
    end

    MA --> NG
    TR -- "Referencia" --> S
    NG -- "Implementa" --> S
    
    subgraph UI_Nav ["Navegación UI"]
        Cat[CategoriesScreen]
        Cat -- "Navega" --> Tool[ToolScreen]
        Tool -- "Sub-navegación" --> Sub[Details Screen]
    end

    TR --> Cat
    NG --> UI_Nav
```

---

## 4. Subsistema de Alarma Pomodoro

Debido a las restricciones de Android sobre alarmas exactas y audio en background, este subsistema utiliza un `Foreground Service` orquestado.

```mermaid
graph TD
    AM[AlarmManager] -- "Exact Alarm" --> AR[PomodoroAlarmReceiver]
    AR -- "Start FGS" --> AS[PomodoroAlarmService]
    AS -- "MediaSession / ExoPlayer" --> Audio[Audio Alarma]
    AS -- "Full-screen Intent" --> Notif[PomodoroNotification]
    Notif -- "Launch" --> Act[PomodoroAlarmActivity]
    Act -- "Action" --> Receiver[PomodoroActionReceiver]
    Receiver -- "Stop/Silence" --> AS
```

---

## 5. Estructura de Paquetes Real

```
app/src/main/java/com/joasasso/minitoolbox/
├── data/               # Repositorios y DataStores globales (Pro, Favoritos, etc.)
├── metrics/            # Pipeline de telemetría (storage, uploader)
├── nav/                # Definiciones de Screen y rutas
├── tools/              # Lógica de herramientas dividida por categoría
│   ├── entretenimiento/
│   │   ├── minijuegos/ # Incluye 'minesweeper'
│   │   └── aleatorio/
│   ├── herramientas/
│   │   ├── calculadoras/
│   │   ├── generadores/
│   │   └── instrumentos/
│   ├── info/
│   └── organizacion/
│       ├── divisorGastos/
│       ├── pomodoro/
│       └── recordatorios/
├── ui/                 # Componentes comunes, temas y capas globales (Ads)
├── utils/              # Managers de Ads y Billing
├── viewmodel/          # ViewModels de pantallas principales
└── widgets/            # Implementaciones de Glance Widgets
```

---

## 6. Modelo de Persistencia

El sistema utiliza **19 instancias de `preferencesDataStore`**. 
- 17 están centralizadas en archivos dentro del paquete `data/`.
- 2 están embebidas directamente en los archivos de Screen (`GuessCapitalScreen.kt` y `QuickMathScreen.kt`) por acoplamiento histórico.

---

## 7. Limitaciones Conocidas de la Arquitectura Actual

1.  **Módulo Único**: Toda la aplicación reside en `:app`, lo que ralentiza tiempos de compilación y dificulta la encapsulación.
2.  **Estado en Composables**: ~90% de las herramientas gestionan su estado interno (`remember/mutableStateOf`) sin un ViewModel, dificultando el testing unitario de la lógica de negocio.
3.  **Ausencia de DI**: No se utiliza inyección de dependencias (Hilt/Koin); las dependencias se pasan manualmente o se acceden vía Singleton/Context.
4.  **Desincronización Taxonómica**: La ruta `"quotes"` está vinculada a `BasicPhrasesScreen`. Las métricas reportadas bajo este ID corresponden en realidad a la herramienta de frases, no a citas.
5.  **Shadowing de Protobuf**: `CategoriesScreen.kt` importa accidentalmente `com.google.protobuf.LazyStringArrayList.emptyList`, sombreando la función estándar de Kotlin.
