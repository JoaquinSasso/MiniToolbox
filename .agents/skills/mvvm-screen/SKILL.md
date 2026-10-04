---
name: mvvm-screen
description: >-
  Procedimiento y patrones de referencia para migrar una pantalla de MiniToolbox de estado en
  el Composable a ViewModel con StateFlow, o para crear una herramienta nueva con MVVM, con
  sus tests unitarios. Usar cuando la tarea implique extraer lógica de un Composable, crear un
  ViewModel o un repositorio, o testear uno existente.
---

# Pantalla con MVVM

## Referencias en el repo (copiá estos patrones, no inventes otros)

| Necesidad | Archivo de referencia |
|---|---|
| ViewModel con repositorio, `UiState`, eventos y cambio de día | `tools/organizacion/recordatorios/agua/AguaViewModel.kt` |
| ViewModel con argumento de navegación (`SavedStateHandle`) | `tools/organizacion/divisorGastos/ReunionDetailViewModel.kt` |
| Formulario con validación | `tools/organizacion/divisorGastos/GastoFormViewModel.kt` |
| Lógica pura extraída (sin Android) | `divisorGastos/DebtEngine.kt`, `arruler/ArRulerMath.kt` |
| Tests de ViewModel con Robolectric | `app/src/test/.../agua/AguaViewModelTest.kt`, `.../divisorGastos/ReunionDetailViewModelTest.kt` |
| Tests de DataStore real | `app/src/test/.../data/AguaDataStoreTest.kt` |

## Estructura

1. **Lógica pura primero.** Los cálculos, validaciones y reglas sin `Context` van a un `object` o a funciones puras, testeables en JVM sin Robolectric. Así está hecho `DebtEngine`.
2. **Repositorio.** El DataStore se expone con funciones `suspend` y `Flow`. Las mutaciones se hacen leyendo y escribiendo dentro de **un único** `edit {}`, para que sean atómicas, nunca con `first()` seguido de `edit`. El repositorio entra al ViewModel como parámetro con valor por defecto (`private val repository: X = DefaultX`): esa es la costura que usan los tests mientras no haya DI.
3. **ViewModel.**
   - Un único `data class XUiState` expuesto como `StateFlow`, armado con `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), XUiState(isLoading = true))`.
   - Eventos de un solo disparo (snackbars, navegación) con `Channel(Channel.BUFFERED).receiveAsFlow()`.
   - Los argumentos de navegación salen del `SavedStateHandle`. Si el destino es type-safe, preferí `savedStateHandle.toRoute<Screen.X>()` antes que `get<String>("campo")`.
   - Si depende de la fecha o de la hora, aceptá un `Flow<LocalDate>` o un `Clock` inyectable, como `dateFlowOverride` en `AguaViewModel`.
   - Usá `AndroidViewModel` solo si necesitás `Application`. Así se puede crear con `viewModel()` sin escribir una factory.
4. **UI.** El Composable recibe `viewModel: X = viewModel()`, lee `val state by viewModel.uiState.collectAsStateWithLifecycle()` y recolecta los eventos en un `LaunchedEffect`. Las funciones del Composable solo llaman a métodos del ViewModel. El estado efímero de la UI (por ejemplo, si un diálogo está abierto) puede quedarse en `rememberSaveable`.

## Tests mínimos

- **Por cada acción pública del ViewModel:** el estado resultante y, si corresponde, el evento emitido.
- **Por cada función pura:** los casos borde (vacío, cero, límites, redondeo).
- **Setup obligatorio:**
  - `@RunWith(RobolectricTestRunner::class)`
  - `@Config(application = TestApplication::class)`
  - `Dispatchers.setMain(...)` en `@Before`
  - Limpiar el DataStore en `@Before`
  - `vm?.viewModelScope?.cancel()` y `Dispatchers.resetMain()` en `@After`

## Verificación

```powershell
.\gradlew.bat testDebugUnitTest --tests "*<Nombre>ViewModelTest"
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Comprobá también que la pantalla conserve su estado al rotar o tras la muerte del proceso, si antes lo hacía. Si no pudiste probarlo en un dispositivo, decilo en el reporte.

## Qué no hacer

- No dejes la pantalla a medio migrar (parte en el ViewModel, parte en `remember` con lógica de negocio).
- No migres varias herramientas en un mismo PR. Una herramienta por rama.
- No agregues Hilt como parte de una migración. La DI es un ítem aparte del backlog.
